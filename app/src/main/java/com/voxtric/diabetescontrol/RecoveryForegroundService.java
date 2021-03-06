package com.voxtric.diabetescontrol;

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
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.ForegroundService;
import com.voxtric.diabetescontrol.utilities.GoogleDriveInterface;

public class RecoveryForegroundService extends ForegroundService implements MediaHttpDownloaderProgressListener
{
  @SuppressWarnings("SpellCheckingInspection")
  private static final String TAG = "RecoveryForegroundServi";

  public static final String ACTION_ONGOING = "voxtric.voxtric.diabetescontrol.RecoveryForegroundService.ACTION_ONGOING";
  public static final String ACTION_FINISHED = "voxtric.voxtric.diabetescontrol.RecoveryForegroundService.ACTION_FINISHED";

  private static final String ONGOING_CHANNEL_ID = "OngoingRecoveryForegroundServiceChannel";
  private static final String ONGOING_CHANNEL_NAME = "Ongoing Recovery Foreground Service";
  private static final int ONGOING_NOTIFICATION_ID = 1093;

  private static final String FINISHED_CHANNEL_ID = "FinishedRecoveryForegroundServiceChannel";
  private static final String FINISHED_CHANNEL_NAME = "Finished Recovery Foreground Service";
  private static final int FINISHED_NOTIFICATION_ID = 1094;
  public static final int MAX_DOWNLOAD_PROGRESS = 100;

  private static int s_progress = -1;
  public static boolean isDownloading()
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

    if (!isDownloading())
    {
      s_progress = 0;
      createNotificationChannel(ONGOING_CHANNEL_ID, ONGOING_CHANNEL_NAME, true);
      createNotificationChannel(FINISHED_CHANNEL_ID, FINISHED_CHANNEL_NAME, false);
      startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(0));
      cancelNotification(FINISHED_NOTIFICATION_ID);

      Thread thread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          boolean success = false;
          byte[] zipBytes = downloadZipBackup();
          if (zipBytes != null)
          {
            success = unpackZipBackup(zipBytes);
          }
          s_progress = -1;

          Intent recoveryFinishedBroadcast = new Intent(ACTION_FINISHED);
          if (success)
          {
            recoveryFinishedBroadcast.putExtra("message_title", getString(R.string.recovery_success_notification_title));
            recoveryFinishedBroadcast.putExtra("message_text", getString(R.string.recovery_success_notification_text));
          }
          else
          {
            recoveryFinishedBroadcast.putExtra("message_title", getString(R.string.recovery_fail_notification_title));
            recoveryFinishedBroadcast.putExtra("message_text", getString(m_failureMessageId));
          }
          recoveryFinishedBroadcast.putExtra("notification_id", FINISHED_NOTIFICATION_ID);
          LocalBroadcastManager.getInstance(RecoveryForegroundService.this).sendBroadcast(recoveryFinishedBroadcast);

