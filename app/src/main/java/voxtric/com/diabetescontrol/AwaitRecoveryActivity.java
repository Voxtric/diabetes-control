package voxtric.com.diabetescontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class AwaitRecoveryActivity extends AppCompatActivity
{
  private AlertDialog m_recoveryWaitDialog = null;
  private RecoveryOngoingBroadcastReceiver m_recoveryOngoingBroadcastReceiver = new RecoveryOngoingBroadcastReceiver();
  private RecoveryCompleteBroadcastReceiver m_recoveryCompleteBroadcastReceiver = new RecoveryCompleteBroadcastReceiver();

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
        .setView(R.layout.dialog_backup_recovery_ongoing)
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
    @Override
    public void onReceive(Context context, Intent intent)
    {
      cancelRecoveryWaitDialog();
      recreate();
      Toast.makeText(AwaitRecoveryActivity.this, R.string.recovery_finished_message, Toast.LENGTH_LONG).show();
    }
  }
}
