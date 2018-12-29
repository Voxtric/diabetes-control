package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.shuhart.bubblepagerindicator.BubblePageIndicator;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Event;

public class NewEntryFragment extends Fragment
{
    // Transferred between rotations.
    private int m_year = 0;
    private int m_month = 0;
    private int m_day = 0;
    private int m_hour = 0;
    private int m_minute = 0;

    private String m_selectedEventName = null;

    // Not transferred between rotations.
    private Date m_date = null;
    private AppDatabase m_database = null;
    private boolean m_ignoreNextOnItemSelectCallback = false;
    ArrayAdapter<String> m_eventSpinnerAdapter = null;

    public NewEntryFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        NestedScrollView view = (NestedScrollView)inflater.inflate(R.layout.fragment_new_entry, container, false);

        initialiseDateButton((Button)view.findViewById(R.id.button_date));
        initialiseTimeButton((Button)view.findViewById(R.id.button_time));
        initialiseEventSpinner((Spinner)view.findViewById(R.id.spinner_event));
        initialiseViewPreviousButton((Button)view.findViewById(R.id.button_see_previous));
        initialiseAddNewEntryButton((Button)view.findViewById(R.id.button_add_new_entry));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        if (activity != null)
        {
            m_database = ((DatabaseActivity)activity).getDatabase();

            //SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
            //String insulinName = preferences.getString("insulin_name", "");
            //((EditText)activity.findViewById(R.id.auto_complete_insulin_name)).setText(insulinName);

            Spinner eventSpinner = activity.findViewById(R.id.spinner_event);
            m_eventSpinnerAdapter = new ArrayAdapter<String>(activity, R.layout.event_spinner_dropdown_item)
            {
                @Override
                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
                {
                    convertView = super.getDropDownView(position, convertView, parent);
                    ViewGroup.LayoutParams layoutParams = convertView.getLayoutParams();
                    layoutParams.height = 100;
                    convertView.setLayoutParams(layoutParams);
                    return convertView;
                }
            };
            eventSpinner.setAdapter(m_eventSpinnerAdapter);

            MainActivity.addHintHide((EditText)activity.findViewById(R.id.auto_complete_insulin_name), Gravity.CENTER, activity);
            MainActivity.addHintHide((EditText)activity.findViewById(R.id.auto_complete_insulin_dose), Gravity.CENTER, activity);
            MainActivity.addHintHide((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level), Gravity.CENTER, activity);
            MainActivity.addHintHide((EditText)activity.findViewById(R.id.auto_complete_food_eaten), Gravity.START | Gravity.TOP, activity);
            MainActivity.addHintHide((EditText)activity.findViewById(R.id.auto_complete_additional_notes), Gravity.START | Gravity.TOP, activity);
        }

        if (savedInstanceState == null)
        {
            updateDateTime(true);
        }
        else
        {
            m_year = savedInstanceState.getInt("year");
            m_month = savedInstanceState.getInt("month");
            m_day = savedInstanceState.getInt("day");
            m_hour = savedInstanceState.getInt("hour");
            m_minute = savedInstanceState.getInt("minute");
            m_selectedEventName = savedInstanceState.getString("selected_event_name");
            updateDateTime(false);
        }

        refreshAutoCompleteView(R.id.auto_complete_insulin_name);
        refreshAutoCompleteView(R.id.auto_complete_insulin_dose);
        refreshAutoCompleteView(R.id.auto_complete_food_eaten);
        refreshAutoCompleteView(R.id.auto_complete_additional_notes);

