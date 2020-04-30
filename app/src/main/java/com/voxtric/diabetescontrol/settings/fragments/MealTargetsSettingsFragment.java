package com.voxtric.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.database.TargetChange;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import com.voxtric.diabetescontrol.utilities.DecimalDigitsInputFilter;
import com.voxtric.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class MealTargetsSettingsFragment extends Fragment
{
  private final @IdRes int[] VIEW_IDS = new int[] {
      R.id.edit_text_target_pre_meal_lower,
      R.id.edit_text_target_pre_meal_upper,
      R.id.edit_text_target_post_meal_lower,
      R.id.edit_text_target_post_meal_upper
  };

  private final View.OnFocusChangeListener UPDATE_DATABASE = new View.OnFocusChangeListener()
  {
    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
      if (!hasFocus)
      {
        final Activity activity = getActivity();
        if (activity != null)
        {
          boolean proceed = true;
          final String[] values = new String[VIEW_IDS.length];
          for (int i = 0; i < values.length && proceed; i++)
          {
            values[i] = ((EditText) activity.findViewById(VIEW_IDS[i])).getText().toString();
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
                targetChange.preMealLower = Float.parseFloat(values[0]);
                targetChange.preMealUpper = Float.parseFloat(values[1]);
                targetChange.postMealLower = Float.parseFloat(values[2]);
                targetChange.postMealUpper = Float.parseFloat(values[3]);
                AppDatabase.getInstance().targetChangesDao().insert(targetChange);
              }
            });
          }
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
    final View view = inflater.inflate(R.layout.fragment_meal_targets_settings, container, false);

    final SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
      for (@IdRes int id : VIEW_IDS)
      {
        EditText editText = view.findViewById(id);
        editText.setFilters(new InputFilter[] { new DecimalDigitsInputFilter(2, 1) });
        activity.setTextFromDatabase(editText);
        activity.saveTextToDatabaseWhenUnfocused(editText);
        CompositeOnFocusChangeListener.applyListenerToView(editText, UPDATE_DATABASE);
        CompositeOnFocusChangeListener.applyListenerToView(editText, new HintHideOnFocusChangeListener(editText, Gravity.CENTER));
      }

      view.findViewById(R.id.set_from_bgl_highlighting_button).setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          Preference.get(activity,
              new String[]{
                  BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE,
                  BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE,
                  BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE
              },
              new String[]{
                  String.valueOf(BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE)),
                  String.valueOf(BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE)),
                  String.valueOf(BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE))
              },
              new Preference.ResultRunnable()
              {
                @Override
                public void run()
                {
                  String[] preferences = new String[]{
                      BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE,
                      BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE,
                      BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE,
                      BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE
                  };

                  for (int i = 0; i < VIEW_IDS.length; i++)
                  {
                    EditText editText = view.findViewById(VIEW_IDS[i]);
                    editText.setText(getResults().get(preferences[i]));
                    editText.requestFocus();  // Necessary for values to be stored in database.
                  }
                }
              });
        }
      });
    }

    return view;
  }
}
