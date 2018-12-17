package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.TargetChange;

public class MainActivity extends DatabaseActivity
{
    private static final int START_FRAGMENT = 0;

    private static final int REQUEST_FIRST_LAUNCH_SETUP = 109;
    private static final int REQUEST_EDIT_EVENTS = 110;
    public static final int RESULT_EVENTS_CHANGED = 111;

    private AppDatabase m_database = null;
    private ViewPager m_viewPager = null;

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

        m_database = Room.databaseBuilder(this, AppDatabase.class, "diabetes-control.db").build();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(m_onNavigationItemSelectedListener);

        m_viewPager = findViewById(R.id.fragment_container);
        initialiseViewPager(m_viewPager, navigation);

        if (savedInstanceState == null)
        {
            navigation.getMenu().getItem(START_FRAGMENT).setChecked(true);
            m_viewPager.setCurrentItem(START_FRAGMENT);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstTime = preferences.getBoolean("first_launch", true);
        if (firstTime)
        {
            preferences.edit().putBoolean("first_launch", false).apply();
            runFirstLaunchSetup();
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
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
        case R.id.navigation_edit_events:
            Intent intent = new Intent(this, EditEventsActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_EVENTS);
            return true;
        case R.id.navigation_edit_contact_details:
            editContactDetails(false);
            return true;
        case R.id.navigation_edit_targets:
            editTargets();
            return true;

        case R.id.navigation_export_ads:
            Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
            return true;
        case R.id.navigation_export_nhs:
            Toast.makeText(MainActivity.this, R.string.not_implemented_message, Toast.LENGTH_LONG).show();
            return true;

        default:
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FIRST_LAUNCH_SETUP)
        {
            requestExportDetails();
            if (resultCode == RESULT_EVENTS_CHANGED)
            {
                ((NewEntryFragment)getSupportFragmentManager().getFragments().get(0)).updateEventSpinner();
            }
        }
        else if (requestCode == REQUEST_EDIT_EVENTS && resultCode == RESULT_EVENTS_CHANGED)
        {
            ((NewEntryFragment)getSupportFragmentManager().getFragments().get(0)).updateEventSpinner();
        }
    }

