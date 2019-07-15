package voxtric.com.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.settings.EditEventsActivity;

public class EventsSettingsFragment extends Fragment
{
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
          startActivity(intent);
        }
      }
    });
    return view;
  }
}