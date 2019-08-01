package voxtric.com.diabetescontrol;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.ForegroundService;
import voxtric.com.diabetescontrol.utilities.GoogleDriveInterface;

public class RecoveryForegroundService extends ForegroundService implements MediaHttpDownloaderProgressListener
{
  private static final String CHANNEL_ID = "RecoveryForegroundServiceChannel";
  private static final String CHANNEL_NAME = "Recovery Foreground Service";
  private static final int ONGOING_NOTIFICATION_ID = 1093;
  private static final int FINISHED_NOTIFICATION_ID = 1094;
  private static final int MAX_DOWNLOAD_PROGRESS = 100;

  private static boolean s_isDownloading = false;
  public static boolean isDownloading()
  {
    return s_isDownloading;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    super.onStartCommand(intent, flags, startId);

    if (!isDownloading())
    {
      s_isDownloading = true;
      createNotificationChannel(CHANNEL_ID, CHANNEL_NAME);
      startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(0));

      Thread thread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          byte[] zipBytes = downloadZipBackup();
          if (zipBytes != null)
          {
            unpackZipBackup(zipBytes);
          }

          stopForeground(true);
          stopSelf();
          s_isDownloading = false;
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

  private byte[] downloadZipBackup()
  {
    byte[] zipBytes = null;
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(RecoveryForegroundService.this);
    if (account != null)
    {
      GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(RecoveryForegroundService.this, account);
      zipBytes = googleDriveInterface.downloadFile(
          String.format("%s/%s", getString(R.string.app_name), AppDatabase.NAME.replace(".db", ".zip")),
          this);
      if (zipBytes == null)
      {
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_upload_fail_notification_text));
      }
    }
    else
    {
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_sign_in_fail_notification_text));
    }
    return zipBytes;
  }

  private void unpackZipBackup(byte[] zipBytes)
  {
    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
    try
    {
      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null)
      {
        String databaseDirectory = new File(getDatabasePath(AppDatabase.NAME).getAbsolutePath()).getParent();
        FileOutputStream outputStream = new FileOutputStream(new File(databaseDirectory, zipEntry.getName()));
        int data = zipInputStream.read();
        while (data != -1)
        {
          outputStream.write(data);
          data = zipInputStream.read();
        }
        zipInputStream.closeEntry();
        outputStream.close();
      }
      AppDatabase.initialise(this);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnSuccessNotification());
    }
    catch (IOException exception)
    {
      Log.e("RecoveryForegroundServi", getString(R.string.recovery_read_file_fail_notification_text), exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.recovery_read_file_fail_notification_text));
    }
  }

  @Override
  protected Notification buildOngoingNotification(int progress)
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.download)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.recovering_notification_title))
        .setProgress(MAX_DOWNLOAD_PROGRESS, progress, progress == MAX_DOWNLOAD_PROGRESS)
        .build();
  }

  @Override
  protected Notification buildOnSuccessNotification()
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.done)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.recovery_success_notification_title))
        .setContentText(getString(R.string.recovery_success_notification_text))
        .build();
  }

  @Override
  protected Notification buildOnFailNotification(int failureMessageId)
  {
    Intent notificationIntent = new Intent(this, SettingsActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.error)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.recovery_fail_notification_title))
        .setContentText(getString(failureMessageId))
        .build();
  }

  @Override
  public void progressChanged(MediaHttpDownloader downloader)
  {
    int progress = (int)(downloader.getProgress() * 100.0);
    pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(progress));
  }
}