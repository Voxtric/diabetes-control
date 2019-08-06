package com.voxtric.diabetescontrol.settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.voxtric.diabetescontrol.AwaitRecoveryActivity;
import com.voxtric.diabetescontrol.BackupForegroundService;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.RecoveryForegroundService;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import com.voxtric.diabetescontrol.utilities.ViewUtilities;

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

    actOnIntent(getIntent());
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
  public void onNewIntent(Intent intent)
  {
    actOnIntent(intent);
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

  private void actOnIntent(final Intent intent)
  {
    String action = intent.getAction();
    if (action != null)
    {
      if (action.equals(BackupForegroundService.ACTION_FINISHED) ||
          action.equals(RecoveryForegroundService.ACTION_FINISHED))
      {
        String messageTitle = intent.getStringExtra("message_title");
        if (messageTitle == null)
        {
          messageTitle = getString(R.string.title_undefined);
        }

        String messageText = intent.getStringExtra("message_text");
        if (messageText == null)
        {
          messageText = getString(R.string.message_undefined);
        }

        ViewUtilities.launchMessageDialog(this,
                                          messageTitle,
                                          messageText,
                                          new DialogInterface.OnClickListener()
                                          {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i)
                                            {
                                              NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                                              if (notificationManager != null)
                                              {
                                                notificationManager.cancel(intent.getIntExtra("notification_id", -1));
                                              }
                                            }
                                          });
      }
      intent.setAction(null);
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
