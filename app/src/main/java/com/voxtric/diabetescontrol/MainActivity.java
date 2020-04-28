package com.voxtric.diabetescontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Food;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.exporting.ExportDurationDialogFragment;
import com.voxtric.diabetescontrol.exporting.ExportForegroundService;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.GoogleDriveInterface;
import com.voxtric.diabetescontrol.utilities.ViewUtilities;

public class MainActivity extends AwaitRecoveryActivity
{
  private static final String TAG = "MainActivity";

  private static final int REQUEST_EDIT_SETTINGS = 100;
  public static final int RESULT_UPDATE_EVENT_SPINNER = 0x01;
  public static final int RESULT_UPDATE_BGL_HIGHLIGHTING = 0x02;
  public static final int RESULT_UPDATE_GRAPH_DATA = 0x04;

  private static final int REQUEST_CHOOSE_DIRECTORY = 101;

  private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 112;

  private ViewPager m_viewPager = null;

  private PopupMenu m_activeMenu = null;

  private AlertDialog m_exportProgressDialog = null;
  private ExportOngoingBroadcastReceiver m_exportOngoingBroadcastReceiver = new ExportOngoingBroadcastReceiver();
  private ExportFinishedBroadcastReceiver m_exportFinishedBroadcastReceiver = new ExportFinishedBroadcastReceiver();

  private AlertDialog m_backupProgressDialog = null;
  private BackupOngoingBroadcastReceiver m_backupOngoingBroadcastReceiver = new BackupOngoingBroadcastReceiver();
  private BackupFinishedBroadcastReceiver m_backupFinishedBroadcastReceiver = new BackupFinishedBroadcastReceiver();

  private String m_moveExportFilePath = null;
  private String m_moveExportFileMimeType = null;

  private BottomNavigationView.OnNavigationItemSelectedListener m_onNavigationItemSelectedListener
      = new BottomNavigationView.OnNavigationItemSelectedListener()
  {
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
      switch (item.getItemId())
      {
        case R.id.navigation_add_new:
          m_viewPager.setCurrentItem(getFragmentIndex(NewEntryFragment.class));
          return true;
        case R.id.navigation_view_all:
          m_viewPager.setCurrentItem(getFragmentIndex(EntryListFragment.class));
          return true;
        case R.id.navigation_view_graph:
          m_viewPager.setCurrentItem(getFragmentIndex(EntryGraphFragment.class));
          return false;
      }
      return false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    PDFBoxResourceLoader.init(this);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    BottomNavigationView navigation = findViewById(R.id.navigation);
    navigation.setOnNavigationItemSelectedListener(m_onNavigationItemSelectedListener);

    m_viewPager = findViewById(R.id.fragment_container);
    initialiseViewPager(m_viewPager, navigation);

    // Ensures act on intent happens after the relevant fragments have been created.
    m_viewPager.post(new Runnable()
    {
      @Override
      public void run()
      {
        actOnIntent(getIntent());
      }
    });
  }

