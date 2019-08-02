package voxtric.com.diabetescontrol;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.Food;
import voxtric.com.diabetescontrol.exporting.ExportDurationDialogFragment;
import voxtric.com.diabetescontrol.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity
{
  private static final int START_FRAGMENT = 0;

  private static final int REQUEST_EDIT_SETTINGS = 100;
  public static final int RESULT_UPDATE_EVENT_SPINNER = 0x01;
  public static final int RESULT_UPDATE_BGL_HIGHLIGHTING = 0x02;

  private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 112;

  private ViewPager m_viewPager = null;

  private String m_exportTitle = null;
  private String m_exportStartMessage = null;
  private String m_exportEndMessage = null;

  private PopupMenu m_activeMenu = null;

  private BottomNavigationView.OnNavigationItemSelectedListener m_onNavigationItemSelectedListener
      = new BottomNavigationView.OnNavigationItemSelectedListener()
  {
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
      switch (item.getItemId())
      {
        case R.id.navigation_add_new:
          m_viewPager.setCurrentItem(0);
          return true;
        case R.id.navigation_view_all:
          m_viewPager.setCurrentItem(1);
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

    if (savedInstanceState == null)
    {
      navigation.getMenu().getItem(START_FRAGMENT).setChecked(true);
      m_viewPager.setCurrentItem(START_FRAGMENT);
    }
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
        Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
        return true;
      case R.id.navigation_export_ads:
        export(getString(R.string.ads_export_title), getString(R.string.ads_export_start_message), getString(R.string.ads_export_end_message));
        return true;
      case R.id.navigation_export_csv:
        Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
        return true;
      case R.id.navigation_export_xlsx:
        Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
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
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_EDIT_SETTINGS)
    {
      if ((resultCode & RESULT_UPDATE_EVENT_SPINNER) == RESULT_UPDATE_EVENT_SPINNER)
      {
        ((NewEntryFragment) getSupportFragmentManager().getFragments().get(0)).updateEventSpinner();
      }
      else if ((resultCode & RESULT_UPDATE_BGL_HIGHLIGHTING) == RESULT_UPDATE_BGL_HIGHLIGHTING)
      {
        ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).refreshEntryList();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE)
    {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        export(m_exportTitle, m_exportStartMessage, m_exportEndMessage);
      }
      else
      {
        Toast.makeText(this, R.string.write_external_storage_export_permission_needed_message, Toast.LENGTH_LONG).show();
      }
    }
    else
    {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  private void export(final String title, String startMessage, String endMessage)
  {
    m_exportTitle = title;
    m_exportStartMessage = startMessage;
    m_exportEndMessage = endMessage;

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
    {
      int hasWriteExternalStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
      if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
      {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
          AlertDialog dialog = new AlertDialog.Builder(this)
              .setTitle(R.string.permission_justification_title)
              .setMessage(R.string.write_external_storage_export_permission_justification_message)
              .setNegativeButton(R.string.deny_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  Toast.makeText(MainActivity.this, R.string.write_external_storage_export_permission_needed_message, Toast.LENGTH_LONG).show();
                }
              })
              .setPositiveButton(R.string.allow_dialog_option, new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
                }
              })
              .create();
          dialog.show();
        }
        else
        {
          requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
        }
        return;
      }
    }

    ExportDurationDialogFragment dialog = new ExportDurationDialogFragment();
    dialog.setText(title, startMessage, endMessage);
    dialog.showNow(getSupportFragmentManager(), ExportDurationDialogFragment.TAG);
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
            ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).viewFull(MainActivity.this, dataView);
            return true;
          case R.id.navigation_edit:
            ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).launchEdit(MainActivity.this, dataView);
            return true;
          case R.id.navigation_delete:
            ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).deleteEntry(MainActivity.this, dataView);
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
}
