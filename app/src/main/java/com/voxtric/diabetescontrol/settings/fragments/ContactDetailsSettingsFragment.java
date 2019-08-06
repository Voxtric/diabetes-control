package com.voxtric.diabetescontrol.settings.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import com.voxtric.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class ContactDetailsSettingsFragment extends Fragment
{
  private final @IdRes int[] VIEW_IDS = new int[] {
      R.id.edit_text_contact_name,
      R.id.edit_text_contact_number
  };

  public ContactDetailsSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    View view = inflater.inflate(R.layout.fragment_contact_details_settings, container, false);

    SettingsActivity activity = (SettingsActivity)getActivity();
    if (activity != null)
    {
      for (@IdRes int id : VIEW_IDS)
      {
        EditText editText = view.findViewById(id);
        activity.setTextFromDatabase(editText);
        activity.saveTextToDatabaseWhenUnfocused(editText);
        CompositeOnFocusChangeListener.applyListenerToView(editText, new HintHideOnFocusChangeListener(editText, Gravity.CENTER));
      }
    }

    return view;
  }
}