  @Override
  public void onResume()
  {
    super.onResume();

    final SharedPreferences preferences = getSharedPreferences("preferences", MODE_PRIVATE);
    boolean firstTimeLaunch = preferences.getBoolean("first_time_launch", true);
    if (firstTimeLaunch)
    {
      AlertDialog dialog = new AlertDialog.Builder(this)
          .setTitle(R.string.show_showcases_dialog_title)
          .setMessage(R.string.show_showcases_dialog_text)
          .setCancelable(false)
          .setPositiveButton(R.string.yes_dialog_option, new DialogInterface.OnClickListener()
          {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
              SharedPreferences.Editor preferenceEditor = preferences.edit();
              preferenceEditor.putBoolean("first_time_launch", false);
              preferenceEditor.putBoolean("show_showcases", true);
              preferenceEditor.commit();
              ShowcaseViewHandler.handleMainActivityShowcaseViews(MainActivity.this);
            }
          })
          .setNegativeButton(R.string.no_dialog_option, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
              preferences.edit().putBoolean("first_time_launch", false).apply();
            }
          })
          .show();
      dialog.setCanceledOnTouchOutside(false);
    }

    if (!RecoveryForegroundService.isDownloading())
    {
      Preference.get(this,
                     new String[]{
                         "automatic_backup", "wifi_only_backup", "last_successful_backup_timestamp"
                     },
                     new String[]{
                         getString(R.string.automatic_backup_never_option), String.valueOf(true), String.valueOf(Long.MIN_VALUE)
                     },
                     new Preference.ResultRunnable()
                     {
                       @Override
                       public void run()
                       {
                         String automaticBackup = getResults().get("automatic_backup");
                         String wifiOnlyBackup = getResults().get("wifi_only_backup");
                         String lastSuccessfulBackupTimestamp = getResults().get("last_successful_backup_timestamp");
                         if (automaticBackup != null && wifiOnlyBackup != null && lastSuccessfulBackupTimestamp != null)
                         {
                           Calendar calendar = Calendar.getInstance();
                           if (automaticBackup.equals(getString(R.string.automatic_backup_daily_option)))
                           {
                             calendar.add(Calendar.DAY_OF_YEAR, -1);
                           }
                           else if (automaticBackup.equals(getString(R.string.automatic_backup_weekly_option)))
                           {
                             calendar.add(Calendar.WEEK_OF_YEAR, -1);
                           }
                           else if (automaticBackup.equals(getString(R.string.automatic_backup_monthly_option)))
                           {
                             calendar.add(Calendar.MONTH, -1);
                           }
                           else
                           {
                             calendar.set(calendar.getMaximum(Calendar.YEAR),
                                          calendar.getMaximum(Calendar.MONTH),
                                          calendar.getMaximum(Calendar.DATE));
                           }

                           if (calendar.getTimeInMillis() >= Long.parseLong(lastSuccessfulBackupTimestamp) && (!Boolean.parseBoolean(
                               wifiOnlyBackup) || GoogleDriveInterface.hasWifiConnection(MainActivity.this)) && GoogleSignIn
                               .getLastSignedInAccount(MainActivity.this) != null && !BackupForegroundService.isUploading() && !RecoveryForegroundService
                               .isDownloading())
                           {
                             Intent intent = new Intent(MainActivity.this, BackupForegroundService.class);
                             startService(intent);
                           }
                         }
                       }
                     });
    }
  }

  @Override
  public void onStart()
  {
    super.onStart();
    ShowcaseViewHandler.handleMainActivityShowcaseViews(MainActivity.this);
  }

  @Override
  public void onPause()
  {
    if (m_backupProgressDialog != null)
    {
      cancelBackupProgressDialog();
    }
    if (m_exportProgressDialog != null)
    {
      cancelExportProgressDialog();
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
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    MenuCompat.setGroupDividerEnabled(menu, true);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull final MenuItem menuItem)
  {
    Intent intent;
    @MenuRes final int menuItemId = menuItem.getItemId();
    switch (menuItemId)
    {
    case R.id.navigation_export_nhs:
    case R.id.navigation_export_ads:
    case R.id.navigation_export_csv:
      if (RecoveryForegroundService.isDownloading())
      {
        Toast.makeText(this, R.string.export_recovery_in_progress_message, Toast.LENGTH_LONG).show();
      }
      else if (ExportForegroundService.isExporting())
      {
        Toast.makeText(this, R.string.export_already_in_progress_message, Toast.LENGTH_LONG).show();
      }
      else if ((menuItemId == R.id.navigation_export_nhs) && !getPreferences(MODE_PRIVATE).getBoolean("nhs_warn_dont_show_again", false))
      {
        new AlertDialog.Builder(this)
            .setTitle(R.string.nhs_export_warn_title)
            .setView(R.layout.dialog_nhs_warn)
            .setNegativeButton(R.string.nhs_export_warn_negative_button, null)
            .setPositiveButton(R.string.nhs_export_want_positive_button, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialogInterface, int i)
              {
                performExport(menuItemId);
              }
            })
            .show();
      }
      else
      {
        performExport(menuItemId);
      }
    return true;

    case R.id.navigation_about:
      intent = new Intent(this, AboutActivity.class);
      startActivity(intent);
      return true;
    case R.id.navigation_settings:
      intent = new Intent(this, SettingsActivity.class);
      startActivityForResult(intent, REQUEST_EDIT_SETTINGS);
      return true;

    default:
      return false;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    switch (requestCode)
    {
    case REQUEST_EDIT_SETTINGS:
      if ((resultCode & RESULT_UPDATE_EVENT_SPINNER) == RESULT_UPDATE_EVENT_SPINNER)
      {
        getFragment(NewEntryFragment.class).updateEventSpinner();
      }
      if ((resultCode & RESULT_UPDATE_BGL_HIGHLIGHTING) == RESULT_UPDATE_BGL_HIGHLIGHTING)
      {
        getFragment(EntryListFragment.class).refreshEntryList();
        getFragment(EntryGraphFragment.class).updateRangeHighlighting();
      }
      if ((resultCode & RESULT_UPDATE_GRAPH_DATA) == RESULT_UPDATE_GRAPH_DATA)
      {
        getFragment(EntryGraphFragment.class).refreshGraph(false, true);
      }
      break;
    case REQUEST_CHOOSE_DIRECTORY:
      if (data != null)
      {
        Uri directoryUri = data.getData();
        if (resultCode == RESULT_OK && directoryUri != null)
        {
          moveExportFile(directoryUri);
        }
      }
      break;
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
    {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
          beginMoveExportFile(m_moveExportFilePath, m_moveExportFileMimeType);
        }
        else
        {
          Toast.makeText(this, R.string.action_not_supported_message, Toast.LENGTH_LONG).show();
        }
      }
      else
      {
        Toast.makeText(this, R.string.write_external_storage_move_export_permission_needed_message, Toast.LENGTH_LONG).show();
      }
    }
    else
    {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  public <E extends Fragment> int getFragmentIndex(Class<E> classType)
  {
    List<Fragment> fragments = getSupportFragmentManager().getFragments();
    for (int i = 0; i < fragments.size(); i++)
    {
      if (classType.isInstance(fragments.get(i)))
      {
        return i;
      }
    }
    return -1;
  }

  public <E extends Fragment> E getFragment(Class<E> classType)
  {
    List<Fragment> fragments = getSupportFragmentManager().getFragments();
    for (Fragment fragment : fragments)
    {
      if (classType.isInstance(fragment))
      {
        //noinspection unchecked
        return (E)fragment;
      }
    }
    return null;
  }

  private void performExport(int exportOptionItemId)
  {
    final Intent intent = new Intent(this, ExportForegroundService.class);
    intent.putExtra("export_type", exportOptionItemId);

    @SuppressLint("DefaultLocale") final String lastExportTimestampPreference = String.format("last_%d_export_timestamp", exportOptionItemId);
    Preference.get(this, lastExportTimestampPreference, "-1", new Preference.ResultRunnable()
    {
      @Override
      public void run()
      {
        ExportDurationDialogFragment dialog = new ExportDurationDialogFragment(intent, Long.parseLong(getResult()));
        dialog.showNow(getSupportFragmentManager(), ExportDurationDialogFragment.TAG);
        dialog.initialiseExportButton();
      }
    });
  }

  public void launchExportProgressDialog(String messageTitle)
  {
    if (m_exportProgressDialog != null)
    {
      cancelExportProgressDialog();
    }

    m_exportProgressDialog = new AlertDialog.Builder(this)
        .setTitle(messageTitle)
        .setView(R.layout.dialog_service_ongoing)
        .setPositiveButton(R.string.ok_dialog_option, null)
        .create();
    m_exportProgressDialog.show();
    TextView message = m_exportProgressDialog.findViewById(R.id.message);
    if (message != null)
    {
      message.setText(R.string.message_export_in_progress);
    }
    updateExportDialogProgress();

    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_exportOngoingBroadcastReceiver,
        new IntentFilter(ExportForegroundService.ACTION_ONGOING));
    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_exportFinishedBroadcastReceiver,
        new IntentFilter(ExportForegroundService.ACTION_FINISHED));
  }

  private void cancelExportProgressDialog()
  {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_exportOngoingBroadcastReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_exportFinishedBroadcastReceiver);
    m_exportProgressDialog.cancel();
    m_exportProgressDialog = null;
  }

  private void updateExportDialogProgress()
  {
    if (m_exportProgressDialog != null && ExportForegroundService.isExporting())
    {
      ProgressBar progressBar = m_exportProgressDialog.findViewById(R.id.progress);
      if (progressBar != null)
      {
        int progress = ExportForegroundService.getProgress();
        progressBar.setIndeterminate(progress == 0);
        progressBar.setMax(ExportForegroundService.getMaxProgress());
        progressBar.setProgress(progress);
      }
    }
  }

  private void launchBackupProgressDialog()
  {
    if (m_backupProgressDialog != null)
    {
      cancelBackupProgressDialog();
    }

    m_backupProgressDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_backup_in_progress)
        .setView(R.layout.dialog_service_ongoing)
        .setPositiveButton(R.string.ok_dialog_option, null)
        .create();
    m_backupProgressDialog.show();
    TextView message = m_backupProgressDialog.findViewById(R.id.message);
    if (message != null)
    {
      message.setText(R.string.message_backup_in_progress);
    }
    updateBackupDialogProgress();

    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_backupOngoingBroadcastReceiver,
        new IntentFilter(BackupForegroundService.ACTION_ONGOING));
    LocalBroadcastManager.getInstance(this).registerReceiver(
        m_backupFinishedBroadcastReceiver,
        new IntentFilter(BackupForegroundService.ACTION_FINISHED));
  }

  private void cancelBackupProgressDialog()
  {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_backupOngoingBroadcastReceiver);
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_backupFinishedBroadcastReceiver);
    m_backupProgressDialog.cancel();
    m_backupProgressDialog = null;
  }

  private void updateBackupDialogProgress()
  {
    if (m_backupProgressDialog != null && BackupForegroundService.isUploading())
    {
      ProgressBar progressBar = m_backupProgressDialog.findViewById(R.id.progress);
      if (progressBar != null)
      {
        int progress = BackupForegroundService.getProgress();
        progressBar.setIndeterminate(progress == 0);
        progressBar.setProgress(progress);
      }
    }
  }

  public void updateNHSWarnDontShowAgain(final View view)
  {
    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
    preferences.edit().putBoolean("nhs_warn_dont_show_again", ((CheckBox)view).isChecked()).apply();
  }

  void navigateToPageFragment(int fragmentIndex)
  {
    BottomNavigationView navigation = findViewById(R.id.navigation);
    navigation.getMenu().getItem(fragmentIndex).setChecked(true);
    m_viewPager.setCurrentItem(fragmentIndex);
  }

  private void actOnIntent(final Intent intent)
  {
    String action = intent.getAction();
    if (action != null)
    {
      switch (action)
      {
      case Intent.ACTION_OPEN_DOCUMENT_TREE:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        String exportFilePath = intent.getStringExtra("export_file_path");
        String exportFileMimeType = intent.getStringExtra("export_file_mime_type");
        if (exportFilePath != null && exportFileMimeType != null &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
          beginMoveExportFile(exportFilePath, exportFileMimeType);
        }
        break;

      case ExportForegroundService.ACTION_ONGOING:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        String messageTitle = intent.getStringExtra("message_title");
        if (messageTitle == null)
        {
          messageTitle = getString(R.string.title_undefined);
        }
        launchExportProgressDialog(messageTitle);
        break;
      case ExportForegroundService.ACTION_FINISHED:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        if (!intent.getBooleanExtra("success", false))
        {
          launchMessageDialog(intent);
        }
        break;

      case Intent.ACTION_SEND:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        shareExportFile(intent.getStringExtra("export_file_path"), intent.getStringExtra("export_file_mime_type"));
        break;

      case BackupForegroundService.ACTION_ONGOING:
        launchBackupProgressDialog();
      case RecoveryForegroundService.ACTION_ONGOING:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        break;
      case ExportForegroundService.ACTION_VIEW_EXPORT_FAIL:
      case BackupForegroundService.ACTION_FINISHED:
      case RecoveryForegroundService.ACTION_FINISHED:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        launchMessageDialog(intent);
        break;
      }
      intent.setAction(null);
    }
  }

  private void launchMessageDialog(final Intent intent)
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

  private void moveExportFile(Uri directoryUri)
  {
    DocumentFile directory = DocumentFile.fromTreeUri(this, directoryUri);
    if (directory != null)
    {
      String fileName = new File(m_moveExportFilePath).getName();
      DocumentFile exportFile = directory.createFile(m_moveExportFileMimeType, fileName);
      if (exportFile != null)
      {
        Uri exportFileUri = exportFile.getUri();
        try
        {
          InputStream inputStream = new FileInputStream(new File(m_moveExportFilePath));
          OutputStream outputStream = getContentResolver().openOutputStream(exportFileUri);
          if (outputStream != null)
          {
            int exportFileData = inputStream.read();
            while (exportFileData != -1)
            {
              outputStream.write(exportFileData);
              exportFileData = inputStream.read();
            }
            viewExportFile(exportFileUri, m_moveExportFileMimeType, false);
          }
          else
          {
            Log.e(TAG, String.format("Couldn't create output stream for export file: '%s': '%s': '%s'", directory.getName(), exportFile.getName(), exportFileUri.toString()));
            Toast.makeText(this, R.string.export_move_output_stream_not_created_message, Toast.LENGTH_LONG).show();
          }
        }
        catch (FileNotFoundException exception)
        {
          Log.e(TAG, String.format("Move Export File Not Found Exception: '%s': '%s': '%s'", directory.getName(), exportFile.getName(), exportFileUri.toString()));
          Toast.makeText(this, R.string.export_move_no_file_found_message, Toast.LENGTH_LONG).show();
        }
        catch (IOException exception)
        {
          boolean handled = false;
          String exceptionMessage = exception.getMessage();
          if (exceptionMessage != null)
          {
            if (exception.getMessage().contains("No space left on device"))
            {
              Log.v(TAG, "Move Export: " + getString(R.string.storage_space_fail_notification_text), exception);
              Toast.makeText(this, R.string.storage_space_fail_notification_text, Toast.LENGTH_LONG).show();
              handled = true;
            }
          }

          if (!handled)
          {
            Log.e(TAG, String.format("Move Export IO Exception: '%s': '%s': '%s'", directory.getName(), fileName, exportFileUri.toString()));
            Toast.makeText(this, R.string.export_move_write_fail_message, Toast.LENGTH_LONG).show();
          }
        }
      }
      else
      {
        Log.e(TAG, String.format("Couldn't create export file: '%s': '%s'", directory.getName(), fileName));
        Toast.makeText(this, R.string.export_move_create_fail_message, Toast.LENGTH_LONG).show();
      }
    }
    else
    {
      Log.e(TAG, "Invalid directory chosen to move export file to.");
      Toast.makeText(this, R.string.export_move_invalid_directory_message, Toast.LENGTH_LONG).show();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void beginMoveExportFile(String exportFilePath, String exportFileMimeType)
  {
    m_moveExportFilePath = exportFilePath;
    m_moveExportFileMimeType = exportFileMimeType;

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
    {
      int hasWriteExternalStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
      {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
          AlertDialog dialog = new AlertDialog.Builder(this)
              .setTitle(R.string.permission_justification_title)
              .setMessage(R.string.write_external_storage_move_export_permission_justification_message)
              .setNegativeButton(R.string.deny_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  Toast.makeText(MainActivity.this, R.string.write_external_storage_move_export_permission_needed_message, Toast.LENGTH_LONG).show();
                }
              })
              .setPositiveButton(R.string.allow_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
                }
              })
              .create();
          dialog.show();
        }
        else
        {
          requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
        return;
      }
    }

    Intent moveFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    startActivityForResult(moveFileIntent, REQUEST_CHOOSE_DIRECTORY);
  }

  private void initialiseViewPager(ViewPager viewPager, final BottomNavigationView navigation)
  {
    FragmentPagerAdapter fragmentStatePagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager())
    {
      @Override
      public Fragment getItem(int i)
      {
        switch (i)
        {
        case 0:
          return new NewEntryFragment();
        case 1:
          return new EntryListFragment();
        case 2:
          return new EntryGraphFragment();
        default:
          throw new InvalidParameterException(String.format("%d is an invalid fragment index for MainActivity view pager.", i));
        }
      }

      @Override
      public int getCount()
      {
        return 3;
      }
    };
    viewPager.setAdapter(fragmentStatePagerAdapter);
    viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
    {
      @Override
      public void onPageScrolled(int i, float v, int i1) {}

      @Override
      public void onPageScrollStateChanged(int i) {}

      @Override
      public void onPageSelected(int pageIndex)
      {
        if (m_activeMenu != null)
        {
          m_activeMenu.dismiss();
        }

        navigation.getMenu().getItem(pageIndex).setChecked(true);
        forceKeyboardClosed();

        switch (pageIndex)
        {
        case 0:
          ShowcaseViewHandler.handleAddNewEntryFragmentShowcaseViews(MainActivity.this);
          break;
        case 1:
          ShowcaseViewHandler.handleEntryListFragmentShowcaseViews(MainActivity.this);
          break;
        case 2:
          ShowcaseViewHandler.handleEntryGraphFragmentShowcaseViews(MainActivity.this);
          break;
        default:
          throw new InvalidParameterException(String.format("%d is an invalid fragment index for MainActivity view pager.", pageIndex));
        }
      }
    });
    viewPager.setOffscreenPageLimit(2);
  }

  public void forceKeyboardClosed()
  {
    View view = findViewById(R.id.fragment_container);
    view.requestFocus();
    InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    if (inputMethodManager != null)
    {
      inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
  }

  public void openEntryMoreMenu(@NonNull View view)
  {
    final ViewGroup dataView = view instanceof LinearLayout ? (ViewGroup)view : (ViewGroup)view.getParent();
    final ViewGroup listView = (ViewGroup)dataView.getParent();
    for (int i = 0; i < listView.getChildCount(); i++)
    {
      if (listView.getChildAt(i) != dataView)
      {
        ViewGroup layout = (LinearLayout)listView.getChildAt(i);
        for (int j = 0; j < layout.getChildCount(); j++)
        {
          layout.getChildAt(j).setBackgroundResource(R.drawable.blank);
        }
      }
    }
    m_activeMenu = new PopupMenu(view.getContext(), view);
    m_activeMenu.getMenuInflater().inflate(R.menu.entry_more, m_activeMenu.getMenu());
    m_activeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
    {
      @Override
      public boolean onMenuItemClick(MenuItem item)
      {
        switch (item.getItemId())
        {
          case R.id.navigation_view_full:
            getFragment(EntryListFragment.class).viewFull(MainActivity.this, dataView);
            return true;
          case R.id.navigation_edit:
            getFragment(EntryListFragment.class).launchEdit(MainActivity.this, dataView);
            return true;
          case R.id.navigation_delete:
            getFragment(EntryListFragment.class).deleteEntry(MainActivity.this, dataView);
            return true;
          case R.id.navigation_cancel:
            return true;
        }
        return false;
      }
    });
    m_activeMenu.setOnDismissListener(new PopupMenu.OnDismissListener()
    {
      @Override
      public void onDismiss(PopupMenu menu)
      {
        m_activeMenu = null;
        for (int i = 0; i < listView.getChildCount(); i++)
        {
          if (listView.getChildAt(i) != dataView)
          {
            ViewGroup layout = (LinearLayout)listView.getChildAt(i);
            for (int j = 0; j < layout.getChildCount(); j++)
            {
              layout.getChildAt(j).setBackgroundResource(R.drawable.back);
            }
          }
        }
      }
    });
    m_activeMenu.show();
  }

  public static View getFullView(final Activity activity, final DataEntry entry)
  {
    final View view = View.inflate(activity, R.layout.dialog_view_full_entry, null);

    Date date = new Date(entry.actualTimestamp);
    String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
    String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

    ((TextView)view.findViewById(R.id.date_value)).setText(dateString);
    ((TextView)view.findViewById(R.id.time_value)).setText(timeString);
    ((TextView)view.findViewById(R.id.event_value)).setText(entry.event);
    ((TextView)view.findViewById(R.id.insulin_name_value)).setText(entry.insulinDose > 0 ? entry.insulinName : activity.getString(R.string.not_applicable));
    ((TextView)view.findViewById(R.id.insulin_dose_value)).setText(entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : activity.getString(R.string.not_applicable));
    ((TextView)view.findViewById(R.id.blood_glucose_level_value)).setText(String.valueOf(entry.bloodGlucoseLevel));

    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final List<Food> foodList = AppDatabase.getInstance().foodsDao().getFoods(entry.actualTimestamp);
        activity.runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            TextView foodEatenValue = view.findViewById(R.id.food_eaten_value);
            if (foodList.size() == 0)
            {
              foodEatenValue.setVisibility(View.GONE);
              view.findViewById(R.id.food_eaten_label).setVisibility(View.GONE);
            }
            else
            {
              StringBuilder foodText = new StringBuilder(foodList.get(0).name);
              for (int i = 1; i < foodList.size(); i++)
              {
                foodText.append("\n");
                foodText.append(foodList.get(i).name);
              }
              foodEatenValue.setText(foodText);
              foodEatenValue.setGravity(Gravity.TOP | Gravity.START);
            }
          }
        });
      }
    });

    TextView additionalNotesValue = view.findViewById(R.id.additional_notes_value);
    if (entry.additionalNotes.length() == 0)
    {
      view.findViewById(R.id.additional_notes_label).setVisibility(View.GONE);
      additionalNotesValue.setVisibility(View.GONE);
    }
    else
    {
      additionalNotesValue.setText(entry.additionalNotes);
      additionalNotesValue.setGravity(Gravity.TOP | Gravity.START);
    }

    return view;
  }

  public void toggleStatisticsVisibility(View view)
  {
    getFragment(EntryGraphFragment.class).toggleStatisticsVisibility(this);
  }

  public void viewExportFile(Uri exportFileUri, String exportFileMimeType, boolean showViewExportFail)
  {
    Intent viewFileIntent = ExportForegroundService.buildViewFileIntent(this, exportFileUri, exportFileMimeType, showViewExportFail);
    startActivity(viewFileIntent);
  }

  public void shareExportFile(String exportFilePath, String exportFileMimeType)
  {
    File exportFile = new File(exportFilePath);
    Uri exportFileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", exportFile);

    Intent shareFileIntent = new Intent(Intent.ACTION_SEND);
    shareFileIntent.setType(exportFileMimeType);
    shareFileIntent.putExtra(Intent.EXTRA_STREAM, exportFileUri);
    shareFileIntent.putExtra(Intent.EXTRA_SUBJECT, exportFile.getName());
    shareFileIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, getString(R.string.app_name)));
    shareFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    Intent createChooserIntent = Intent.createChooser(shareFileIntent, getString(R.string.share_title, exportFile.getName()));
    createChooserIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(createChooserIntent);
  }

  private class ExportOngoingBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      updateExportDialogProgress();
    }
  }

  private class ExportFinishedBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, final Intent intent)
    {
      cancelExportProgressDialog();
      if (!intent.getBooleanExtra("success", false))
      {
        launchMessageDialog(intent);
      }
      else
      {
        final String exportFilePath = intent.getStringExtra("export_file_path");
        final String exportFileMimeType = intent.getStringExtra("export_file_mime_type");
        if (exportFilePath == null)
        {
          Log.e(TAG, "Export finished intent missing: 'export_file_path'");
        }
        else if (exportFileMimeType == null)
        {
          Log.e(TAG, "Export finished intent missing: 'export_file_mime_type'");
        }
        else
        {
          final File exportFile = new File(exportFilePath);
          final Uri exportFileUri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", exportFile);

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

          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this)
              .setTitle(messageTitle)
              .setMessage(messageText)
              .setPositiveButton(R.string.view_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  viewExportFile(exportFileUri, exportFileMimeType, true);
                }
              }).setNegativeButton(R.string.share_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  shareExportFile(exportFile.getAbsolutePath(), exportFileMimeType);
                }
              });

          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
          {
            dialogBuilder.setNeutralButton(R.string.move_dialog_option, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialogInterface, int i)
              {
                beginMoveExportFile(exportFilePath, exportFileMimeType);
              }
            });
          }

          dialogBuilder.show();
        }
      }
    }
  }

  private class BackupOngoingBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      updateBackupDialogProgress();
    }
  }

  private class BackupFinishedBroadcastReceiver extends BroadcastReceiver
  {
    @Override
    public void onReceive(Context context, Intent intent)
    {
      cancelBackupProgressDialog();
      launchMessageDialog(intent);
    }
  }
}
