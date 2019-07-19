package voxtric.com.diabetescontrol.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Calendar;

@Database(version = AppDatabase.VERSION, entities = {
    DataEntry.class,
    Event.class,
    TargetChange.class,
    Preference.class,
    Food.class
})
public abstract class AppDatabase extends RoomDatabase
{
  public abstract DataEntriesDao dataEntriesDao();

  public abstract EventsDao eventsDao();

  public abstract TargetChangesDao targetChangesDao();

  public abstract PreferencesDao preferencesDao();

  public abstract FoodsDao foodsDao();



  public static final String NAME = "diabetes-control.db";
  public static final int VERSION = 3;

  private static AppDatabase s_instance = null;

  public static AppDatabase getInstance()
  {
    return s_instance;
  }

  public static AppDatabase initialise(Context context)
  {
    if (s_instance != null)
    {
      s_instance.close();
    }
    s_instance = Room.databaseBuilder(context, AppDatabase.class, NAME)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build();
    return s_instance;
  }

  private static final Migration MIGRATION_2_3 = new Migration(2, 3)
  {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database)
    {
      Log.i("Migration", "v2 to v3");

      // Add preference table with the database version as the first entry.
      {
        database.execSQL("CREATE TABLE preferences(name TEXT PRIMARY KEY NOT NULL, value TEXT)");
        String versionString = String.valueOf(AppDatabase.VERSION);
        database.execSQL("INSERT INTO preferences (name, value) VALUES ('database_version', :versionString)", new Object[]{ versionString });
      }

      // Migrate to the new layout for data entries.
      {
        database.execSQL("ALTER TABLE data_entries RENAME TO data_entries_OLD");
        database.execSQL("CREATE TABLE data_entries (" +
            "actual_timestamp INTEGER PRIMARY KEY NOT NULL, " +
            "day_timestamp INTEGER NOT NULL, " +
            "event TEXT, " +
            "blood_glucose_level REAL NOT NULL, " +
            "insulin_name TEXT, " +
            "insulin_dose INTEGER NOT NULL, " +
            "additional_notes TEXT)");
        Cursor cursor = database.query("SELECT * FROM data_entries_OLD");
        while (cursor.moveToNext())
        {
          long actualTimestamp = cursor.getLong(cursor.getColumnIndex("actual_timestamp"));
          long dayTimestamp = cursor.getLong(cursor.getColumnIndex("day_timestamp"));
          String event = cursor.getString(cursor.getColumnIndex("event"));
          float bloodGlucoseLevel = cursor.getFloat(cursor.getColumnIndex("blood_glucose_level"));
          String insulinName = cursor.getString(cursor.getColumnIndex("insulin_name"));
          int insulinDose = 0;
          try
          {
            insulinDose = Integer.parseInt(cursor.getString(cursor.getColumnIndex("insulin_dose")));
          }
          catch (NumberFormatException ignored) {}
          String additionalNotes = cursor.getString(cursor.getColumnIndex("additional_notes"));
          database.execSQL("INSERT INTO data_entries (actual_timestamp, day_timestamp, event, blood_glucose_level, insulin_name, insulin_dose, additional_notes)" +
                  "VALUES (:actualTimestamp, :dayTimestamp, :event, :bloodGlucoseLevel, :insulinName, :insulinDose, :additionalNotes)",
              new Object[]{ actualTimestamp, dayTimestamp, event, bloodGlucoseLevel, insulinName, insulinDose, additionalNotes });
        }
      }

      // Add the new food table.
      {
        database.execSQL("CREATE TABLE foods (" +
            "data_entry_timestamp INTEGER NOT NULL, " +
            "name TEXT NOT NULL," +
            "PRIMARY KEY (data_entry_timestamp, name)," +
            "FOREIGN KEY (data_entry_timestamp) REFERENCES data_entries (actual_timestamp) ON DELETE CASCADE)");
      }

      // Move the food from the old data entries table and delete the old table.
      {
        Cursor cursor = database.query("SELECT actual_timestamp, food_eaten FROM data_entries_OLD");
        while (cursor.moveToNext())
        {
          long timestamp = cursor.getLong(0);
          String[] foodEaten = cursor.getString(1).split(",");
          for (int i = 0; i < foodEaten.length; i++)
          {
            String food = foodEaten[i].trim();
            if (food.length() > 0)
            {
              database.execSQL("INSERT INTO foods VALUES (:timestamp, :food)", new Object[]{ timestamp, food });
            }
          }
        }
        database.execSQL("DROP TABLE data_entries_OLD");
      }
    }
  };

  private static final Migration MIGRATION_1_2 = new Migration(1, 2)
  {
    @SuppressLint("DefaultLocale")
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database)
    {
      Log.i("Migration", "v1 to v2");

      // Add 'order' column to 'events' table and order by timestamp.
      {
        database.execSQL("ALTER TABLE events ADD COLUMN 'order' INTEGER NOT NULL DEFAULT -1");
        Cursor cursor = database.query("SELECT id FROM events ORDER BY time_in_day ASC");
        int order = 0;
        while (cursor.moveToNext())
        {
          int id = cursor.getInt(0);
          database.execSQL("UPDATE events SET 'order' = :order WHERE id IS :id", new Object[]{order, id});
          order++;
        }
      }

      // Add 'actual_timestamp' column to 'data_entries' table and copy 'time_stamp' column data to 'actual_timestamp' column.
      {
        database.execSQL("ALTER TABLE data_entries ADD COLUMN actual_timestamp INTEGER");
        database.execSQL("UPDATE data_entries SET actual_timestamp = time_stamp");
      }

      // Add 'day_timestamp' column to 'data_entries' table and copy day beginning rounded 'time_stamp' data to 'day_timestamp column'.
      {
        database.execSQL("ALTER TABLE data_entries ADD COLUMN day_timestamp INTEGER");
        Cursor cursor = database.query("SELECT time_stamp FROM data_entries");
        while (cursor.moveToNext())
        {
          long timestamp = cursor.getLong(0);
          Calendar calendar = Calendar.getInstance();
          calendar.setTimeInMillis(timestamp);
          calendar.set(Calendar.HOUR_OF_DAY, 0);
          calendar.set(Calendar.MINUTE, 0);
          calendar.set(Calendar.SECOND, 0);
          calendar.set(Calendar.MILLISECOND, 0);
          long dayTimestamp = calendar.getTimeInMillis();

          database.execSQL("UPDATE data_entries SET day_timestamp = :dayTimestamp WHERE time_stamp = :timestamp", new Object[]{dayTimestamp, timestamp});
        }
      }

      // Delete the 'time_stamp' column of the 'data_entries' table by creating a new table without the 'time_stamp' column and copying the desired data across.
      {
        database.execSQL("ALTER TABLE data_entries RENAME TO data_entries_OLD");
        database.execSQL("CREATE TABLE data_entries (" +
            "actual_timestamp INTEGER PRIMARY KEY NOT NULL, " +
            "day_timestamp INTEGER NOT NULL, " +
            "event TEXT, " +
            "insulin_name TEXT, " +
            "insulin_dose TEXT, " +
            "blood_glucose_level REAL NOT NULL, " +
            "food_eaten TEXT, " +
            "additional_notes TEXT)");
        database.execSQL("INSERT INTO data_entries (actual_timestamp, day_timestamp, event, insulin_name, insulin_dose, blood_glucose_level, food_eaten, additional_notes) " +
            "SELECT actual_timestamp, day_timestamp, event, insulin_name, insulin_dose, blood_glucose_level, food_eaten, additional_notes FROM data_entries_OLD");
      }
      database.execSQL("DROP TABLE data_entries_OLD");

      // Rename the 'time_stamp' column of the 'data_entries' table to 'timestamp' by creating a new table with the correct name and copying the desired data across.
      {
        database.execSQL("ALTER TABLE target_changes RENAME TO target_changes_OLD");
        database.execSQL("CREATE TABLE target_changes (" +
            "timestamp INTEGER PRIMARY KEY NOT NULL, " +
            "pre_meal_lower REAL NOT NULL, " +
            "pre_meal_upper REAL NOT NULL, " +
            "post_meal_lower REAL NOT NULL, " +
            "post_meal_upper REAL NOT NULL)");
        database.execSQL("INSERT INTO target_changes (timestamp, pre_meal_lower, pre_meal_upper, post_meal_lower, post_meal_upper) " +
            "SELECT time_stamp, pre_meal_lower, pre_meal_upper, post_meal_lower, post_meal_upper FROM target_changes_OLD");
        database.execSQL("DROP TABLE target_changes_OLD");
      }
    }
  };
}
