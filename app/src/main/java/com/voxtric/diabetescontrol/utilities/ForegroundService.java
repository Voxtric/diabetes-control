package com.voxtric.diabetescontrol.utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.StringRes;

@SuppressWarnings("unused")
public abstract class ForegroundService extends Service
{
  private NotificationManager m_notificationManager = null;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId)
  {
    m_notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  protected void pushNotification(int notificationId, Notification notification)
  {
    m_notificationManager.notify(notificationId, notification);
  }

  protected void cancelNotification(int notificationId)
  {
    m_notificationManager.cancel(notificationId);
  }

  protected void createNotificationChannel(String channelId, String channelName, boolean ongoingNotification)
  {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
    {
      NotificationChannel serviceChannel = new NotificationChannel(
          channelId,
          channelName,
          ongoingNotification ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_DEFAULT);
      m_notificationManager.createNotificationChannel(serviceChannel);
    }
  }

  protected abstract Notification buildOngoingNotification(int progress);
  protected abstract Notification buildOnSuccessNotification();
  protected abstract Notification buildOnFailNotification(@StringRes int failureMessageId);
}
