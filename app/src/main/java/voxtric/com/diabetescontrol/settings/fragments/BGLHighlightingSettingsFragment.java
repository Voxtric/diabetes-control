package voxtric.com.diabetescontrol.settings.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.HashMap;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.settings.SettingsActivity;

public class BGLHighlightingSettingsFragment extends Fragment
{
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
          new String[]{"ideal_minimum", "high_minimum", "action_required_minimum"},
          new String[]{"2.6", "8.2", "11.9"},
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

  private void saveValuesToDatabaseWhenUnfocused(final DatabaseActivity activity, final EditText view, final String prefrenceName)
  {
    view.setOnFocusChangeListener(new View.OnFocusChangeListener()
    {
      @Override
      public void onFocusChange(View v, boolean hasFocus)
      {
        if (!hasFocus)
        {
          Preference.put(activity, prefrenceName, view.getText().toString(), null);
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
