package voxtric.com.diabetescontrol.settings.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
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
  private static final Map<String, Float> DEFAULT_VALUES;
  static
  {
    Map<String, Float> defaults = new HashMap<>();
    defaults.put("ideal_minimum", 2.6f);
    defaults.put("high_minimum", 8.2f);
    defaults.put("action_required_minimum", 11.9f);
    DEFAULT_VALUES = Collections.unmodifiableMap(defaults);
}

  public BGLHighlightingSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    View view = inflater.inflate(R.layout.fragment_bgl_highlighting_settings, container, false);

    SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
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
          new String[]{ "ideal_minimum", "high_minimum", "action_required_minimum" },
          new String[]{ String.valueOf(DEFAULT_VALUES.get("ideal_minimum")), String.valueOf(DEFAULT_VALUES.get("high_minimum")), String.valueOf(DEFAULT_VALUES.get("action_required_minimum")) },
          new Preference.ResultRunnable()
          {
            @Override
            public void run()
            {
              HashMap<String, String> results = getResults();
              idealRangeLower.setText(results.get("ideal_minimum"));
              idealRangeUpper.setText(results.get("high_minimum"));
              highRangeLower.setText(results.get("high_minimum"));
              highRangeUpper.setText(results.get("action_required_minimum"));
            }
          });
    }

    return view;
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

            // TODO: Validate for higher than 0
            // TODO: Validate for higher or lower than previous or next value


            Preference.put(activity, preferenceName, text, null);
            view.setText(String.valueOf(value));
          }
          catch (NumberFormatException ignored)
          {
            Preference.get(activity, preferenceName, String.valueOf(DEFAULT_VALUES.get(preferenceName)), new Preference.ResultRunnable()
            {
              @Override
              public void run()
              {
                view.append(getResult());
              }
            });
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
