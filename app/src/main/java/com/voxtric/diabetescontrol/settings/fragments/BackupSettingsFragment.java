package com.voxtric.diabetescontrol.settings.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.model.File;

import com.voxtric.diabetescontrol.AwaitRecoveryActivity;
import com.voxtric.diabetescontrol.BackupForegroundService;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.RecoveryForegroundService;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.exporting.ExportForegroundService;
import com.voxtric.diabetescontrol.utilities.GoogleDriveInterface;

public class BackupSettingsFragment extends GoogleDriveSignInFragment
{
  private boolean m_backupEnabledSwitchForced = false;
  private BackupFinishedBroadcastReceiver m_backupFinishedBroadcastReceiver = null;
  private ExportFinishedBroadcastReceiver m_exportFinishedBroadcastReceiver = null;

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
          wifiOnlyBackupCheck.setChecked(Boolean.parseBoolean(getResult()));
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

      final CheckBox notifyBackupCompletionCheck = view.findViewById(R.id.notify_on_backup_finished_check);
      Preference.get(activity, "backup_complete_notify", String.valueOf(false), new Preference.ResultRunnable()
      {
        @Override
        public void run()
        {
          notifyBackupCompletionCheck.setChecked(Boolean.parseBoolean(getResult()));
        }
      });
      notifyBackupCompletionCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked)
        {
          Preference.put(activity, "backup_complete_notify", String.valueOf(checked), null);
        }
      });

      view.findViewById(R.id.backup_button).setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          view.requestFocus();
          startBackup(activity);
        }
      });
      view.findViewById(R.id.restore_button).setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          startRecovery(activity, true);
        }
      });
    }

    return view;
  }

  @Override
  public void onResume()
  {
    super.onResume();

    if (m_backupFinishedBroadcastReceiver == null && BackupForegroundService.isUploading())
    {
      m_backupFinishedBroadcastReceiver = new BackupFinishedBroadcastReceiver();
    }
    if (m_exportFinishedBroadcastReceiver == null && ExportForegroundService.isExporting())
    {
      m_exportFinishedBroadcastReceiver = new ExportFinishedBroadcastReceiver();
    }

    Context context = getContext();
    if (context != null)
    {
      if (m_backupFinishedBroadcastReceiver != null)
      {
        LocalBroadcastManager.getInstance(context).registerReceiver(
            m_backupFinishedBroadcastReceiver,
            new IntentFilter(BackupForegroundService.ACTION_FINISHED));
      }
      if (m_exportFinishedBroadcastReceiver != null)
      {
        LocalBroadcastManager.getInstance(context).registerReceiver(
            m_exportFinishedBroadcastReceiver,
            new IntentFilter(ExportForegroundService.ACTION_FINISHED));
      }
    }
  }

  @Override
  public void onPause()
  {
    Context context = getContext();
    if (context != null)
    {
      if (m_backupFinishedBroadcastReceiver != null)
      {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(m_backupFinishedBroadcastReceiver);
      }
      if (m_exportFinishedBroadcastReceiver != null)
      {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(m_exportFinishedBroadcastReceiver);
      }
    }
    super.onPause();
  }

  @Override
  protected void onSignInSuccess(GoogleSignInAccount googleSignInAccount)
  {
    Toast.makeText(getContext(), getString(R.string.sign_in_success_message, googleSignInAccount.getEmail()), Toast.LENGTH_LONG).show();
    final Activity activity = getActivity();
    if (activity != null)
    {
      checkAutoBackupSafe(activity);

      String contactName = googleSignInAccount.getDisplayName();
      EditText contactNameEditText = activity.findViewById(R.id.edit_text_contact_name);
      if (contactNameEditText.getText().length() == 0)
      {
        contactNameEditText.setText(contactName);
        final String viewName = contactNameEditText.getResources().getResourceName(contactNameEditText.getId());
        Preference.put(activity, viewName, contactName, null);
      }
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

  @Override
  protected void onSignOut()
  {
    Toast.makeText(getActivity(), R.string.sign_out_message, Toast.LENGTH_LONG).show();
  }

  private void setBackupEnabled(View rootView, boolean enabled)
  {
    rootView.findViewById(R.id.automatic_backup_label).setEnabled(enabled);
    rootView.findViewById(R.id.automatic_backup_spinner).setEnabled(enabled);
    rootView.findViewById(R.id.wifi_only_backup_label).setEnabled(enabled);
    rootView.findViewById(R.id.wifi_only_backup_check).setEnabled(enabled);
    rootView.findViewById(R.id.notify_on_backup_finished_label).setEnabled(enabled);
    rootView.findViewById(R.id.notify_on_backup_finished_check).setEnabled(enabled);
    boolean enableBackupButtons = enabled &&
        !BackupForegroundService.isUploading() &&
        !RecoveryForegroundService.isDownloading() &&
        !ExportForegroundService.isExporting();
    rootView.findViewById(R.id.restore_button).setEnabled(enableBackupButtons);
    rootView.findViewById(R.id.backup_button).setEnabled(enableBackupButtons);
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
                .setNegativeButton(R.string.cancel_dialog_option, new DialogInterface.OnClickListener()
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
                .setPositiveButton(R.string.sign_in_dialog_option, new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i)
                  {
                    signIn();
                  }
                });
          }
          else
          {
            dialogBuilder.setTitle(R.string.title_sign_out)
                .setMessage(R.string.message_sign_out)
                .setNegativeButton(R.string.cancel_dialog_option, new DialogInterface.OnClickListener()
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
                .setPositiveButton(R.string.sign_out_dialog_option, new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i)
                  {
                    AsyncTask.execute(new Runnable()
                    {
                      @Override
                      public void run()
                      {
                        GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(activity, GoogleSignIn.getLastSignedInAccount(activity));
                        googleDriveInterface.deleteFile("Diabetes Control");
                        signOut();
                      }
                    });
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

  private void checkAutoBackupSafe(final Activity activity)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
        if (account != null)
        {
          GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(activity, account);
          final GoogleDriveInterface.Result<File> result = googleDriveInterface.getFileMetadata(
              String.format("%s/%s", getString(R.string.app_name), AppDatabase.NAME.replace(".db", ".zip")));
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              switch (result.result)
              {
              case GoogleDriveInterface.RESULT_SUCCESS:
                AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(R.string.title_auto_backup_safe)
                    .setMessage(R.string.message_auto_backup_safe)
                    .setNegativeButton(R.string.overwrite_dialog_option, new DialogInterface.OnClickListener()
                    {
                      @Override
                      public void onClick(DialogInterface dialogInterface, int i)
                      {
                        startBackup(activity);
                      }
                    })
                    .setPositiveButton(R.string.recover_dialog_option, new DialogInterface.OnClickListener()
                    {
                      @Override
                      public void onClick(DialogInterface dialogInterface, int i)
                      {
                        startRecovery(activity, false);
                      }
                    })
                    .create();
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                break;
              case GoogleDriveInterface.RESULT_PARENT_FOLDER_MISSING:
              case GoogleDriveInterface.RESULT_FILE_MISSING:
                startBackup(activity);
                break;
              }
            }
          });
        }
      }
    });
  }

  private void startBackup(final Activity activity)
  {
    Intent intent = new Intent(activity, BackupForegroundService.class);
    activity.startService(intent);
    Toast.makeText(activity, R.string.backup_started_message, Toast.LENGTH_LONG).show();

    Button backupNowButton = activity.findViewById(R.id.backup_button);
    backupNowButton.setEnabled(false);
    Button applyBackupButton = activity.findViewById(R.id.restore_button);
    applyBackupButton.setEnabled(false);

    m_backupFinishedBroadcastReceiver = new BackupFinishedBroadcastReceiver();
    LocalBroadcastManager.getInstance(activity).registerReceiver(m_backupFinishedBroadcastReceiver, new IntentFilter(BackupForegroundService.ACTION_FINISHED));
  }

  private void startRecovery(final Activity activity, boolean confirmWithUser)
  {
    if (confirmWithUser)
    {
      AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.title_backup_recovery)
          .setMessage(R.string.message_backup_recovery)
          .setNegativeButton(R.string.cancel_dialog_option, null)
          .setPositiveButton(R.string.recover_dialog_option, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
              startRecovery(activity, false);
            }
          })
          .create();
      dialog.show();
    }
    else
    {
      Intent intent = new Intent(activity, RecoveryForegroundService.class);
      activity.startService(intent);
      ((AwaitRecoveryActivity)activity).launchRecoveryWaitDialog();
    }
  }

  private void configureBackupButtonsEnabled(Activity activity)
  {
    boolean backupButtonsEnabled = GoogleSignIn.getLastSignedInAccount(activity) != null && !ExportForegroundService.isExporting();
    Button backupNowButton = activity.findViewById(R.id.backup_button);
    backupNowButton.setEnabled(backupButtonsEnabled);
    Button applyBackupButton = activity.findViewById(R.id.restore_button);
    applyBackupButton.setEnabled(backupButtonsEnabled);
  }

  private class BackupFinishedBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
      m_backupFinishedBroadcastReceiver = null;

      Activity activity = getActivity();
      if (activity != null)
      {
        configureBackupButtonsEnabled(activity);

        if (getString(R.string.backup_success_notification_title).equals(intent.getStringExtra("message_title")))
        {
          Toast.makeText(activity, R.string.backup_succeeded_message, Toast.LENGTH_LONG).show();
        }
        else
        {
          Toast.makeText(activity, R.string.backup_failed_message, Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  private class ExportFinishedBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
      m_exportFinishedBroadcastReceiver = null;

      Activity activity = getActivity();
      if (activity != null)
      {
        configureBackupButtonsEnabled(activity);
      }
    }
  }
}
