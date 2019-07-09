package voxtric.com.diabetescontrol.database;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Calendar;

public abstract class DatabaseActivity extends AppCompatActivity
{
  public final String DATABASE_NAME = "diabetes-control.db";

  protected AppDatabase m_database = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    m_database = Room.databaseBuilder(this, AppDatabase.class, DATABASE_NAME)
        .addMigrations(MIGRATION_1_2)
        .build();
  }

  public AppDatabase getDatabase()
  {
    return m_database;
  }

  static final Migration MIGRATION_1_2 = new Migration(1, 2)
  {
    @SuppressLint("DefaultLocale")
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database)
    {
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

          timestamp = calendar.getTimeInMillis();
          database.execSQL("UPDATE data_entries SET day_timestamp = :timestamp", new Object[]{timestamp});
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
