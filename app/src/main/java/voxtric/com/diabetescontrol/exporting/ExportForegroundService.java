package voxtric.com.diabetescontrol.exporting;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import voxtric.com.diabetescontrol.MainActivity;
import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.utilities.ForegroundService;

public class ExportForegroundService extends ForegroundService
{
  private static final String TAG = "ExportForegroundService";

  public static final String ACTION_ONGOING = "voxtric.com.diabetescontrol.exporting.ExportForegroundService.ACTION_ONGOING";
  public static final String ACTION_FINISHED = "voxtric.com.diabetescontrol.exporting.ExportForegroundService.ACTION_FINISHED";

  private static final String ONGOING_CHANNEL_ID = "OngoingExportForegroundServiceChannel";
  private static final String ONGOING_CHANNEL_NAME = "Ongoing Export Foreground Service";
  private static final int ONGOING_NOTIFICATION_ID = 1095;

  private static final String FINISHED_CHANNEL_ID = "FinishedExportForegroundServiceChannel";
  private static final String FINISHED_CHANNEL_NAME = "Finished Export Foreground Service";
  private static final int FINISHED_NOTIFICATION_ID = 1096;

  private static int s_progress = -1;
  public static boolean isExporting()
  {
    return s_progress != -1;
  }
  public static int getProgress()
  {
    return s_progress;
  }

  private int m_maxProgress = 0;
  private NotificationStringIds m_notificationStringIds = null;

  private String m_exportFormatName = null;
  private String m_exportFileExtension = null;
  private String m_exportFileMimeType = null;
  private File m_exportFile = null;

