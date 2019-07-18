package voxtric.com.diabetescontrol.utilities;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.IdRes;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class AutoCompleteTextViewUtilities
{
  private static HashMap<String, Long> readAutocompleteValue(File file)
  {
    HashMap<String, Long> autocompleteValues = null;
    try
    {
      if (!file.exists())
      {
        autocompleteValues = new HashMap<>();
      }
      else
      {
        ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
        //noinspection unchecked
        autocompleteValues = (HashMap<String, Long>)inputStream.readObject();
      }
    }
    catch (EOFException exception)
    {
      if (file.delete())
      {
        autocompleteValues = new HashMap<>();
      }
    }
    catch (Exception exception)
    {
      Log.e(AutoCompleteTextViewUtilities.class.getCanonicalName(), exception.toString());
      exception.printStackTrace();
    }
    return autocompleteValues;
  }

  private static void writeAutoCompleteValues(File file, HashMap<String, Long> autocompleteValues)
  {
    try
    {
      ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
      outputStream.writeObject(autocompleteValues);
      outputStream.close();
    }
    catch (IOException exception)
    {
      Log.e(AutoCompleteTextViewUtilities.class.getCanonicalName(), exception.toString());
      exception.printStackTrace();
    }
  }

  public static void clearAgedValuesAutoCompleteValues(final Activity activity, @IdRes final int input, final long olderThan)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final String idName = activity.getResources().getResourceName(input).replaceAll(".*/", "");
        File file = new File(activity.getFilesDir(), idName);
        HashMap<String, Long> autoCompleteValues = readAutocompleteValue(file);
        ArrayList<String> toRemove = new ArrayList<>();
        for (String value : autoCompleteValues.keySet())
        {
          Long timestamp = autoCompleteValues.get(value);
          if (timestamp != null && timestamp < olderThan)
          {
            toRemove.add(value);
          }
        }
        for (String value : toRemove)
        {
          autoCompleteValues.remove(value);
        }
        refreshAutoCompleteView(activity, input, autoCompleteValues);
      }
    });
  }

  public static void saveAutoCompleteView(final Activity activity, @IdRes final int input)
  {
    AutoCompleteTextView textView = activity.findViewById(input);
    final String text = textView.getText().toString();
    if (text.length() > 0)
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          String idName = activity.getResources().getResourceName(input).replaceAll(".*/", "");
          File file = new File(activity.getFilesDir(), idName);
          HashMap<String, Long> autocompleteValues = readAutocompleteValue(file);
          autocompleteValues.put(text, System.currentTimeMillis());
          writeAutoCompleteValues(file, autocompleteValues);

          refreshAutoCompleteView(activity, input, autocompleteValues);
        }
      });
    }
  }

  private static void refreshAutoCompleteView(final Activity activity, @IdRes final int input, HashMap<String, Long> autoCompleteValues)
  {
    String idName = activity.getResources().getResourceName(input).replaceAll(".*/", "");
    File file = new File(activity.getFilesDir(), idName);
    if (autoCompleteValues == null)
    {
      autoCompleteValues = readAutocompleteValue(file);
    }
    String[] stringArray = new String[0];
    stringArray = autoCompleteValues.keySet().toArray(stringArray);

    final String[] finalStringArray = stringArray;
    activity.runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        AutoCompleteTextView autoText = activity.findViewById(input);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, finalStringArray);
        autoText.setAdapter(adapter);
      }
    });
  }
}
