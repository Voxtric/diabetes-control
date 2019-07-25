package voxtric.com.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.Preference;

public class BackupSettingsFragment extends Fragment
{
  public BackupSettingsFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    final View view = inflater.inflate(R.layout.fragment_backup_settings, container, false);

    final Activity activity = getActivity();
    if (activity != null)
    {
      final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
      boolean backupEnabled = preferences.getBoolean("backup_enabled", false);

      Switch backupEnabledSwitch = view.findViewById(R.id.backup_enabled_switch);
      backupEnabledSwitch.setChecked(backupEnabled);
      setBackupEnabled(view, backupEnabled);
      backupEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          setBackupEnabled(view, checked);
          preferences.edit().putBoolean("backup_enabled", checked).apply();
        }
      });

      final Spinner automaticBackupSpinner = view.findViewById(R.id.automatic_backup_spinner);
      Preference.get(activity, "automatic_backup", "Weekly", new Preference.ResultRunnable()
      {
        @Override
        public void run()
        {
          for (int i = 0; i < automaticBackupSpinner.getCount(); i++)
          {
            if (automaticBackupSpinner.getItemAtPosition(i).toString().equalsIgnoreCase(getResult()))
            {
              automaticBackupSpinner.setSelection(i);
              break;
            }
          }
        }
      });
      automaticBackupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
      {
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {}

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
        {
          Preference.put(activity, "automatic_backup", automaticBackupSpinner.getSelectedItem().toString(), null);
        }
      });

      final CheckBox wifiOnlyBackupCheck = view.findViewById(R.id.wifi_only_backup_check);
      Preference.get(activity, "wifi_only_backup", String.valueOf(true), new Preference.ResultRunnable()
      {
        @Override
        public void run()
        {
          wifiOnlyBackupCheck.setChecked(Boolean.valueOf(getResult()));
        }
      });
      wifiOnlyBackupCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          Preference.put(activity, "wifi_only_backup", String.valueOf(checked), null);
        }
      });
    }

    return view;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void setBackupEnabled(View rootView, boolean enabled)
  {
    rootView.findViewById(R.id.automatic_backup_label).setEnabled(enabled);
    rootView.findViewById(R.id.automatic_backup_spinner).setEnabled(enabled);
    rootView.findViewById(R.id.wifi_only_backup_label).setEnabled(enabled);
    rootView.findViewById(R.id.wifi_only_backup_check).setEnabled(enabled);
    rootView.findViewById(R.id.apply_backup_button).setEnabled(enabled);
    rootView.findViewById(R.id.backup_now_button).setEnabled(enabled);
  }
}
