package voxtric.com.diabetescontrol.settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import voxtric.com.diabetescontrol.AwaitRecoveryActivity;
import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.utilities.CompositeOnFocusChangeListener;

public class SettingsActivity extends AwaitRecoveryActivity
{
  private int m_result = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
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

  @Override
  public void onPause()
  {
    View view = getCurrentFocus();
    if (view != null)
    {
      view.clearFocus();
    }
    super.onPause();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    View view = findViewById(R.id.root);
    if (view != null)
    {
      view.requestFocus();
      InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      if (inputMethodManager != null)
      {
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
      }
    }
  }

  public void setTextFromDatabase(final EditText view)
  {
    String viewName = view.getResources().getResourceName(view.getId());
    Preference.get(this, viewName, "", new Preference.ResultRunnable()
    {
      @Override
      public void run()
      {
        view.setText(getResult());
      }
    });
  }

  public void saveTextToDatabaseWhenUnfocused(final EditText view)
  {
    final String viewName = view.getResources().getResourceName(view.getId());
    CompositeOnFocusChangeListener.applyListenerToView(view, new View.OnFocusChangeListener()
    {
      @Override
      public void onFocusChange(View v, boolean hasFocus)
      {
        if (!hasFocus)
        {
          Preference.put(SettingsActivity.this, viewName, view.getText().toString(), null);
        }
      }
    });
  }

  public void applyResultFlag(int resultFlag)
  {
    m_result |= resultFlag;
    setResult(m_result);
  }
}