  @Override
  public int onStartCommand(final Intent intent, int flags, int startId)
  {
    super.onStartCommand(intent, flags, startId);
    if (!isExporting())
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
          int exportType = intent.getIntExtra("export_type", -1);
          long exportStart = intent.getLongExtra("export_start", Long.MAX_VALUE);
          long exportEnd = intent.getLongExtra("export_end", Long.MIN_VALUE);
          if (exportType != -1 && exportStart != Long.MAX_VALUE && exportEnd != Long.MIN_VALUE)
          {
            m_notificationStringIds = new NotificationStringIds(exportType);
            pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(0));

            List<DataEntry> entries = AppDatabase.getInstance().dataEntriesDao().findAllBetween(exportStart, exportEnd);
            if (!entries.isEmpty())
            {
              byte[] exportFileBytes = generateExportFile(exportType, entries);
              if (exportFileBytes != null)
              {
                String fileName = getFileName(entries);
                success = writeExportFile(fileName, exportFileBytes);
              }
              else
              {
                pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_file_generation_fail_notification_text));
              }
            }
            else
            {
              pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_time_range_fail_notification_text));
            }
          }
          else
          {
            pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_intent_details_fail_notification_text));
          }
          s_progress = -1;

          stopForeground(true);
          stopSelf();
        }
      });
      thread.setDaemon(false);
      thread.start();
    }

    return START_NOT_STICKY;
  }

  private String getFileName(List<DataEntry> entries)
  {
    Date startDate = new Date(entries.get(entries.size() - 1).dayTimeStamp);
    Date endDate = new Date(entries.get(0).dayTimeStamp);
    String startDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(startDate);
    String endDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(endDate);
    if (startDateString.equals(endDateString))
    {
      return String.format("%s (%s)%s", startDateString, m_exportFormatName, m_exportFileExtension);
    }
    else
    {
      return String.format("%s - %s (%s)%s", startDateString, endDateString, m_exportFormatName, m_exportFileExtension);
    }
  }

  private byte[] generateExportFile(@IdRes int exportType, List<DataEntry> entries)
  {
    byte[] exportFileBytes = null;
    m_maxProgress = entries.size();
    IExporter exporter = null;
    switch (exportType)
    {
    case R.id.navigation_export_nhs:
      break;
    case R.id.navigation_export_ads:
      exporter = new ADSExporter(entries);
      break;
    case R.id.navigation_export_excel:
      break;
    case R.id.navigation_export_csv:
      break;
    }
    if (exporter != null)
    {
      exportFileBytes = exporter.export(ExportForegroundService.this);
      m_exportFormatName = exporter.getFormatName();
      m_exportFileExtension = exporter.getFileExtension();
      m_exportFileMimeType = exporter.getFileMimeType();
    }
    else
    {
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_type_fail_notification_text));
    }
    return exportFileBytes;
  }

  private boolean writeExportFile(String exportFileName, byte[] exportFileBytes)
  {
    boolean success = false;
    try
    {
      File directory = new File(getFilesDir(), "exports");
      if (!directory.exists() && !directory.mkdirs())
      {
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_directory_fail_notification_text));
      }
      else
      {
        m_exportFile = new File(directory, exportFileName);
        OutputStream outputStream = new FileOutputStream(m_exportFile);
        outputStream.write(exportFileBytes);
        outputStream.close();
        success = true;
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnSuccessNotification());
      }
    }
    catch (FileNotFoundException exception)
    {
      Log.v(TAG, "Export File Not Found Exception", exception);
      pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_directory_fail_notification_text));
    }
    catch (IOException exception)
    {
      boolean handled = false;
      String exceptionMessage = exception.getMessage();
      if (exceptionMessage != null)
      {
        if (exception.getMessage().contains("No space left on device"))
        {
          Log.v(TAG, getString(R.string.storage_space_fail_notification_text), exception);
          pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.storage_space_fail_notification_text));
          handled = true;
        }
      }

      if (!handled)
      {
        Log.e(TAG, "Export IO Exception", exception);
        pushNotification(FINISHED_NOTIFICATION_ID, buildOnFailNotification(R.string.export_fail_notification_text));
      }
    }
    return success;
  }

  @Override
  protected Notification buildOngoingNotification(int progress)
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(ACTION_ONGOING);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
        .setSmallIcon(R.drawable.exporting)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setProgress(m_maxProgress, progress, progress == 0|| progress == m_maxProgress);
    if (m_notificationStringIds != null)
    {
      notificationBuilder.setContentTitle(getString(m_notificationStringIds.ongoingNotificationTitleId));
    }
    return notificationBuilder.build();
  }

  @Override
  protected Notification buildOnSuccessNotification()
  {
    Uri exportFileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", m_exportFile);
    Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
    notificationIntent.setDataAndType(exportFileUri, m_exportFileMimeType);
    int intentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION |
                      Intent.FLAG_ACTIVITY_CLEAR_TOP |
                      Intent.FLAG_ACTIVITY_SINGLE_TOP |
                      Intent.FLAG_ACTIVITY_NEW_TASK;
    notificationIntent.addFlags(intentFlags);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.done)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(m_notificationStringIds.successNotificationTitleId))
        .setContentText(getString(m_notificationStringIds.successNotificationTitleId))
        .build();
  }

  @Override
  protected Notification buildOnFailNotification(int failureMessageId)
  {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    notificationIntent.setAction(ACTION_FINISHED);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    return new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
        .setSmallIcon(R.drawable.error)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentTitle(getString(m_notificationStringIds.failNotificationTitleId))
        .setContentText(getString(failureMessageId))
        .build();
  }

  public void incrementProgress(int progressIncrement)
  {
    s_progress += progressIncrement;
    pushNotification(ONGOING_NOTIFICATION_ID, buildOngoingNotification(s_progress));

    Intent exportOngoingBroadcast = new Intent(ACTION_ONGOING);
    LocalBroadcastManager.getInstance(this).sendBroadcast(exportOngoingBroadcast);
  }

  private static class NotificationStringIds
  {
    @StringRes
    final int ongoingNotificationTitleId;
    final int successNotificationTitleId;
    final int successNotificationTextId;
    final int failNotificationTitleId;

    NotificationStringIds(@IdRes int exportType)
    {
      switch (exportType)
      {
      case R.id.navigation_export_nhs:
        ongoingNotificationTitleId = R.string.exporting_nhs_notification_title;
        successNotificationTitleId = R.string.export_nhs_success_notification_title;
        successNotificationTextId = R.string.export_nhs_success_notification_text;
        failNotificationTitleId = R.string.export_nhs_fail_notification_title;
        break;
      case R.id.navigation_export_ads:
        ongoingNotificationTitleId = R.string.exporting_ads_notification_title;
        successNotificationTitleId = R.string.export_ads_success_notification_title;
        successNotificationTextId = R.string.export_ads_success_notification_text;
        failNotificationTitleId = R.string.export_ads_fail_notification_title;
        break;
      case R.id.navigation_export_excel:
        ongoingNotificationTitleId = R.string.exporting_excel_notification_title;
        successNotificationTitleId = R.string.export_excel_success_notification_title;
        successNotificationTextId = R.string.export_excel_success_notification_text;
        failNotificationTitleId = R.string.export_excel_fail_notification_title;
        break;
      case R.id.navigation_export_csv:
        ongoingNotificationTitleId = R.string.exporting_csv_notification_title;
        successNotificationTitleId = R.string.export_csv_success_notification_title;
        successNotificationTextId = R.string.export_csv_success_notification_text;
        failNotificationTitleId = R.string.export_csv_fail_notification_title;
        break;
      default:
        ongoingNotificationTitleId = R.string.title_undefined;
        successNotificationTitleId = R.string.title_undefined;
        successNotificationTextId = R.string.message_undefined;
        failNotificationTitleId = R.string.title_undefined;
      }
    }
  }
}
