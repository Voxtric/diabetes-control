package com.voxtric.diabetescontrol;

import androidx.multidex.MultiDexApplication;

import com.voxtric.diabetescontrol.database.AppDatabase;

public class DiabetesControl extends MultiDexApplication
{
  @Override
  public void onCreate()
  {
    super.onCreate();
    if (!RecoveryForegroundService.isDownloading())
    {
      AppDatabase.initialise(this);
    }
  }
}
