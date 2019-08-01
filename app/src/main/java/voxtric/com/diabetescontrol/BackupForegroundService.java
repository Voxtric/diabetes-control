package voxtric.com.diabetescontrol;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

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
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.ForegroundService;
import voxtric.com.diabetescontrol.utilities.GoogleDriveInterface;

public class BackupForegroundService extends ForegroundService implements MediaHttpUploaderProgressListener
{
  private static final String CHANNEL_ID = "BackupForegroundServiceChannel";
  private static final String CHANNEL_NAME = "Backup Foreground Service";
  private static final int ONGOING_NOTIFICATION_ID = 1091;
  private static final int FINISHED_NOTIFICATION_ID = 1092;
  private static final int MAX_UPLOAD_PROGRESS = 100;
  private static final int ZIP_ENTRY_BUFFER_SIZE = 1024 * 1024; // 1 MiB

  private static boolean s_isUploading = false;
  public static boolean isUploading()
  {
    return s_isUploading;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    super.onStartCommand(intent, flags, startId);

    if (!isUploading())
    {
      s_isUploading = true;
      createNotificationChannel(CHANNEL_ID, CHANNEL_NAME);
      startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification(0));

      Thread thread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          byte[] zipBytes = createZipBackup();
          if (zipBytes != null)
          {
            uploadZipBackup(zipBytes);
          }

          stopForeground(true);
          stopSelf();
          s_isUploading = false;
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
      Log.e("BackupForegroundService", getString(R.string.backup_find_database_fail_notification_text), exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_find_database_fail_notification_text));
      return null;
    }
    catch (IOException exception)
    {
      Log.e("BackupForegroundService", getString(R.string.backup_create_file_fail_notification_text), exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_create_file_fail_notification_text));
      return null;
    }
    return outputStream.toByteArray();
  }

  private void uploadZipBackup(byte[] zipBytes)
  {
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(BackupForegroundService.this);
    if (account != null)
    {
      GoogleDriveInterface googleDriveInterface = new GoogleDriveInterface(BackupForegroundService.this, account);
      boolean uploadSuccess = googleDriveInterface.uploadFile(
          String.format("%s/%s", getString(R.string.app_name), AppDatabase.NAME.replace(".db", ".zip")),
          "application/zip",
          zipBytes,
          this);
      if (uploadSuccess)
      {
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnSuccessNotification());
      }
      else
      {
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_upload_fail_notification_text));
      }
    }
    else
    {
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.backup_recovery_sign_in_fail_notification_text));
    }
  }

  @Override
  protected Notification buildOngoingNotification(int progress)
  {
    Intent notificationIntent = new Intent(this, SettingsActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    return new NotificationCompat.Builder(this, CHANNEL_ID)
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
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.done)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentTitle(getString(R.string.backup_success_notification_title))
        .setContentText(getString(R.string.backup_success_notification_text))
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
        .setContentTitle(getString(R.string.backup_fail_notification_title))
        .setContentText(getString(failureMessageId))
        .build();
  }

  @Override
  public void progressChanged(MediaHttpUploader uploader) throws IOException
  {
    int progress = (int)(uploader.getProgress() * 100.0);
    if (progress < MAX_UPLOAD_PROGRESS)
    {
      pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(progress));
    }
  }
}
