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

public abstract class AwaitDatabaseUpdateActivity extends AppCompatActivity
{
  private AlertDialog m_waitDialog = null;
  private RecoveryOngoingBroadcastReceiver m_recoveryOngoingBroadcastReceiver = new RecoveryOngoingBroadcastReceiver();
  private RecoveryCompleteBroadcastReceiver m_recoveryCompleteBroadcastReceiver = new RecoveryCompleteBroadcastReceiver();

  @Override
  protected void onResume()
  {
    super.onResume();
    if (RecoveryForegroundService.isDownloading())
    {
      launchWaitDialog();
    }
  }

  @Override
  protected void onPause()
  {
    if (m_waitDialog != null)
    {
      cancelWaitDialog();
    }
    super.onPause();
  }

  public void launchWaitDialog()
  {
    if (m_waitDialog != null)
    {
      cancelWaitDialog();
    }
    m_waitDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_recovery_wait)
        .setView(R.layout.dialog_backup_recovery_ongoing)
        .create();
    m_waitDialog.setCancelable(false);
    m_waitDialog.setCanceledOnTouchOutside(false);
    m_waitDialog.show();
    TextView message = m_waitDialog.findViewById(R.id.message);
    if (message != null)
    {
      message.setText(R.string.message_recovery_wait);
    }
    updateProgress();

    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_recoveryCompleteBroadcastReceiver,
        new IntentFilter(RecoveryForegroundService.ACTION_FINISHED));
    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_recoveryOngoingBroadcastReceiver,
        new IntentFilter(RecoveryForegroundService.ACTION_ONGOING));
  }

  public void cancelWaitDialog()
  {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_recoveryOngoingBroadcastReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_recoveryCompleteBroadcastReceiver);
    m_waitDialog.cancel();
    m_waitDialog = null;
  }

  private void updateProgress()
  {
    if (m_waitDialog != null && RecoveryForegroundService.isDownloading())
    {
      ProgressBar progressBar = m_waitDialog.findViewById(R.id.progress);
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
      updateProgress();
    }
  }

  private class RecoveryCompleteBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      cancelWaitDialog();
      recreate();
      Toast.makeText(AwaitDatabaseUpdateActivity.this, R.string.recovery_finished_message, Toast.LENGTH_LONG).show();
    }
  }
}
