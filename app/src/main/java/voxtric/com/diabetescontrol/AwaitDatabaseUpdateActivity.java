package voxtric.com.diabetescontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public abstract class AwaitDatabaseUpdateActivity extends AppCompatActivity
{
  private AlertDialog m_waitDialog = null;
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
        .setMessage(R.string.message_recovery_wait)
        .create();
    m_waitDialog.setCancelable(false);
    m_waitDialog.setCanceledOnTouchOutside(false);
    m_waitDialog.show();
    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_recoveryCompleteBroadcastReceiver,
        new IntentFilter(RecoveryForegroundService.ACTION_COMPLETE));
  }

  public void cancelWaitDialog()
  {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_recoveryCompleteBroadcastReceiver);
    m_waitDialog.cancel();
    m_waitDialog = null;
  }

  public class RecoveryCompleteBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      cancelWaitDialog();
      recreate();
    }
  }
}
