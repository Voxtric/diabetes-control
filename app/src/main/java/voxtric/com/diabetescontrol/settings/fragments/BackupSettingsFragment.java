package voxtric.com.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.Preference;

public class BackupSettingsFragment extends GoogleDriveSignInFragment
{
  private boolean m_backupEnabledSwitchForced = false;

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
      boolean backupEnabled = GoogleSignIn.getLastSignedInAccount(activity) != null;

      final Switch backupEnabledSwitch = view.findViewById(R.id.backup_enabled_switch);
      backupEnabledSwitch.setChecked(backupEnabled);
      setBackupEnabled(view, backupEnabled);
      backupEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          setBackupEnabled(view, checked);
          if (!m_backupEnabledSwitchForced)
          {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            if (checked)
            {
              dialogBuilder.setTitle(R.string.title_sign_in)
                  .setMessage(R.string.message_sign_in)
                  .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                  {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                      m_backupEnabledSwitchForced = true;
                      backupEnabledSwitch.setChecked(false);
                      m_backupEnabledSwitchForced = false;
                      setBackupEnabled(backupEnabledSwitch.getRootView(), false);
                    }
                  })
                  .setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener()
                  {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                      // TODO: Perform immediate backup.
                      signIn();
                    }
                  });
            }
            else
            {
              dialogBuilder.setTitle(R.string.title_sign_out)
                  .setMessage(R.string.message_sign_out)
                  .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                  {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                      m_backupEnabledSwitchForced = true;
                      backupEnabledSwitch.setChecked(true);
                      m_backupEnabledSwitchForced = false;
                      setBackupEnabled(backupEnabledSwitch.getRootView(), true);
                    }
                  })
                  .setPositiveButton(R.string.sign_out, new DialogInterface.OnClickListener()
                      {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                          // TODO: Delete backup files.
                          signOut();
                        }
                      });
            }
            dialogBuilder.show();
          }
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
  protected void onSignInSuccess(GoogleSignInAccount googleSignInAccount)
  {
    Toast.makeText(getContext(), getString(R.string.sign_in_success_message, googleSignInAccount.getEmail()), Toast.LENGTH_LONG).show();
  }

  @Override
  protected void onSignInFail()
  {
    Activity activity = getActivity();
    if (activity != null)
    {
      Toast.makeText(activity, R.string.sign_in_fail_message, Toast.LENGTH_LONG).show();
      Switch backupEnabledSwitch = activity.findViewById(R.id.backup_enabled_switch);
      m_backupEnabledSwitchForced = true;
      backupEnabledSwitch.setChecked(false);
      m_backupEnabledSwitchForced = false;
      setBackupEnabled(backupEnabledSwitch.getRootView(), false);
    }
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
