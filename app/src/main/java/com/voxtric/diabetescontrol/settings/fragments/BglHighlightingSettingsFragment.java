package com.voxtric.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.voxtric.diabetescontrol.MainActivity;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.RecoveryForegroundService;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import com.voxtric.diabetescontrol.utilities.DecimalDigitsInputFilter;
import com.voxtric.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class BglHighlightingSettingsFragment extends Fragment
{
  public static final String HIGHLIGHTING_ENABLED_PREFERENCE = "bgl_highlighting_enabled";
  public static final String IDEAL_MINIMUM_PREFERENCE = "bgl_ideal_minimum";
  public static final String HIGH_MINIMUM_PREFERENCE = "bgl_high_minimum";
  public static final String ACTION_REQUIRED_MINIMUM_PREFERENCE = "bgl_action_required_minimum";
  
  public static final Map<String, Float> DEFAULT_VALUES;
  static
  {
    Map<String, Float> defaults = new HashMap<>();
    defaults.put(IDEAL_MINIMUM_PREFERENCE, 4.0f);
    defaults.put(HIGH_MINIMUM_PREFERENCE, 8.0f);
    defaults.put(ACTION_REQUIRED_MINIMUM_PREFERENCE, 12.0f);
    DEFAULT_VALUES = Collections.unmodifiableMap(defaults);
  }

  private HashMap<String, String> m_bglRangeValues = null;

  public BglHighlightingSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      m_bglRangeValues = new HashMap<>();
      m_bglRangeValues.put(IDEAL_MINIMUM_PREFERENCE, savedInstanceState.getString(IDEAL_MINIMUM_PREFERENCE));
      m_bglRangeValues.put(HIGH_MINIMUM_PREFERENCE, savedInstanceState.getString(HIGH_MINIMUM_PREFERENCE));
      m_bglRangeValues.put(ACTION_REQUIRED_MINIMUM_PREFERENCE, savedInstanceState.getString(ACTION_REQUIRED_MINIMUM_PREFERENCE));
    }

    final View view = inflater.inflate(R.layout.fragment_bgl_highlighting_settings, container, false);

    final SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
      final Switch highlightingEnabledSwitch = view.findViewById(R.id.highlighting_enabled_switch);
      highlightingEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          setBGLHighlightingEnabled(activity, checked);
          Preference.put(activity, HIGHLIGHTING_ENABLED_PREFERENCE, String.valueOf(checked), null);
          activity.applyResultFlag(MainActivity.RESULT_UPDATE_BGL_HIGHLIGHTING);
        }
      });

      final Button resetBGLValuesButton = view.findViewById(R.id.button_reset_bgl_values);
      resetBGLValuesButton.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(final View view)
        {
          Preference.remove(activity,
              new String[]{
                  IDEAL_MINIMUM_PREFERENCE,
                  HIGH_MINIMUM_PREFERENCE,
                  ACTION_REQUIRED_MINIMUM_PREFERENCE
              },
              new Runnable()
              {
                @Override
                public void run()
                {
                  setValues(activity, view.getRootView());
                  Toast.makeText(activity, R.string.bgl_range_values_reset, Toast.LENGTH_LONG).show();
                  activity.applyResultFlag(MainActivity.RESULT_UPDATE_BGL_HIGHLIGHTING);
                }
              });
        }
      });

      final EditText idealRangeLower = view.findViewById(R.id.ideal_range_lower);
      final EditText idealRangeUpper = view.findViewById(R.id.ideal_range_upper);
      final EditText highRangeLower = view.findViewById(R.id.high_range_lower);
      final EditText highRangeUpper = view.findViewById(R.id.high_range_upper);

      DecimalDigitsInputFilter inputFilter = new DecimalDigitsInputFilter(2, 1);
      idealRangeLower.setFilters(new InputFilter[] { inputFilter });
      idealRangeUpper.setFilters(new InputFilter[] { inputFilter });
      highRangeLower.setFilters(new InputFilter[] { inputFilter });
      highRangeUpper.setFilters(new InputFilter[] { inputFilter });

      CompositeOnFocusChangeListener.applyListenerToView(idealRangeLower, new HintHideOnFocusChangeListener(idealRangeLower, Gravity.CENTER));
      CompositeOnFocusChangeListener.applyListenerToView(idealRangeUpper, new HintHideOnFocusChangeListener(idealRangeLower, Gravity.CENTER));
      CompositeOnFocusChangeListener.applyListenerToView(highRangeLower, new HintHideOnFocusChangeListener(idealRangeLower, Gravity.CENTER));
      CompositeOnFocusChangeListener.applyListenerToView(highRangeUpper, new HintHideOnFocusChangeListener(idealRangeLower, Gravity.CENTER));

      LinkedEditTextsTextWatcher idealRangeUpperTextWatcher = new LinkedEditTextsTextWatcher(highRangeLower);
      LinkedEditTextsTextWatcher highRangeLowerTextWatcher = new LinkedEditTextsTextWatcher(idealRangeUpper);
      idealRangeUpperTextWatcher.setLinkedEditTextTextWatcher(highRangeLowerTextWatcher);
      highRangeLowerTextWatcher.setLinkedEditTextTextWatcher(idealRangeUpperTextWatcher);
      idealRangeUpper.addTextChangedListener(idealRangeUpperTextWatcher);
      highRangeLower.addTextChangedListener(highRangeLowerTextWatcher);

      saveValuesToDatabaseWhenUnfocused(activity, idealRangeLower, IDEAL_MINIMUM_PREFERENCE);
      saveValuesToDatabaseWhenUnfocused(activity, idealRangeUpper, HIGH_MINIMUM_PREFERENCE);
      saveValuesToDatabaseWhenUnfocused(activity, highRangeLower, HIGH_MINIMUM_PREFERENCE);
      saveValuesToDatabaseWhenUnfocused(activity, highRangeUpper, ACTION_REQUIRED_MINIMUM_PREFERENCE);

      setValues(activity, view);
    }

    return view;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putString(IDEAL_MINIMUM_PREFERENCE, m_bglRangeValues.get(IDEAL_MINIMUM_PREFERENCE));
    outState.putString(HIGH_MINIMUM_PREFERENCE, m_bglRangeValues.get(HIGH_MINIMUM_PREFERENCE));
    outState.putString(ACTION_REQUIRED_MINIMUM_PREFERENCE, m_bglRangeValues.get(ACTION_REQUIRED_MINIMUM_PREFERENCE));
  }

  private void setValues(final Activity activity, View view)
  {
    if (!RecoveryForegroundService.isDownloading())
    {
      final Switch highlightingEnabledSwitch = view.findViewById(R.id.highlighting_enabled_switch);
      final EditText idealRangeLower = view.findViewById(R.id.ideal_range_lower);
      final EditText idealRangeUpper = view.findViewById(R.id.ideal_range_upper);
      final EditText highRangeLower = view.findViewById(R.id.high_range_lower);
      final EditText highRangeUpper = view.findViewById(R.id.high_range_upper);

      Preference.get(activity,
                     new String[]{
                         HIGHLIGHTING_ENABLED_PREFERENCE,
                         IDEAL_MINIMUM_PREFERENCE,
                         HIGH_MINIMUM_PREFERENCE,
                         ACTION_REQUIRED_MINIMUM_PREFERENCE },
                     new String[]{
                        String.valueOf(true),
                        String.valueOf(DEFAULT_VALUES.get(IDEAL_MINIMUM_PREFERENCE)),
                        String.valueOf(DEFAULT_VALUES.get(HIGH_MINIMUM_PREFERENCE)),
                        String.valueOf(DEFAULT_VALUES.get(ACTION_REQUIRED_MINIMUM_PREFERENCE)) },
                     new Preference.ResultRunnable()
                     {
                       @Override
                       public void run()
                       {
                         m_bglRangeValues = getResults();
                         idealRangeLower.setText(m_bglRangeValues.get(IDEAL_MINIMUM_PREFERENCE));
                         idealRangeUpper.setText(m_bglRangeValues.get(HIGH_MINIMUM_PREFERENCE));
                         highRangeLower.setText(m_bglRangeValues.get(HIGH_MINIMUM_PREFERENCE));
                         highRangeUpper.setText(m_bglRangeValues.get(ACTION_REQUIRED_MINIMUM_PREFERENCE));

                         boolean highlightingEnabled = Boolean.parseBoolean(m_bglRangeValues.get(
                             HIGHLIGHTING_ENABLED_PREFERENCE));
                         highlightingEnabledSwitch.setChecked(highlightingEnabled);
                         highlightingEnabledSwitch.jumpDrawablesToCurrentState();
                         setBGLHighlightingEnabled(activity, highlightingEnabled);
                       }
                     });
    }
  }

  private void setBGLHighlightingEnabled(Activity activity, boolean enabled)
  {
    activity.findViewById(R.id.ideal_range_label).setEnabled(enabled);
    activity.findViewById(R.id.ideal_range_lower).setEnabled(enabled);
    activity.findViewById(R.id.ideal_range_upper).setEnabled(enabled);

    activity.findViewById(R.id.high_range_label).setEnabled(enabled);
    activity.findViewById(R.id.high_range_lower).setEnabled(enabled);
    activity.findViewById(R.id.high_range_upper).setEnabled(enabled);

    activity.findViewById(R.id.button_reset_bgl_values).setEnabled(enabled);
  }

  private void saveValuesToDatabaseWhenUnfocused(final SettingsActivity activity, final EditText view, final String preferenceName)
  {
    CompositeOnFocusChangeListener.applyListenerToView(view, new View.OnFocusChangeListener()
    {
      @Override
      public void onFocusChange(View v, boolean hasFocus)
      {
        if (!hasFocus)
        {
          String text = view.getText().toString();
          if (text.endsWith("."))
          {
            text += "0";
          }

          try
          {
            float value = Float.parseFloat(text);
            boolean valueValid;
            switch (preferenceName)
            {
              case IDEAL_MINIMUM_PREFERENCE:
                valueValid = value < Float.parseFloat(Objects.requireNonNull(m_bglRangeValues.get(HIGH_MINIMUM_PREFERENCE)));
                break;
              case HIGH_MINIMUM_PREFERENCE:
                valueValid = value > Float.parseFloat(Objects.requireNonNull(m_bglRangeValues.get(IDEAL_MINIMUM_PREFERENCE))) &&
                    value < Float.parseFloat(Objects.requireNonNull(m_bglRangeValues.get(ACTION_REQUIRED_MINIMUM_PREFERENCE)));
                break;
              case ACTION_REQUIRED_MINIMUM_PREFERENCE:
                valueValid = value > Float.parseFloat(Objects.requireNonNull(m_bglRangeValues.get(HIGH_MINIMUM_PREFERENCE)));
                break;
              default:
                valueValid = false;
                break;
            }

            if (valueValid)
            {
              Preference.put(activity, preferenceName, text, null);
              view.setText(String.valueOf(value));
              m_bglRangeValues.put(preferenceName, text);
              activity.applyResultFlag(MainActivity.RESULT_UPDATE_BGL_HIGHLIGHTING);
            }
            else
            {
              view.setText(m_bglRangeValues.get(preferenceName));
              Toast.makeText(activity, R.string.bgl_range_value_out_of_range_message, Toast.LENGTH_LONG).show();
            }
          }
          catch (NumberFormatException ignored)
          {
            view.append(m_bglRangeValues.get(preferenceName));
            Toast.makeText(activity, R.string.bgl_range_value_empty_message, Toast.LENGTH_LONG).show();
          }
        }
      }
    });
  }

  static class LinkedEditTextsTextWatcher implements TextWatcher
  {
    private final EditText m_linkedEditText;
    private LinkedEditTextsTextWatcher m_linkedEditTextTextWatcher = null;

    private boolean m_processUpdate = true;

    LinkedEditTextsTextWatcher(EditText linkedEditText)
    {
      m_linkedEditText = linkedEditText;
    }

    void setLinkedEditTextTextWatcher(LinkedEditTextsTextWatcher textWatcher)
    {
      m_linkedEditTextTextWatcher = textWatcher;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void afterTextChanged(Editable editable)
    {
      if (m_processUpdate)
      {
        m_linkedEditTextTextWatcher.m_processUpdate = false;
        m_linkedEditText.setText(editable);
        m_linkedEditTextTextWatcher.m_processUpdate = true;
      }
    }
  }
}
