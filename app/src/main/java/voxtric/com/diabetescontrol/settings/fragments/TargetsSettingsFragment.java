package voxtric.com.diabetescontrol.settings.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import voxtric.com.diabetescontrol.R;

public class TargetsSettingsFragment extends Fragment
{
  public TargetsSettingsFragment()
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
    return inflater.inflate(R.layout.fragment_targets_settings, container, false);
  }
}
