package voxtric.com.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import voxtric.com.diabetescontrol.utilities.ViewUtilities;

public class BGLHighlightingSettingsFragment extends Fragment
{
  public static final Map<String, Float> DEFAULT_VALUES;
  static
  {
    Map<String, Float> defaults = new HashMap<>();
    defaults.put("ideal_minimum", 2.6f);
    defaults.put("high_minimum", 8.2f);
    defaults.put("action_required_minimum", 11.9f);
    DEFAULT_VALUES = Collections.unmodifiableMap(defaults);
  }

  HashMap<String, String> m_bglRangeValues = null;

  public BGLHighlightingSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    final View view = inflater.inflate(R.layout.fragment_bgl_highlighting_settings, container, false);

    final SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
      final Switch highlightingEnabledSwitch = view.findViewById(R.id.highlighting_enabled);
      highlightingEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          setBGLHighlightingEnabled(activity, checked);
          Preference.put(activity, "bgl_highlighting_enabled", String.valueOf(checked), null);
        }
      });

      final EditText idealRangeLower = view.findViewById(R.id.ideal_range_lower);
      final EditText idealRangeUpper = view.findViewById(R.id.ideal_range_upper);
      final EditText highRangeLower = view.findViewById(R.id.high_range_lower);
      final EditText highRangeUpper = view.findViewById(R.id.high_range_upper);

      ViewUtilities.addHintHide(idealRangeLower, Gravity.CENTER, activity);
      ViewUtilities.addHintHide(idealRangeUpper, Gravity.CENTER, activity);
      ViewUtilities.addHintHide(highRangeLower, Gravity.CENTER, activity);
      ViewUtilities.addHintHide(highRangeUpper, Gravity.CENTER, activity);

      LinkedEditTextsTextWatcher idealRangeUpperTextWatcher = new LinkedEditTextsTextWatcher(highRangeLower);
      LinkedEditTextsTextWatcher highRangeLowerTextWatcher = new LinkedEditTextsTextWatcher(idealRangeUpper);
      idealRangeUpperTextWatcher.setLinkedEditTextTextWatcher(highRangeLowerTextWatcher);
      highRangeLowerTextWatcher.setLinkedEditTextTextWatcher(idealRangeUpperTextWatcher);
      idealRangeUpper.addTextChangedListener(idealRangeUpperTextWatcher);
      highRangeLower.addTextChangedListener(highRangeLowerTextWatcher);

      saveValuesToDatabaseWhenUnfocused(activity, idealRangeLower, "ideal_minimum");
      saveValuesToDatabaseWhenUnfocused(activity, idealRangeUpper, "high_minimum");
      saveValuesToDatabaseWhenUnfocused(activity, highRangeLower, "high_minimum");
      saveValuesToDatabaseWhenUnfocused(activity, highRangeUpper, "action_required_minimum");

      Preference.get(activity,
          new String[]{ "bgl_highlighting_enabled", "ideal_minimum", "high_minimum", "action_required_minimum" },
          new String[]{ "true", String.valueOf(DEFAULT_VALUES.get("ideal_minimum")), String.valueOf(DEFAULT_VALUES.get("high_minimum")), String.valueOf(DEFAULT_VALUES.get("action_required_minimum")) },
          new Preference.ResultRunnable()
          {
            @Override
            public void run()
            {
              m_bglRangeValues = getResults();
              idealRangeLower.setText(m_bglRangeValues.get("ideal_minimum"));
              idealRangeUpper.setText(m_bglRangeValues.get("high_minimum"));
              highRangeLower.setText(m_bglRangeValues.get("high_minimum"));
              highRangeUpper.setText(m_bglRangeValues.get("action_required_minimum"));

              boolean highlightingEnabled = Boolean.valueOf(m_bglRangeValues.get("bgl_highlighting_enabled"));
              highlightingEnabledSwitch.setChecked(highlightingEnabled);
              setBGLHighlightingEnabled(activity, highlightingEnabled);
            }
          });
    }

    return view;
  }

  private void setBGLHighlightingEnabled(Activity activity, boolean enabled)
  {
    activity.findViewById(R.id.ideal_range_label).setEnabled(enabled);
    activity.findViewById(R.id.ideal_range_lower).setEnabled(enabled);
    activity.findViewById(R.id.ideal_range_upper).setEnabled(enabled);

    activity.findViewById(R.id.high_range_label).setEnabled(enabled);
    activity.findViewById(R.id.high_range_lower).setEnabled(enabled);
    activity.findViewById(R.id.high_range_upper).setEnabled(enabled);
  }

  private void saveValuesToDatabaseWhenUnfocused(final DatabaseActivity activity, final EditText view, final String preferenceName)
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
            float value = Float.valueOf(text);
            boolean valueValid;
            switch (preferenceName)
            {
              case "ideal_minimum":
                valueValid = value < Float.valueOf(m_bglRangeValues.get("high_minimum"));
                break;
              case "high_minimum":
                valueValid = value > Float.valueOf(m_bglRangeValues.get("ideal_minimum")) &&
                    value < Float.valueOf(m_bglRangeValues.get("action_required_minimum"));
                break;
              case "action_required_minimum":
                valueValid = value > Float.valueOf(m_bglRangeValues.get("high_minimum"));
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

  class LinkedEditTextsTextWatcher implements TextWatcher
  {
    private EditText m_linkedEditText;
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