        updateEventSpinner();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putInt("year", m_year);
        outState.putInt("month", m_month);
        outState.putInt("day", m_day);
        outState.putInt("hour", m_hour);
        outState.putInt("minute", m_minute);
        outState.putString("selected_event_name", m_selectedEventName);
    }

    private void updateDateTime(boolean forceNew)
    {
        Calendar calender = Calendar.getInstance();
        if (forceNew)
        {
            m_year = calender.get(Calendar.YEAR);
            m_month = calender.get(Calendar.MONTH);
            m_day = calender.get(Calendar.DAY_OF_MONTH);
            m_hour = calender.get(Calendar.HOUR_OF_DAY);
            m_minute = calender.get(Calendar.MINUTE);
        }

        Activity activity = getActivity();
        if (activity != null)
        {
            calender.set(Calendar.YEAR, m_year);
            calender.set(Calendar.MONTH, m_month);
            calender.set(Calendar.DAY_OF_MONTH, m_day);
            calender.set(Calendar.HOUR_OF_DAY, m_hour);
            calender.set(Calendar.MINUTE, m_minute);
            m_date = calender.getTime();

            String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(m_date);
            Button dateButton = activity.findViewById(R.id.button_date);
            dateButton.setText(dateString);

            String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(m_date);
            Button timeButton = activity.findViewById(R.id.button_time);
            timeButton.setText(timeString);
        }
    }

    public void updateEventSpinner()
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            AsyncTask.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    final List<Event> events = m_database.eventsDao().getEvents();
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            m_eventSpinnerAdapter.clear();
                            for (int i = 0; i < events.size(); i++)
                            {
                                m_eventSpinnerAdapter.add(events.get(i).name);
                            }

                            if (m_selectedEventName != null)
                            {
                                selectEvent(m_selectedEventName);
                            }
                            else
                            {
                                pickBestEvent();
                            }
                        }
                    });
                }
            });
        }
    }

    private void pickBestEvent()
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            AsyncTask.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    Calendar calender = Calendar.getInstance();
                    calender.setTimeInMillis(0);
                    calender.set(Calendar.HOUR_OF_DAY, m_hour);
                    calender.set(Calendar.MINUTE, m_minute);
                    long timeOnlyTimeStamp = calender.getTimeInMillis();

                    final List<Event> events = m_database.eventsDao().getEvents();
                    long smallestDifference = Long.MAX_VALUE;
                    int closestEventIndex = -1;
                    for (int i = 0; i < events.size(); i++)
                    {
                        long difference = Math.abs(events.get(i).timeInDay - timeOnlyTimeStamp);
                        if (difference < smallestDifference)
                        {
                            smallestDifference = difference;
                            closestEventIndex = i;
                        }
                        else
                        {
                            break;
                        }
                    }
                    if (closestEventIndex != -1)
                    {
                        final int finalClosestEventIndex = closestEventIndex;
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                selectEvent(events.get(finalClosestEventIndex).name);
                            }
                        });
                    }
                }
            });
        }
    }

    private void selectEvent(String eventName)
    {
        Activity activity = getActivity();
        if (activity != null)
        {
            Spinner eventSpinner = activity.findViewById(R.id.spinner_event);
            boolean found = false;
            for (int i = 0; i < eventSpinner.getCount() && !found; i++)
            {
                if (eventName.equals(eventSpinner.getItemAtPosition(i).toString()))
                {
                    m_ignoreNextOnItemSelectCallback = true;
                    eventSpinner.setSelection(i);
                    found = true;
                }
            }
            if (!found)
            {
                m_eventSpinnerAdapter.add(eventName);
                m_ignoreNextOnItemSelectCallback = true;
                eventSpinner.setSelection(eventSpinner.getCount() - 1);
                Toast.makeText(activity, R.string.new_entry_deleted_event_message, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initialiseDateButton(final Button dateButton)
    {
        dateButton.setTypeface(null, Typeface.NORMAL);
        dateButton.setAllCaps(false);
        dateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final DatePicker datePicker = new DatePicker(getActivity());
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setView(datePicker)
                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                m_year = datePicker.getYear();
                                m_month = datePicker.getMonth();
                                m_day = datePicker.getDayOfMonth();
                                updateDateTime(false);
                            }
                        })
                        .create();
                dialog.show();
            }
        });
    }

    private void initialiseTimeButton(final Button timeButton)
    {
        timeButton.setTypeface(null, Typeface.NORMAL);
        timeButton.setAllCaps(false);
        timeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final TimePicker timePicker = new TimePicker(getActivity());
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setView(timePicker)
                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                m_hour = timePicker.getCurrentHour();
                                m_minute = timePicker.getCurrentMinute();
                                updateDateTime(false);
                                if (m_selectedEventName == null)
                                {
                                    pickBestEvent();
                                }
                            }
                        })
                        .create();
                dialog.show();

                // Ensure time picker displays correctly.
                Activity activity = getActivity();
                if (activity != null)
                {
                    Point point = new Point();
                    activity.getWindowManager().getDefaultDisplay().getSize(point);
                    if (point.x > point.y)
                    {
                        timePicker.getLayoutParams().width = WindowManager.LayoutParams.WRAP_CONTENT;
                        timePicker.getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;
                    }
                    else
                    {
                        timePicker.getLayoutParams().width = WindowManager.LayoutParams.MATCH_PARENT;
                        timePicker.getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        });
    }

    private void initialiseEventSpinner(final Spinner eventSpinner)
    {
        eventSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                if (!m_ignoreNextOnItemSelectCallback)
                {
                    m_selectedEventName = eventSpinner.getItemAtPosition(position).toString();
                }
                m_ignoreNextOnItemSelectCallback = false;
            }
        });
    }

    private void initialiseViewPreviousButton(final Button viewPreviousButton)
    {
        viewPreviousButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Activity activity = getActivity();
                if (activity != null)
                {
                    final View layout = View.inflate(activity, R.layout.dialog_choose_criteria, null);
                    int totalActiveRadioButtons = 5;
                    if (((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name)).getText().length() == 0)
                    {
                        layout.findViewById(R.id.radio_insulin_name).setEnabled(false);
                        totalActiveRadioButtons--;
                    }
                    if (((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_dose)).getText().length() == 0)
                    {
                        layout.findViewById(R.id.radio_insulin_dose).setEnabled(false);
                        totalActiveRadioButtons--;
                    }
                    if (((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().length() == 0)
                    {
                        layout.findViewById(R.id.radio_blood_glucose_level).setEnabled(false);
                        totalActiveRadioButtons--;
                    }
                    if (((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_food_eaten)).getText().length() == 0)
                    {
                        layout.findViewById(R.id.radio_food_eaten).setEnabled(false);
                        totalActiveRadioButtons--;
                    }
                    if (((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_additional_notes)).getText().length() == 0)
                    {
                        layout.findViewById(R.id.radio_additional_notes).setEnabled(false);
                        totalActiveRadioButtons--;
                    }
                    determineCriteria(layout, activity);

                    if (totalActiveRadioButtons == 0)
                    {
                        AlertDialog dialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.title_missing_comparison_values)
                                .setMessage(R.string.message_missing_comparison_values)
                                .setPositiveButton(R.string.ok, null)
                                .create();
                        dialog.show();
                    }
                    else
                    {
                        AlertDialog dialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.title_see_previous_criteria)
                                .setView(layout)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.done, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        RadioGroup radioGroup = layout.findViewById(R.id.radio_group_previous_criteria);
                                        int radioGroupButtonID = radioGroup.getCheckedRadioButtonId();
                                        displaySimilar(activity, layout, radioGroupButtonID);

                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                                        if (radioGroupButtonID == layout.findViewById(R.id.radio_insulin_name).getId())
                                        {
                                            preferences.edit().putInt("previous_criteria", 0).apply();
                                        }
                                        else if (radioGroupButtonID == layout.findViewById(R.id.radio_insulin_dose).getId())
                                        {
                                            preferences.edit().putInt("previous_criteria", 1).apply();
                                        }
                                        else if (radioGroupButtonID == layout.findViewById(R.id.radio_blood_glucose_level).getId())
                                        {
                                            preferences.edit().putInt("previous_criteria", 2).apply();
                                        }
                                        else if (radioGroupButtonID == layout.findViewById(R.id.radio_food_eaten).getId())
                                        {
                                            preferences.edit().putInt("previous_criteria", 3).apply();
                                        }
                                        else if (radioGroupButtonID == layout.findViewById(R.id.radio_additional_notes).getId())
                                        {
                                            preferences.edit().putInt("previous_criteria", 4).apply();
                                        }
                                    }
                                })
                                .create();
                        dialog.show();
                    }
                }
            }
        });
    }

    private void initialiseAddNewEntryButton(final Button addNewEntryButton)
    {
        addNewEntryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                saveAutoCompleteView(R.id.auto_complete_insulin_name);
                saveAutoCompleteView(R.id.auto_complete_insulin_dose);
                saveAutoCompleteView(R.id.auto_complete_food_eaten);
                saveAutoCompleteView(R.id.auto_complete_additional_notes);

                final Activity activity = getActivity();
                if (activity != null)
                {
                    String bloodGlucoseLevel = ((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString();
                    String insulinName = ((EditText)activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString();
                    String insulinDose = ((EditText)activity.findViewById(R.id.auto_complete_insulin_dose)).getText().toString();
                    if (bloodGlucoseLevel.length() == 0)
                    {
                        Toast.makeText(activity, R.string.new_entry_bgl_empty_message, Toast.LENGTH_LONG).show();
                    }
                    else if (insulinName.length() == 0 && insulinDose.length() > 0)
                    {
                        Toast.makeText(activity, R.string.new_entry_insulin_name_empty_message, Toast.LENGTH_LONG).show();
                    }
                    else if (insulinDose.length() == 0 && insulinName.length() > 0)
                    {
                        Toast.makeText(activity, R.string.new_entry_insulin_dose_empty_message, Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        tryAddEntry();
                    }
                }
            }
        });
    }

    private void determineCriteria(View layout, Activity activity)
    {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int defaultSelected = preferences.getInt("previous_criteria", -1);
        @IdRes int[] ids = {
                R.id.radio_insulin_name,
                R.id.radio_insulin_dose,
                R.id.radio_blood_glucose_level,
                R.id.radio_food_eaten,
                R.id.radio_additional_notes
        };
        boolean findFirst = true;
        if (defaultSelected != -1)
        {
            if (layout.findViewById(ids[defaultSelected]).isEnabled())
            {
                ((RadioButton)layout.findViewById(ids[defaultSelected])).setChecked(true);
                findFirst = false;
            }
        }
        if (findFirst)
        {
            for (int id : ids)
            {
                if (layout.findViewById(id).isEnabled())
                {
                    ((RadioButton) layout.findViewById(id)).setChecked(true);
                    break;
                }
            }
        }
    }

    private Pair<Integer, List<DataEntry>> getEntries(Activity activity, View criteriaLayout, int radioGroupButtonID, long timeStamp, boolean before)
    {
        List<DataEntry> entries = null;
        if (before)
        {
            if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_name).getId())
            {
                entries = m_database.dataEntriesDao().findPreviousEntryWithInsulinName(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_dose).getId())
            {
                entries = m_database.dataEntriesDao().findPreviousEntryWithInsulinDose(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_insulin_dose)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
            {
                entries = m_database.dataEntriesDao().findPreviousEntryWithBloodGlucoseLevel(
                        timeStamp, Float.valueOf(((EditText) activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString()));
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
            {
                entries = m_database.dataEntriesDao().findPreviousEntryWithFoodEaten(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_food_eaten)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_additional_notes).getId())
            {
                entries = m_database.dataEntriesDao().findPreviousEntryWithAdditionalNotes(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_additional_notes)).getText().toString() + '%');
            }
        }
        else
        {
            if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_name).getId())
            {
                entries = m_database.dataEntriesDao().findNextEntryWithInsulinName(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_dose).getId())
            {
                entries = m_database.dataEntriesDao().findNextEntryWithInsulinDose(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_insulin_dose)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
            {
                entries = m_database.dataEntriesDao().findNextEntryWithBloodGlucoseLevel(
                        timeStamp, Float.valueOf(((EditText) activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString()));
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
            {
                entries = m_database.dataEntriesDao().findNextEntryWithFoodEaten(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_food_eaten)).getText().toString() + '%');
            }
            else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_additional_notes).getId())
            {
                entries = m_database.dataEntriesDao().findNextEntryWithAdditionalNotes(
                        timeStamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.auto_complete_additional_notes)).getText().toString() + '%');
            }
        }

        int startIndex = -1;
        if (entries != null && !entries.isEmpty())
        {
            startIndex = 0;
            List<DataEntry> surrounding = m_database.dataEntriesDao().findFirstAfter(entries.get(0).timeStamp);
            if (!surrounding.isEmpty())
            {
                entries.add(surrounding.get(0));
            }
            surrounding = m_database.dataEntriesDao().findFirstBefore(entries.get(0).timeStamp);
            if (!surrounding.isEmpty())
            {
                entries.add(0, surrounding.get(0));
                startIndex = 1;
            }
        }
        return new Pair<>(startIndex, entries);
    }

    private void displaySimilar(final Activity activity, final View criteriaLayout, final int radioGroupButtonID)
    {
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                final Pair<Integer, List<DataEntry>> entries = getEntries(activity, criteriaLayout, radioGroupButtonID, m_date.getTime(), true);
                final Pair<Integer, List<DataEntry>> previousEntries;
                if (entries.first != -1)
                {
                    previousEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, true);
                }
                else
                {
                    previousEntries = null;
                }
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (previousEntries == null)
                        {
                            AlertDialog dialog = new AlertDialog.Builder(activity)
                                    .setTitle(R.string.title_no_results)
                                    .setMessage(R.string.message_no_results)
                                    .setPositiveButton(R.string.ok, null)
                                    .create();
                            dialog.show();
                        }
                        else
                        {
                            View view = View.inflate(activity, R.layout.dialog_view_previous, null);
                            ViewPager viewPager = view.findViewById(R.id.view_pager_previous_entries);
                            ViewPreviousPagerAdapter adapter = new ViewPreviousPagerAdapter(activity, entries.second);
                            viewPager.setAdapter(adapter);
                            BubblePageIndicator indicator = view.findViewById(R.id.page_indicator_previous_entries);
                            indicator.setViewPager(viewPager);
                            viewPager.setCurrentItem(entries.first);

                            AlertDialog dialog = new AlertDialog.Builder(activity)
                                    .setView(view)
                                    .setNeutralButton(R.string.done, null)
                                    .setNegativeButton(R.string.previous, null)
                                    .setPositiveButton(R.string.next, null)
                                    .create();
                            dialog.show();

                            if (previousEntries.first == -1)
                            {
                                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                            }
                            else
                            {
                                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, dialog, view);
                            }
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                        }
                    }
                });
            }
        });
    }

    private void configureViewPreviousButtons(
            final Activity activity, final View criteriaLayout, final int radioGroupButtonID, final long timeStamp,
            final AlertDialog dialog, final View layoutView)
    {
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                AsyncTask.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final Pair<Integer, List<DataEntry>> entries = getEntries(activity, criteriaLayout, radioGroupButtonID, timeStamp, true);
                        final Pair<Integer, List<DataEntry>> previousEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, true);
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ViewPager viewPager = layoutView.findViewById(R.id.view_pager_previous_entries);
                                ViewPreviousPagerAdapter adapter = new ViewPreviousPagerAdapter(activity, entries.second);
                                viewPager.setAdapter(adapter);
                                BubblePageIndicator indicator = layoutView.findViewById(R.id.page_indicator_previous_entries);
                                indicator.setViewPager(viewPager);
                                viewPager.setCurrentItem(entries.first);
                                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, dialog, layoutView);
                                if (previousEntries.first == -1)
                                {
                                    view.setEnabled(false);
                                }
                            }
                        });
                    }
                });
            }
        });
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                AsyncTask.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final Pair<Integer, List<DataEntry>> entries = getEntries(activity, criteriaLayout, radioGroupButtonID, timeStamp, false);
                        final Pair<Integer, List<DataEntry>> followingEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, false);
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ViewPager viewPager = layoutView.findViewById(R.id.view_pager_previous_entries);
                                ViewPreviousPagerAdapter adapter = new ViewPreviousPagerAdapter(activity, entries.second);
                                viewPager.setAdapter(adapter);
                                BubblePageIndicator indicator = layoutView.findViewById(R.id.page_indicator_previous_entries);
                                indicator.setViewPager(viewPager);
                                viewPager.setCurrentItem(entries.first);
                                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).timeStamp, dialog, layoutView);
                                if (followingEntries.first == -1)
                                {
                                    view.setEnabled(false);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    public DataEntry createEntry(Activity activity)
    {
        DataEntry entry = new DataEntry();
        entry.timeStamp = m_date.getTime();
        entry.event = ((Spinner)activity.findViewById(R.id.spinner_event)).getSelectedItem().toString();
        entry.insulinName = ((EditText)activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString();
        entry.insulinDose = ((EditText)activity.findViewById(R.id.auto_complete_insulin_dose)).getText().toString();
        entry.bloodGlucoseLevel = Float.parseFloat(((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString());
        entry.foodEaten = ((EditText)activity.findViewById(R.id.auto_complete_food_eaten)).getText().toString();
        entry.additionalNotes = ((EditText)activity.findViewById(R.id.auto_complete_additional_notes)).getText().toString();

        if (entry.insulinName.length() == 0)
        {
            entry.insulinName = "N/A";
        }
        if (entry.insulinDose.length() == 0)
        {
            entry.insulinDose = "N/A";
        }
        return entry;
    }

    private void addEntry(final DataEntry entry, final Activity activity, DataEntry entryToReplace)
    {
        if (entryToReplace != null)
        {
            m_database.dataEntriesDao().delete(entryToReplace);
        }
        m_database.dataEntriesDao().insert(entry);
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
                //preferences.edit().putString("insulin_name", entry.insulinName).apply();
                reset();
                Toast.makeText(activity, R.string.new_entry_added_message, Toast.LENGTH_LONG).show();

                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null)
                {
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if (fragments.size() >= 2 && fragments.get(1) instanceof EntryListFragment)
                    {
                        ((EntryListFragment)fragments.get(1)).refreshEntryList();
                    }
                }
            }
        });
    }

    private void tryAddEntry()
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            final DataEntry entry = createEntry(activity);
            AsyncTask.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(entry.timeStamp);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    List<DataEntry> previousEntries = m_database.dataEntriesDao().findFirstBefore(calendar.getTimeInMillis() - 1, entry.event);
                    if (previousEntries.isEmpty())
                    {
                        addEntry(entry, activity, null);
                    }
                    else
                    {
                        final DataEntry previousEntry = previousEntries.get(0);
                        calendar.setTimeInMillis(entry.timeStamp);
                        Calendar previousCalendar = Calendar.getInstance();
                        previousCalendar.setTimeInMillis(previousEntry.timeStamp);
                        if (calendar.get(Calendar.YEAR) != previousCalendar.get(Calendar.YEAR) ||
                            calendar.get(Calendar.MONTH) != previousCalendar.get(Calendar.MONTH) ||
                            calendar.get(Calendar.DAY_OF_MONTH) != previousCalendar.get(Calendar.DAY_OF_MONTH))
                        {
                            addEntry(entry, activity, null);
                        }
                        else
                        {
                            activity.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    AlertDialog dialog = new AlertDialog.Builder(activity)
                                            .setTitle(R.string.title_event_collision)
                                            .setMessage(R.string.message_event_collision_replace)
                                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    Toast.makeText(activity, R.string.new_entry_cancelled_message, Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                                            {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which)
                                                {
                                                    AsyncTask.execute(new Runnable()
                                                    {
                                                        @Override
                                                        public void run()
                                                        {
                                                            addEntry(entry, activity, previousEntry);
                                                        }
                                                    });
                                                }
                                            })
                                            .create();
                                    dialog.show();
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    private void clearText(EditText input)
    {
        input.getText().clear();
        input.getOnFocusChangeListener().onFocusChange(input, false);
    }

    private void reset()
    {
        Activity activity = getActivity();
        if (activity != null)
        {
            updateDateTime(true);

            activity.findViewById(R.id.grid_layout).requestFocus();

            clearText((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level));
            clearText((EditText)activity.findViewById(R.id.auto_complete_insulin_name));
            clearText((EditText)activity.findViewById(R.id.auto_complete_insulin_dose));
            clearText((EditText)activity.findViewById(R.id.auto_complete_food_eaten));
            clearText((EditText)activity.findViewById(R.id.auto_complete_additional_notes));

            m_selectedEventName = null;
            pickBestEvent();
        }
    }

    protected void saveAutoCompleteView(@IdRes int input)
    {
        Activity activity = getActivity();
        if (activity != null)
        {
            String idName = getResources().getResourceName(input);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
            SharedPreferences.Editor editor = preferences.edit();
            Set<String> strings = preferences.getStringSet(idName, null);
            Set<String> newStrings = new HashSet<>();
            if (strings != null)
            {
                newStrings.addAll(strings);
            }
            AutoCompleteTextView textView = activity.findViewById(input);
            String name = textView.getText().toString();
            newStrings.add(name);
            editor.putStringSet(idName, newStrings);
            editor.apply();
            refreshAutoCompleteView(input);
        }
    }

    protected void refreshAutoCompleteView(@IdRes int input)
    {
        Activity activity = getActivity();
        if (activity != null)
        {
            String idName = getResources().getResourceName(input);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            Set<String> strings = prefs.getStringSet(idName, null);
            String[] stringArray = new String[0];
            if (strings != null)
            {
                stringArray = strings.toArray(stringArray);
            }

            AutoCompleteTextView autoText = activity.findViewById(input);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, stringArray);
            autoText.setAdapter(adapter);
        }
    }

    public void setValues(DataEntry entry, Activity activity)
    {
        m_selectedEventName = entry.event;
        updateEventSpinner();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry.timeStamp);
        m_year = calendar.get(Calendar.YEAR);
        m_month = calendar.get(Calendar.MONTH);
        m_day = calendar.get(Calendar.DAY_OF_MONTH);
        m_hour = calendar.get(Calendar.HOUR_OF_DAY);
        m_minute = calendar.get(Calendar.MINUTE);
        updateDateTime(false);

        ((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name)).setText(entry.insulinName);
        ((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_dose)).setText(entry.insulinDose);
        ((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).setText(String.valueOf(entry.bloodGlucoseLevel));
        if (entry.foodEaten.length() > 0)
        {
            AutoCompleteTextView foodEaten = activity.findViewById(R.id.auto_complete_food_eaten);
            foodEaten.setText(entry.foodEaten);
            foodEaten.setGravity(Gravity.START | Gravity.TOP);
        }
        if (entry.additionalNotes.length() > 0)
        {
            AutoCompleteTextView additionalNotes = activity.findViewById(R.id.auto_complete_additional_notes);
            additionalNotes.setText(entry.additionalNotes);
            additionalNotes.setGravity(Gravity.START | Gravity.TOP);
        }
    }
}
