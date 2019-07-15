package voxtric.com.diabetescontrol.database;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "preferences")
public class Preference
{
  @NonNull
  @PrimaryKey()
  public String name;

  @ColumnInfo(name = "int_value")
  public int int_value;

  @ColumnInfo(name = "float_value")
  public float float_value;

  @ColumnInfo(name = "string_value")
  public String string_value;

  private static Preference ensurePreferenceExists(PreferencesDao preferencesDao, String name)
  {
    Preference preference = preferencesDao.getPreference(name);
    if (preference == null)
    {
      preference = new Preference();
      preference.name = name;
      preferencesDao.insert(preference);
    }
    return preference;
  }

  static void put(final DatabaseActivity activity, final String name, final int value, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = ensurePreferenceExists(preferencesDao, name);
        preference.int_value = value;
        preferencesDao.update(preference);

        if (onCompletionMainThread != null)
        {
          activity.runOnUiThread(onCompletionMainThread);
        }
      }
    });
  }

  static void put(final DatabaseActivity activity, final String name, final float value, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = ensurePreferenceExists(preferencesDao, name);
        preference.float_value = value;
        preferencesDao.update(preference);

        if (onCompletionMainThread != null)
        {
          activity.runOnUiThread(onCompletionMainThread);
        }
      }
    });
  }

  static void put(final DatabaseActivity activity, final String name, final String value, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = ensurePreferenceExists(preferencesDao, name);
        preference.string_value = value;
        preferencesDao.update(preference);

        if (onCompletionMainThread != null)
        {
          activity.runOnUiThread(onCompletionMainThread);
        }
      }
    });
  }

  static void get(final DatabaseActivity activity, final String name, final int defaultValue, @NonNull final ResultRunnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = preferencesDao.getPreference(name);
        if (preference == null)
        {
          preference = new Preference();
          preference.int_value = defaultValue;
        }
        activity.runOnUiThread(onCompletionMainThread);
      }
    });
  }

  static void get(final DatabaseActivity activity, final String name, final float defaultValue, @NonNull final ResultRunnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = preferencesDao.getPreference(name);
        if (preference == null)
        {
          preference = new Preference();
          preference.float_value = defaultValue;
        }
        activity.runOnUiThread(onCompletionMainThread);
      }
    });
  }

  static void get(final DatabaseActivity activity, final String name, final String defaultValue, @NonNull final ResultRunnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = preferencesDao.getPreference(name);
        if (preference == null)
        {
          preference = new Preference();
          preference.string_value = defaultValue;
        }
        activity.runOnUiThread(onCompletionMainThread);
      }
    });
  }

  abstract class ResultRunnable implements Runnable
  {
    final Preference preference;

    ResultRunnable(Preference preference)
    {
      this.preference = preference;
    }
  }
}