    public static void addHintHide(final EditText viewWithHint, final int targetGravity, final Activity activity)
    {
        viewWithHint.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            private String originalHint = null;

            @Override
            public void onFocusChange(View view, boolean hasFocus)
            {
                if (originalHint == null)
                {
                    originalHint = viewWithHint.getHint().toString();
                }

                if (hasFocus)
                {
                    viewWithHint.setGravity(targetGravity);
                    viewWithHint.setHint("");

                    // They keyboard may fail to be raised normally on some versions of Android, so do it manually.
                    InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null)
                    {
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                }
                else if (viewWithHint.getText().length() == 0)
                {
                    viewWithHint.setGravity(Gravity.CENTER);
                    viewWithHint.setHint(originalHint);
                }
            }
        });
    }

    private void editContactDetails(final boolean firstTimeLaunch)
    {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        View view = View.inflate(this, R.layout.dialog_edit_contact_details, null);
        final EditText contactName = view.findViewById(R.id.edit_text_contact_name);
        final EditText contactNumber = view.findViewById(R.id.edit_text_contact_number);
        addHintHide(contactName, Gravity.CENTER, this);
        addHintHide(contactNumber, Gravity.CENTER, this);
        contactName.setText(preferences.getString("contact_name", ""));
        contactNumber.setText(preferences.getString("contact_number", ""));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_edit_contact_details)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                preferences.edit().putString("contact_name", contactName.getText().toString().trim()).apply();
                preferences.edit().putString("contact_number", contactNumber.getText().toString().trim()).apply();
                dialog.cancel();
                if (firstTimeLaunch)
                {
                    editTargets();
                }
            }
        });
        if (contactName.getText().length() == 0 || contactNumber.getText().length() == 0)
        {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }

        TextWatcher textWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(contactName.getText().length() > 0 && contactNumber.getText().length() > 0);
            }
        };
        contactName.addTextChangedListener(textWatcher);
        contactNumber.addTextChangedListener(textWatcher);
    }

    private void editTargets()
    {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        View view = View.inflate(this, R.layout.dialog_edit_targets, null);
        final EditText preMealLower = view.findViewById(R.id.edit_text_target_pre_meal_lower);
        final EditText preMealUpper = view.findViewById(R.id.edit_text_target_pre_meal_upper);
        final EditText postMealLower = view.findViewById(R.id.edit_text_target_post_meal_lower);
        final EditText postMealUpper = view.findViewById(R.id.edit_text_target_post_meal_upper);
        addHintHide(preMealLower, Gravity.CENTER, this);
        addHintHide(preMealUpper, Gravity.CENTER, this);
        addHintHide(postMealLower, Gravity.CENTER, this);
        addHintHide(postMealUpper, Gravity.CENTER, this);
        final String preMealLowerString = preferences.getString("pre_meal_lower_target", "");
        final String preMealUpperString = preferences.getString("pre_meal_upper_target", "");
        final String postMealLowerString = preferences.getString("post_meal_lower_target", "");
        final String postMealUpperString = preferences.getString("post_meal_upper_target", "");
        preMealLower.setText(preMealLowerString);
        preMealUpper.setText(preMealUpperString);
        postMealLower.setText(postMealLowerString);
        postMealUpper.setText(postMealUpperString);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_targets)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        final String newPreMealLowerString = preMealLower.getText().toString().trim();
                        final String newPreMealUpperString = preMealUpper.getText().toString().trim();
                        final String newPostMealLowerString = postMealLower.getText().toString().trim();
                        final String newPostMealUpperString = postMealUpper.getText().toString().trim();
                        if (!newPreMealLowerString.equals(preMealLowerString) ||
                            !newPreMealUpperString.equals(preMealUpperString) ||
                            !newPostMealLowerString.equals(postMealLowerString) ||
                            !newPostMealUpperString.equals(postMealUpperString))
                        {
                            preferences.edit().putString("pre_meal_lower_target", newPreMealLowerString).apply();
                            preferences.edit().putString("pre_meal_upper_target", newPreMealUpperString).apply();
                            preferences.edit().putString("post_meal_lower_target", newPostMealLowerString).apply();
                            preferences.edit().putString("post_meal_upper_target", newPostMealUpperString).apply();
                            AsyncTask.execute(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    TargetChange targetChange = new TargetChange();
                                    targetChange.timeStamp = System.currentTimeMillis();
                                    targetChange.preMealLower = Float.valueOf(newPreMealLowerString);
                                    targetChange.preMealUpper = Float.valueOf(newPreMealUpperString);
                                    targetChange.postMealLower = Float.valueOf(newPostMealLowerString);
                                    targetChange.postMealUpper = Float.valueOf(newPostMealUpperString);
                                    m_database.targetChangesDao().insert(targetChange);
                                }
                            });
                        }
                    }
                })
                .create();
        dialog.show();
        if (preMealLower.getText().length() == 0 || preMealUpper.getText().length() == 0 ||
                postMealLower.getText().length() == 0 || postMealUpper.getText().length() == 0)
        {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        }

        TextWatcher textWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
                        preMealLower.getText().length() > 0 && preMealUpper.getText().length() > 0 &&
                        postMealLower.getText().length() > 0 && postMealUpper.getText().length() > 0);
            }
        };
        preMealLower.addTextChangedListener(textWatcher);
        preMealUpper.addTextChangedListener(textWatcher);
        postMealLower.addTextChangedListener(textWatcher);
        postMealUpper.addTextChangedListener(textWatcher);
    }

    private void runFirstLaunchSetup()
    {
        // Ensure default values have been placed into the database.
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                if (m_database.eventsDao().getEvents().isEmpty())
                {
                    EditEventsActivity.addNHSEvents(m_database);
                }
            }
        });

        // Show the user how to configure their app.
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.first_launch_setup_title)
                .setMessage(R.string.first_launch_setup_message_1)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Intent intent = new Intent(MainActivity.this, EditEventsActivity.class);
                        startActivityForResult(intent, REQUEST_FIRST_LAUNCH_SETUP);
                    }
                })
                .create();
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dialog.cancel();
                requestExportDetails();
            }
        });
    }

    private void requestExportDetails()
    {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.first_launch_setup_title)
                .setMessage(R.string.first_launch_setup_message_2)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        editContactDetails(true);
                    }
                })
                .create();
        dialog.show();
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
        final ViewGroup dataView = (ViewGroup)view.getParent();
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

        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.getMenuInflater().inflate(R.menu.entry_more, menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {
                switch (item.getItemId())
                {
                case R.id.navigation_view_full:
                    ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).viewFull(dataView, MainActivity.this);
                    return true;
                case R.id.navigation_edit:
                    ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).launchEdit(dataView, MainActivity.this);
                    return true;
                case R.id.navigation_delete:
                    ((EntryListFragment)getSupportFragmentManager().getFragments().get(1)).deleteEntry(dataView, MainActivity.this);
                    return true;
                case R.id.navigation_cancel:
                    return true;
                }
                return false;
            }
        });
        menu.setOnDismissListener(new PopupMenu.OnDismissListener()
        {
            @Override
            public void onDismiss(PopupMenu menu)
            {
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
        menu.show();
    }

    public static View getFullView(Context context, DataEntry entry)
    {
        View view = View.inflate(context, R.layout.dialog_view_full_entry, null);

        Date date = new Date(entry.timeStamp);
        String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
        String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

        ((TextView)view.findViewById(R.id.text_view_date)).setText(dateString);
        ((TextView)view.findViewById(R.id.text_view_time)).setText(timeString);
        ((TextView)view.findViewById(R.id.text_view_event)).setText(entry.event);
        ((TextView)view.findViewById(R.id.text_view_insulin_name)).setText(entry.insulinName);
        ((TextView)view.findViewById(R.id.text_view_insulin_dose)).setText(entry.insulinDose);
        ((TextView)view.findViewById(R.id.text_view_blood_glucose_level)).setText(String.valueOf(entry.bloodGlucoseLevel));

        TextView foodEatenTextView = view.findViewById(R.id.text_view_food_eaten);
        if (entry.foodEaten.length() == 0)
        {
            foodEatenTextView.setVisibility(View.GONE);
            view.findViewById(R.id.text_view_food_eaten_label).setVisibility(View.GONE);
        }
        else
        {
            foodEatenTextView.setText(entry.foodEaten);
            foodEatenTextView.setGravity(Gravity.TOP | Gravity.START);
        }

        TextView additionalNotesTextView = view.findViewById(R.id.text_view_additional_notes);
        if (entry.additionalNotes.length() == 0)
        {
            additionalNotesTextView.setVisibility(View.GONE);
            view.findViewById(R.id.text_view_additional_notes_label).setVisibility(View.GONE);
        }
        else
        {
            additionalNotesTextView.setText(entry.additionalNotes);
            additionalNotesTextView.setGravity(Gravity.TOP | Gravity.START);
        }

        return view;
    }
}