          stopForeground(true);
          stopSelf();
        }
      });
      thread.setDaemon(false);
      thread.start();
    }

    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  private byte[] downloadZipBackup()
  {
    byte[] zipBytes = null;
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(RecoveryForegroundService.this);
    if (account != null)
    {
      GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(RecoveryForegroundService.this, account);
      GoogleDriveInterface.Result<byte[]> result = googleDriveInterface.downloadFile(
          String.format("%s/%s", getString(R.string.app_name), AppDatabase.NAME.replace(".db", ".zip")),
          this);
      switch (result.result)
      {
      case GoogleDriveInterface.RESULT_SUCCESS:
        zipBytes = result.returned;
        break;
      case GoogleDriveInterface.RESULT_PARENT_FOLDER_MISSING:
      case GoogleDriveInterface.RESULT_FILE_MISSING:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_missing_backup_text));
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
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_download_interrupted_notification_text));
        break;
      case GoogleDriveInterface.RESULT_UNKNOWN_ERROR:
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_download_fail_notification_text));
        break;
      default:
        throw new RuntimeException("Unknown Google Drive Interface download result.");
      }
    }
    else
    {
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_sign_in_fail_notification_text));
    }
    return zipBytes;
  }

  private boolean unpackZipBackup(byte[] zipBytes)
  {
    boolean success = false;
    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    File databaseDirectory = new File(getDatabasePath(AppDatabase.NAME).getAbsolutePath()).getParentFile();
    if (databaseDirectory != null)
    {
      try
      {
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null)
        {
          File databaseFile = new File(databaseDirectory, zipEntry.getName() + ".tmp");
          if (!databaseFile.getAbsolutePath().startsWith(databaseDirectory.getAbsolutePath()))
          {
            throw new SecurityException("Zip Path Traversal Vulnerability.");
          }
          FileOutputStream outputStream = new FileOutputStream(databaseFile);
          int data = zipInputStream.read();
          while (data != -1)
          {
            outputStream.write(data);
            data = zipInputStream.read();
          }
          zipInputStream.closeEntry();
          outputStream.close();
        }
        success = true;

        File[] recoveryFiles = databaseDirectory.listFiles(new FilenameFilter()
        {
          @Override
          public boolean accept(File file, String fileName)
          {
            return fileName.endsWith(".tmp");
          }
        });
        if (recoveryFiles != null)
        {
          for (int i = 0; i < recoveryFiles.length && success; i++)
          {
            File recoveryFile = recoveryFiles[i];
            success = recoveryFile.renameTo(new File(recoveryFile.getAbsolutePath().replace(".tmp", "")));
          }
        }

        if (success)
        {
          AppDatabase.initialise(this);
          BackupForegroundService.updateLastSuccessfulBackupPreference();
          pushNotification(FINISHED_NOTIFICATION_ID, buildOnSuccessNotification());
        }
        else
        {
          pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_file_rename_fail_notification_text));
        }
      }
      catch (SecurityException exception)
      {
        Log.e(TAG, "Recovery Security Exception", exception);
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_zip_bomb_notification_text));
      }
      catch (IOException exception)
      {
        boolean handled = false;
        String exceptionMessage = exception.getMessage();
        if (exceptionMessage != null)
        {
          if (exception.getMessage().contains("No space left on device"))
          {
            Log.v(TAG, "Recovery: " + getString(R.string.storage_space_fail_notification_text), exception);
            pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.storage_space_fail_notification_text));
            handled = true;
          }
        }

        if (!handled)
        {
          Log.e(TAG, "Recovery IO Exception", exception);
          pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_read_file_fail_notification_text));
        }
      }
      finally
      {
        File[] tempFiles = databaseDirectory.listFiles(new FilenameFilter()
        {
          @Override
          public boolean accept(File file, String fileName)
          {
            return fileName.endsWith(".tmp");
          }
        });
        if (tempFiles != null)
        {
          for (File tempFile : tempFiles)
          {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
          }
        }
      }
    }
    else
    {
      Log.e(TAG, "Couldn't find database directory");
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_no_database_directory_notification_text));
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
        .setSmallIcon(R.drawable.downloading)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.recovering_notification_title))
        .setProgress(MAX_DOWNLOAD_PROGRESS, progress, progress == 0|| progress == MAX_DOWNLOAD_PROGRESS)
        .build();
  }

  @Override
  protected Notification buildOnSuccessNotification()
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(ACTION_FINISHED);
    notificationIntent.putExtra("message_title", getString(R.string.recovery_success_notification_title));
    notificationIntent.putExtra("message_text", getString(R.string.recovery_success_notification_text));
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.done)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(R.string.recovery_success_notification_title))
        .setContentText(getString(R.string.recovery_success_notification_text))
        .build();
  }

  @Override
  protected Notification buildOnFailNotification(int failureMessageId)
  {
    m_failureMessageId = failureMessageId;

    Intent notificationIntent = new Intent(this, SettingsActivity.class);
    notificationIntent.setAction(ACTION_FINISHED);
    notificationIntent.putExtra("message_title", getString(R.string.recovery_fail_notification_title));
    notificationIntent.putExtra("message_text", getString(failureMessageId));
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    stackBuilder.addNextIntentWithParentStack(notificationIntent);
    PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.error)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(R.string.recovery_fail_notification_title))
        .setContentText(getString(failureMessageId))
        .build();
  }

  @Override
  public void progressChanged(MediaHttpDownloader downloader)
  {
    s_progress = (int)(downloader.getProgress() * 100.0);
    pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(s_progress));

    Intent recoveryOngoingBroadcast = new Intent(ACTION_ONGOING);
    LocalBroadcastManager.getInstance(this).sendBroadcast(recoveryOngoingBroadcast);
  }
}
