package com.voxtric.diabetescontrol;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.voxtric.diabetescontrol.utilities.ViewUtilities;

public abstract class AwaitRecoveryActivity extends AppCompatActivity
{
  private AlertDialog m_recoveryWaitDialog = null;
  private RecoveryOngoingBroadcastReceiver m_recoveryOngoingBroadcastReceiver = new RecoveryOngoingBroadcastReceiver();
  private RecoveryCompleteBroadcastReceiver m_recoveryCompleteBroadcastReceiver = new RecoveryCompleteBroadcastReceiver();

  @Override
  protected void onStart()
  {
    super.onStart();

    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
    boolean hasRecoveryMessage = preferences.getBoolean("has_recovery_message", false);
    if (hasRecoveryMessage)
    {
      String messageTitle = preferences.getString("message_title", getString(R.string.title_undefined));
      String messageText = preferences.getString("message_text", getString(R.string.message_undefined));
      final int notificationId = preferences.getInt("recovery_notification_id", -1);
      ViewUtilities.launchMessageDialog(AwaitRecoveryActivity.this, messageTitle, messageText, new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialogInterface, int i)
        {
          NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
          if (notificationManager != null)
          {
            notificationManager.cancel(notificationId);
          }
        }
      });
    }
    SharedPreferences.Editor preferencesEditor = preferences.edit();
    preferencesEditor.putBoolean("has_recovery_message", false);
    preferencesEditor.apply();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (RecoveryForegroundService.isDownloading())
    {
      launchRecoveryWaitDialog();
    }
  }

  @Override
  protected void onPause()
  {
    if (m_recoveryWaitDialog != null)
    {
      cancelRecoveryWaitDialog();
    }
    super.onPause();
  }

  public void launchRecoveryWaitDialog()
  {
    if (m_recoveryWaitDialog != null)
    {
      cancelRecoveryWaitDialog();
    }

    m_recoveryWaitDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_recovery_wait)
        .setView(R.layout.dialog_service_ongoing)
        .create();
    m_recoveryWaitDialog.setCancelable(false);
    m_recoveryWaitDialog.setCanceledOnTouchOutside(false);
    m_recoveryWaitDialog.show();
    TextView message = m_recoveryWaitDialog.findViewById(R.id.message);
    if (message != null)
    {
      message.setText(R.string.message_recovery_wait);
    }
    updateRecoveryWaitDialogProgress();

    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_recoveryOngoingBroadcastReceiver,
        new IntentFilter(RecoveryForegroundService.ACTION_ONGOING));
    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_recoveryCompleteBroadcastReceiver,
        new IntentFilter(RecoveryForegroundService.ACTION_FINISHED));
  }

  public void cancelRecoveryWaitDialog()
  {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_recoveryOngoingBroadcastReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_recoveryCompleteBroadcastReceiver);
    m_recoveryWaitDialog.cancel();
    m_recoveryWaitDialog = null;
  }

  private void updateRecoveryWaitDialogProgress()
  {
    if (m_recoveryWaitDialog != null && RecoveryForegroundService.isDownloading())
    {
      ProgressBar progressBar = m_recoveryWaitDialog.findViewById(R.id.progress);
      if (progressBar != null)
      {
        int progress = RecoveryForegroundService.getProgress();
        progressBar.setIndeterminate(progress == 0 || progress == RecoveryForegroundService.MAX_DOWNLOAD_PROGRESS);
        progressBar.setProgress(progress);
      }
    }
  }

  private class RecoveryOngoingBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      updateRecoveryWaitDialogProgress();
    }
  }

  private class RecoveryCompleteBroadcastReceiver extends BroadcastReceiver
  {
    @SuppressLint("ApplySharedPref")
    @Override
    public void onReceive(Context context, Intent intent)
    {
      cancelRecoveryWaitDialog();
      String messageTitle = intent.getStringExtra("message_title");
      if (messageTitle == null)
      {
        messageTitle = getString(R.string.title_undefined);
      }
      String messageText = intent.getStringExtra("message_text");
      if (messageText == null)
      {
        messageText = getString(R.string.message_undefined);
      }
      final int notificationId = intent.getIntExtra("notification_id", -1);
      if (messageTitle.equals(getString(R.string.recovery_success_notification_title)))
      {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();
        preferencesEditor.putString("recovery_message_title", messageTitle);
        preferencesEditor.putString("recovery_message_text", messageText);
        preferencesEditor.putInt("recovery_notification_id", notificationId);
        preferencesEditor.putBoolean("has_recovery_message", true);
        preferencesEditor.commit();
        recreate();
      }
      else
      {
        ViewUtilities.launchMessageDialog(AwaitRecoveryActivity.this, messageTitle, messageText,
            new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialogInterface, int i)
              {
                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null)
                {
                  notificationManager.cancel(notificationId);
                }
              }
            });
      }
    }
  }
}
