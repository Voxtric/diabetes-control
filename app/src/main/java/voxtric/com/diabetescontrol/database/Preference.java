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

  @ColumnInfo(name = "value")
  public String value;

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

  public static void put(final DatabaseActivity activity, final String name, final String value, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = activity.getDatabase().preferencesDao();
        Preference preference = ensurePreferenceExists(preferencesDao, name);
        preference.value = value;
        preferencesDao.update(preference);

        if (onCompletionMainThread != null)
        {
          activity.runOnUiThread(onCompletionMainThread);
        }
      }
    });
  }

  public static void get(final DatabaseActivity activity, final String name, final String defaultValue, @NonNull final ResultRunnable onCompletionMainThread)
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
          onCompletionMainThread.setResult(defaultValue);
        }
        else
        {
          onCompletionMainThread.setResult(preference.value);
        }
        activity.runOnUiThread(onCompletionMainThread);
      }
    });
  }

  public abstract static class ResultRunnable implements Runnable
  {
    private String result;

    private void setResult(String result)
    {
      this.result = result;
    }

    public String getResult()
    {
      return result;
    }
  }
}
