package com.voxtric.diabetescontrol.settings;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.voxtric.diabetescontrol.AwaitRecoveryActivity;
import com.voxtric.diabetescontrol.BackupForegroundService;
import com.voxtric.diabetescontrol.MainActivity;
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
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    actOnIntent(getIntent());
  }

  @Override
  protected void onStart()
  {
    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
    boolean hasRecoveryMessage = preferences.getBoolean("has_recovery_message", false);

    super.onStart();

    if (hasRecoveryMessage)
    {
      applyResultFlag(MainActivity.RESULT_UPDATE_GRAPH_DATA);
    }
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
    super.onNewIntent(intent);
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

  public void moreInfo(final View view)
  {
    @StringRes int titleResource;
    @StringRes int textResource;

    switch (view.getId())
    {
    default:
      titleResource = 0;
      textResource = 0;
      break;
    case R.id.events_more_info_button:
      titleResource = R.string.events_settings_help_title;
      textResource = R.string.events_settings_help_text;
      break;
    case R.id.contact_details_more_info_button:
      titleResource = R.string.contact_details_settings_help_title;
      textResource = R.string.contact_details_settings_help_text;
      break;
    case R.id.bgl_highlighting_more_info_button:
      titleResource = R.string.bgl_highlighting_settings_help_title;
      textResource = R.string.bgl_highlighting_settings_help_text;
      break;
    case R.id.meal_targets_more_info_button:
      titleResource = R.string.meal_targets_settings_help_title;
      textResource = R.string.meal_targets_settings_help_text;
      break;
    case R.id.backup_more_info_button:
      titleResource = R.string.backup_settings_help_title;
      textResource = R.string.backup_settings_help_text;
      break;
    }

    if (titleResource != 0)
    {
      AlertDialog dialog = new AlertDialog.Builder(this).setTitle(titleResource)
                                                        .setView(R.layout.dialog_settings_help)
                                                        .setPositiveButton(R.string.ok_dialog_option, null)
                                                        .show();
      ((TextView)dialog.findViewById(R.id.settings_help_text)).setText(textResource);
    }
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
