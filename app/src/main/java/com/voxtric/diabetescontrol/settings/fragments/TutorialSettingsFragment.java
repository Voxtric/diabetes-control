package com.voxtric.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.voxtric.diabetescontrol.MainActivity;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.settings.EditEventsActivity;
import com.voxtric.diabetescontrol.settings.SettingsActivity;

public class TutorialSettingsFragment extends Fragment
{
  public TutorialSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    View view = inflater.inflate(R.layout.fragment_tutorial_settings, container, false);
    view.findViewById(R.id.reset_tutorial_button).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        Activity activity = getActivity();
        if (activity != null)
        {
          final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
          SharedPreferences.Editor preferenceEditor = preferences.edit();
          preferenceEditor.putInt("main_activity_showcase_progress", 0);
          preferenceEditor.putInt("add_new_entry_fragment_showcase_progress", 0);
          preferenceEditor.putInt("entry_list_fragment_showcase_progress", 0);
          preferenceEditor.putInt("entry_graph_fragment_showcase_progress", 0);
          // TODO: Add the rest.
          preferenceEditor.apply();

          Toast.makeText(activity, R.string.tutorial_reset_toast, Toast.LENGTH_LONG).show();
        }
      }
    });
    return view;
  }
}
