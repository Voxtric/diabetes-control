package voxtric.com.diabetescontrol;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.ForegroundService;
import voxtric.com.diabetescontrol.utilities.GoogleDriveInterface;

public class BackupForegroundService extends ForegroundService implements MediaHttpUploaderProgressListener
{
  private static final String TAG = "BackupForegroundService";

  public static final String ACTION_ONGOING = "voxtric.com.diabetescontrol.BackupForegroundService.ACTION_ONGOING";
  public static final String ACTION_FINISHED = "voxtric.com.diabetescontrol.BackupForegroundService.ACTION_FINISHED";

  private static final String ONGOING_CHANNEL_ID = "OngoingBackupForegroundServiceChannel";
  private static final String ONGOING_CHANNEL_NAME = "Ongoing Backup Foreground Service";
  private static final int ONGOING_NOTIFICATION_ID = 1091;

  private static final String FINISHED_CHANNEL_ID = "FinishedBackupForegroundServiceChannel";
  private static final String FINISHED_CHANNEL_NAME = "Finished Backup Foreground Service";
  private static final int FINISHED_NOTIFICATION_ID = 1092;

  private static final int MAX_UPLOAD_PROGRESS = 100;
  private static final int ZIP_ENTRY_BUFFER_SIZE = 1024 * 1024; // 1 MiB

  private static int s_progress = -1;
  public static boolean isUploading()
  {
    return s_progress != -1;
  }
  public static int getProgress()
  {
    return s_progress;
  }

  @StringRes
  private int m_failureMessageId = -1;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    super.onStartCommand(intent, flags, startId);

    if (!isUploading())
    {
      s_progress = 0;
      createNotificationChannel(ONGOING_CHANNEL_ID, ONGOING_CHANNEL_NAME, true);
      createNotificationChannel(FINISHED_CHANNEL_ID, FINISHED_CHANNEL_NAME, false);
      startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(0));

      Thread thread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          boolean success = false;
          byte[] zipBytes = createZipBackup();
          if (zipBytes != null)
          {
            success = uploadZipBackup(zipBytes);
          }

          s_progress = -1;
          stopForeground(true);
          stopSelf();

          Intent backupFinishedIntent = new Intent(ACTION_FINISHED);
          if (success)
          {
            backupFinishedIntent.putExtra("message_title_id", R.string.backup_success_notification_title);
            backupFinishedIntent.putExtra("message_text_id", R.string.backup_success_notification_text);
          }
          else
          {
            backupFinishedIntent.putExtra("message_title_id", R.string.backup_fail_notification_title);
            backupFinishedIntent.putExtra("message_text_id", m_failureMessageId);
          }
          LocalBroadcastManager.getInstance(BackupForegroundService.this).sendBroadcast(backupFinishedIntent);
        }
      });
      thread.setDaemon(true);
      thread.start();
    }

    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  private byte[] createZipBackup()
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outputStream));
    byte[] dataBuffer = new byte[ZIP_ENTRY_BUFFER_SIZE];

    try
    {
      String[] affixes = new String[]{ "", "-shm", "-wal" };
      for (String affix : affixes)
      {
        File databaseFile = new File(getDatabasePath(AppDatabase.NAME).getAbsolutePath() + affix);
        FileInputStream fileInputStream = new FileInputStream(databaseFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, ZIP_ENTRY_BUFFER_SIZE);

        ZipEntry zipEntry = new ZipEntry(databaseFile.getName());
        zipOutputStream.putNextEntry(zipEntry);
        int count;
        while ((count = bufferedInputStream.read(dataBuffer, 0, ZIP_ENTRY_BUFFER_SIZE)) != -1)
        {
          zipOutputStream.write(dataBuffer, 0, count);
        }
        bufferedInputStream.close();
      }
      zipOutputStream.close();
    }
    catch (FileNotFoundException exception)
    {
      Log.e(TAG, getString(R.string.backup_find_database_fail_notification_text), exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_find_database_fail_notification_text));
      return null;
    }
    catch (IOException exception)
    {
      Log.e(TAG, getString(R.string.backup_create_file_fail_notification_text), exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_create_file_fail_notification_text));
      return null;
    }
    return outputStream.toByteArray();
  }

  private boolean uploadZipBackup(byte[] zipBytes)
  {
    boolean success = false;
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(BackupForegroundService.this);
    if (account != null)
    {
      GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(BackupForegroundService.this, account);
      int result = googleDriveInterface.uploadFile(
          String.format("%s/%s", getString(R.string.app_name), AppDatabase.NAME.replace(".db", ".zip")),
          "application/zip",
          zipBytes,
          this);

      boolean notifyOnFinished = true;
      Preference preference = AppDatabase.getInstance().preferencesDao().getPreference("backup_complete_notify");
      if (preference != null)
      {
        notifyOnFinished = Boolean.valueOf(preference.value);
      }

      switch (result)
      {
      case GoogleDriveInterface.RESULT_SUCCESS:
        if (notifyOnFinished)
        {
          pushNotification(FINISHED_NOTIFICATION_ID, buildOnSuccessNotification());
        }
        success = true;
        break;
      case GoogleDriveInterface.RESULT_AUTHENTICATION_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_sign_in_fail_notification_text));
        break;
      case GoogleDriveInterface.RESULT_CONNECTION_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_no_connection_text));
        break;
      case GoogleDriveInterface.RESULT_TIMEOUT_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_request_timeout_text));
        break;
      case GoogleDriveInterface.RESULT_INTERRUPT_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_upload_interrupted_notification_text));
        break;
      case GoogleDriveInterface.RESULT_SPACE_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_no_space_notification_text));
        break;
      case GoogleDriveInterface.RESULT_UNKNOWN_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_upload_fail_notification_text));
        break;
      default:
        throw new RuntimeException("Unknown Google Drive Interface upload result.");
      }
    }
    else
    {
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_sign_in_fail_notification_text));
    }
    return success;
  }

  @Override
  protected Notification buildOngoingNotification(int progress)
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(ACTION_ONGOING);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    return new NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
        .setSmallIcon(R.drawable.upload)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.backing_up_notification_title))
        .setProgress(MAX_UPLOAD_PROGRESS, progress, progress == 0)
        .build();
  }

  @Override
  protected Notification buildOnSuccessNotification()
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(ACTION_FINISHED);
    notificationIntent.putExtra("message_title_id", R.string.backup_success_notification_title);
    notificationIntent.putExtra("message_text_id", R.string.backup_success_notification_text);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.done)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(R.string.backup_success_notification_title))
        .setContentText(getString(R.string.backup_success_notification_text))
        .build();
  }

  @Override
  protected Notification buildOnFailNotification(int failureMessageId)
  {
    m_failureMessageId = failureMessageId;

    Intent notificationIntent = new Intent(this, SettingsActivity.class);
    notificationIntent.setAction(ACTION_FINISHED);
    notificationIntent.putExtra("message_title_id", R.string.backup_fail_notification_title);
    notificationIntent.putExtra("message_text_id", failureMessageId);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addNextIntentWithParentStack(notificationIntent);
    PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.error)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(R.string.backup_fail_notification_title))
        .setContentText(getString(failureMessageId))
        .build();
  }

  @Override
  public void progressChanged(MediaHttpUploader uploader) throws IOException
  {
    s_progress = (int)(uploader.getProgress() * 100.0);
    pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(s_progress));

    Intent backupOngoingBroadcast = new Intent(ACTION_ONGOING);
    LocalBroadcastManager.getInstance(this).sendBroadcast(backupOngoingBroadcast);
  }
}
