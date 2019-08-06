package com.voxtric.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.voxtric.diabetescontrol.MainActivity;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.settings.EditEventsActivity;
import com.voxtric.diabetescontrol.settings.SettingsActivity;

public class EventsSettingsFragment extends Fragment
{
  private static final int REQUEST_EDIT_EVENTS = 200;
  public static final int RESULT_UPDATE_EVENTS = -200;

  public EventsSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    View view = inflater.inflate(R.layout.fragment_events_settings, container, false);
    view.findViewById(R.id.edit_events_button).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        Activity activity = getActivity();
        if (activity != null)
        {
          Intent intent = new Intent(activity, EditEventsActivity.class);
          startActivityForResult(intent, REQUEST_EDIT_EVENTS);
        }
      }
    });
    return view;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == EventsSettingsFragment.REQUEST_EDIT_EVENTS && resultCode == EventsSettingsFragment.RESULT_UPDATE_EVENTS)
    {
      SettingsActivity activity = (SettingsActivity)getActivity();
      if (activity != null)
      {
        activity.applyResultFlag(MainActivity.RESULT_UPDATE_EVENT_SPINNER);
      }
    }
  }
}
