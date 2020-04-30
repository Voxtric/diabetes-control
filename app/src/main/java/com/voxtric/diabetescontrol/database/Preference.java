package com.voxtric.diabetescontrol.database;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.security.InvalidParameterException;
import java.util.HashMap;

@Entity(tableName = "preferences")
public class Preference
{
  @NonNull
  @PrimaryKey()
  public String name = "Null Preference";

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

  public static void put(final Activity activity, final String name, final String value, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
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

  public static void put(final String name, final String value)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
        Preference preference = ensurePreferenceExists(preferencesDao, name);
        preference.value = value;
        preferencesDao.update(preference);
      }
    });
  }

  public static void get(final Activity activity, final String name, final String defaultValue, @NonNull final ResultRunnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
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

  public static void get(final Activity activity, final String[] names, final String[] defaultValues, @NonNull final ResultRunnable onCompletionMainThread)
  {
    if (names.length != defaultValues.length)
    {
      throw new InvalidParameterException("Length of 'names' must equal length of 'defaultValues'");
    }

    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
        HashMap<String, String> results = new HashMap<>();

        for (int i = 0; i < names.length; i++)
        {
          Preference preference = preferencesDao.getPreference(names[i]);
          if (preference == null)
          {
            results.put(names[i], defaultValues[i]);
          }
          else
          {
            results.put(names[i], preference.value);
          }
        }
        onCompletionMainThread.setResults(results);
        activity.runOnUiThread(onCompletionMainThread);
      }
    });
  }

  public static void remove(final Activity activity, final String[] names, final Runnable onCompletionMainThread)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
        for (String name : names)
        {
          Preference preference = preferencesDao.getPreference(name);
          if (preference != null)
          {
            preferencesDao.delete(preference);
          }
        }

        if (onCompletionMainThread != null)
        {
          activity.runOnUiThread(onCompletionMainThread);
        }
      }
    });
  }

  public abstract static class ResultRunnable implements Runnable
  {
    private HashMap<String, String> m_results;

    private void setResult(String value)
    {
      m_results = new HashMap<>();
      m_results.put("", value);
    }

    private void setResults(HashMap<String, String> values)
    {
      m_results = values;
    }

    protected String getResult()
    {
      return m_results.get("");
    }

    protected HashMap<String, String> getResults()
    {
      return m_results;
    }
  }
}
