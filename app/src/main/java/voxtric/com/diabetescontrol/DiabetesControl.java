package voxtric.com.diabetescontrol;

import android.app.Application;

import voxtric.com.diabetescontrol.database.AppDatabase;

public class DiabetesControl extends Application
{
  @Override
  public void onCreate()
  {
    super.onCreate();
    AppDatabase.initialise(this);
  }
}
