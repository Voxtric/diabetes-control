package voxtric.com.diabetescontrol.settings.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.TargetChange;
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.ViewUtilities;

public class MealTargetsSettingsFragment extends Fragment
{
  private final @IdRes int[] VIEW_IDS = new int[] {
      R.id.edit_text_target_pre_meal_lower,
      R.id.edit_text_target_pre_meal_upper,
      R.id.edit_text_target_post_meal_lower,
      R.id.edit_text_target_post_meal_upper
  };

  private final Runnable UPDATE_DATABASE = new Runnable()
  {
    @Override
    public void run()
    {
      final DatabaseActivity activity = (DatabaseActivity)getActivity();
      if (activity != null)
      {
        boolean proceed = true;
        final String[] values = new String[VIEW_IDS.length];
        for (int i = 0; i < values.length && proceed; i++)
        {
          values[i] = ((EditText)activity.findViewById(VIEW_IDS[i])).getText().toString();
          proceed = values[i] != null && values[i].length() > 0;
        }

        if (proceed)
        {
          AsyncTask.execute(new Runnable()
          {
            @Override
            public void run()
            {
              TargetChange targetChange = new TargetChange();
              targetChange.timestamp = System.currentTimeMillis();
              targetChange.preMealLower = Float.valueOf(values[0]);
              targetChange.preMealUpper = Float.valueOf(values[1]);
              targetChange.postMealLower = Float.valueOf(values[2]);
              targetChange.postMealUpper = Float.valueOf(values[3]);
              activity.getDatabase().targetChangesDao().insert(targetChange);
            }
          });
        }
      }
    }
  };

  public MealTargetsSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    View view = inflater.inflate(R.layout.fragment_meal_targets_settings, container, false);

    SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
      for (@IdRes int id : VIEW_IDS)
      {
        EditText editText = view.findViewById(id);
        activity.setTextFromDatabase(editText);
        activity.saveTextToDatabaseWhenUnfocused(editText, UPDATE_DATABASE);
        ViewUtilities.addHintHide(editText, Gravity.CENTER, activity);
      }
    }

    return view;
  }
}
