package voxtric.com.diabetescontrol.settings.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import voxtric.com.diabetescontrol.BackupForegroundService;
import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.Preference;

public class BackupSettingsFragment extends GoogleDriveSignInFragment
{
  private boolean m_backupEnabledSwitchForced = false;

  private static final int REQUEST_PERMISSION_FOREGROUND_SERVICE = 114;

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
      initialiseBackupEnabledSwitch(activity, view, backupEnabled);
      initialiseAutomaticBackupSpinner(activity, view);

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

      view.findViewById(R.id.backup_now_button).setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          startBackup(activity);
        }
      });
    }

    return view;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    if (requestCode == REQUEST_PERMISSION_FOREGROUND_SERVICE)
    {
      Activity activity = getActivity();
      if (activity != null)
      {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
          startBackup(activity);
        }
        else
        {
          Toast.makeText(activity, R.string.foreground_service_permission_needed_message, Toast.LENGTH_LONG).show();
          signOut();
        }
      }
    }
    else
    {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void onSignInSuccess(GoogleSignInAccount googleSignInAccount)
  {
    Toast.makeText(getContext(), getString(R.string.sign_in_success_message, googleSignInAccount.getEmail()), Toast.LENGTH_LONG).show();
    final Activity activity = getActivity();
    if (activity != null && hasForegroundServicePermission(activity))
    {
      startBackup(activity);
    }
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
    rootView.findViewById(R.id.backup_now_button).setEnabled(enabled && !BackupForegroundService.isUploading());
  }

  private void initialiseBackupEnabledSwitch(final Activity activity, final View view, boolean backupEnabled)
  {
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
  }

  private void initialiseAutomaticBackupSpinner(final Activity activity, final View view)
  {

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
  }

  private boolean hasForegroundServicePermission(final Activity activity)
  {
    boolean hasPermission = true;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
    {
      int hasForegroundServicePermission = activity.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE);
      if (hasForegroundServicePermission != PackageManager.PERMISSION_GRANTED)
      {
        hasPermission = false;
        if (shouldShowRequestPermissionRationale(Manifest.permission.FOREGROUND_SERVICE))
        {
          androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(activity)
              .setTitle(R.string.permission_justification_title)
              .setMessage(R.string.foreground_service_permission_justification_message)
              .setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  Toast.makeText(activity, R.string.foreground_service_permission_needed_message, Toast.LENGTH_LONG).show();
                  signOut();
                }
              })
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_FOREGROUND_SERVICE);
                }
              })
              .create();
          dialog.show();
        }
        else
        {
          requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_FOREGROUND_SERVICE);
        }
      }
    }
    return hasPermission;
  }

  private void startBackup(final Activity activity)
  {
    Intent intent = new Intent(activity, BackupForegroundService.class);
    activity.startService(intent);
    Toast.makeText(activity, R.string.backup_started_message, Toast.LENGTH_LONG).show();

    final Button backupNowButton = activity.findViewById(R.id.backup_now_button);
    backupNowButton.setEnabled(false);
    new CountDownTimer(1000, 1)
    {
      @Override
      public void onTick(long l) {}

      @Override
      public void onFinish()
      {
        if (BackupForegroundService.isUploading())
        {
          start();
        }
        else
        {
          backupNowButton.setEnabled(GoogleSignIn.getLastSignedInAccount(activity) != null);
        }
      }
    }.start();
  }
}
