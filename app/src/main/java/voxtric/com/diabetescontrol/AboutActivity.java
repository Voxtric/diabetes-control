package voxtric.com.diabetescontrol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import voxtric.com.diabetescontrol.database.AppDatabase;

public class AboutActivity extends AppCompatActivity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    ((TextView)findViewById(R.id.app_version_text)).setText(getString(R.string.app_version_text, BuildConfig.VERSION_NAME));
    ((TextView)findViewById(R.id.database_version_text)).setText(getString(R.string.database_version_text, AppDatabase.Version));
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      finish();
      return true;
    }
    return false;
  }
}
