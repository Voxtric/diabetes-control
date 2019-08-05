package voxtric.com.diabetescontrol;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
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
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.Food;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.exporting.ExportDurationDialogFragment;
import voxtric.com.diabetescontrol.exporting.ExportForegroundService;
import voxtric.com.diabetescontrol.settings.SettingsActivity;
import voxtric.com.diabetescontrol.utilities.GoogleDriveInterface;
import voxtric.com.diabetescontrol.utilities.ViewUtilities;

public class MainActivity extends AwaitRecoveryActivity
{
  private static final String TAG = "MainActivity";

  private static final int REQUEST_EDIT_SETTINGS = 100;
  public static final int RESULT_UPDATE_EVENT_SPINNER = 0x01;
  public static final int RESULT_UPDATE_BGL_HIGHLIGHTING = 0x02;

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
        case R.id.navigation_graph:
          Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
          return false;
      }
      return false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    PDFBoxResourceLoader.init(this);

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

                           if (calendar.getTimeInMillis() >= Long.valueOf(lastSuccessfulBackupTimestamp) && (!Boolean.valueOf(
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
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem)
  {
    Intent intent;
    switch (menuItem.getItemId())
    {
    case R.id.navigation_export_nhs:
    case R.id.navigation_export_excel:
        Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
        return true;
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
      else
      {
        intent = new Intent(this, ExportForegroundService.class);
        intent.putExtra("export_type", menuItem.getItemId());
        ExportDurationDialogFragment dialog = new ExportDurationDialogFragment(intent);
        dialog.showNow(getSupportFragmentManager(), ExportDurationDialogFragment.TAG);
        dialog.initialiseExportButton();
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
      else if ((resultCode & RESULT_UPDATE_BGL_HIGHLIGHTING) == RESULT_UPDATE_BGL_HIGHLIGHTING)
      {
        getFragment(EntryListFragment.class).refreshEntryList();
      }
      break;
    case REQUEST_CHOOSE_DIRECTORY:
      Uri directoryUri = data.getData();
      if (resultCode == RESULT_OK && directoryUri != null)
      {
        moveExportFile(directoryUri);
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

  public void launchExportProgressDialog(@StringRes int exportTitleId)
  {
    if (m_exportProgressDialog != null)
    {
      cancelExportProgressDialog();
    }

    m_exportProgressDialog = new AlertDialog.Builder(this)
        .setTitle(exportTitleId)
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

  private void navigateToPageFragment(int fragmentIndex)
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
        String exportFilePath = intent.getStringExtra("export_file_path");
        String exportFileMimeType = intent.getStringExtra("export_file_mime_type");
        if (exportFilePath != null && exportFileMimeType != null &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
        {
          beginMoveExportFile(exportFilePath, exportFileMimeType);
        }
        break;

      case ExportForegroundService.ACTION_ONGOING:
        launchExportProgressDialog(intent.getIntExtra("message_title_id", R.string.title_undefined));
        break;
      case ExportForegroundService.ACTION_FINISHED:
        if (!intent.getBooleanExtra("success", false))
        {
          launchMessageDialog(intent);
        }
        break;

      case BackupForegroundService.ACTION_ONGOING:
        launchBackupProgressDialog();
      case RecoveryForegroundService.ACTION_ONGOING:
        navigateToPageFragment(getFragmentIndex(EntryListFragment.class));
        break;
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
    ViewUtilities.launchMessageDialog(this,
        intent.getIntExtra("message_title_id", R.string.title_undefined),
        intent.getIntExtra("message_text_id", R.string.message_undefined),
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
            ExportForegroundService.viewFile(this, exportFileUri, m_moveExportFileMimeType);
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
    FragmentStatePagerAdapter fragmentStatePagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager())
    {
      @Override
      public Fragment getItem(int i)
      {
        switch (i)
        {
          case 0:
            return new NewEntryFragment();
          case 1:
          default:
            return new EntryListFragment();
        }
      }

      @Override
      public int getCount()
      {
        return 2;
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
        View view = findViewById(R.id.fragment_container);
        view.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
        {
          inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
      }
    });
  }

  public void openEntryMoreMenu(View view)
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
    ((TextView)view.findViewById(R.id.insulin_name_value)).setText(entry.insulinDose > 0 ? entry.insulinName : "N/A");
    ((TextView)view.findViewById(R.id.insulin_dose_value)).setText(entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : "N/A");
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

          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this)
              .setTitle(getString(intent.getIntExtra("message_title_id", R.string.title_undefined)))
              .setMessage(getString(intent.getIntExtra("message_text_id", R.string.message_undefined)))
              .setPositiveButton(R.string.view_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  ExportForegroundService.viewFile(MainActivity.this, exportFileUri, exportFileMimeType);
                }
              }).setNegativeButton(R.string.share_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  ExportForegroundService.shareFile(MainActivity.this, exportFileUri, exportFileMimeType, exportFile.getName());
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
