package voxtric.com.diabetescontrol;

import androidx.multidex.MultiDexApplication;

import voxtric.com.diabetescontrol.database.AppDatabase;

public class DiabetesControl extends MultiDexApplication
{
  @Override
  public void onCreate()
  {
    super.onCreate();
    AppDatabase.initialise(this);
  }
}
