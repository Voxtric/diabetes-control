package com.voxtric.diabetescontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.shuhart.bubblepagerindicator.BubblePageIndicator;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntriesDao;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Event;
import com.voxtric.diabetescontrol.database.EventsDao;
import com.voxtric.diabetescontrol.database.Food;
import com.voxtric.diabetescontrol.database.FoodsDao;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.settings.EditEventsActivity;
import com.voxtric.diabetescontrol.utilities.AutoCompleteTextViewUtilities;
import com.voxtric.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import com.voxtric.diabetescontrol.utilities.DecimalDigitsInputFilter;
import com.voxtric.diabetescontrol.utilities.GoogleDriveInterface;
import com.voxtric.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class NewEntryFragment extends Fragment
{
  private final int MAX_FOOD_EATEN_ITEM_LENGTH = 50;

  // Transferred between rotations.
  private int m_year = 0;
  private int m_month = 0;
  private int m_day = 0;
  private int m_hour = 0;
  private int m_minute = 0;

  private String m_currentEventName = null;
  private boolean m_eventNameAutoSelected = true;
  private boolean m_eventNameAutoSelecting = false;

  // Not transferred between rotations.
  private Date m_date = null;
  private ArrayAdapter<String> m_eventSpinnerAdapter = null;

  private final HashMap<View, ListItemTextWatcher> m_foodListTextWatchers = new HashMap<>();

  public NewEntryFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    NestedScrollView view = (NestedScrollView)inflater.inflate(R.layout.fragment_new_entry, container, false);

    initialiseDateButton((Button)view.findViewById(R.id.date_button));
    initialiseTimeButton((Button)view.findViewById(R.id.time_button));
    initialiseEventSpinner((Spinner)view.findViewById(R.id.event_spinner));
    initialiseViewPreviousButton((Button)view.findViewById(R.id.see_previous_button));
    initialiseAddNewEntryButton((Button)view.findViewById(R.id.add_new_entry_button));

    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);

    final Activity activity = getActivity();
    if (activity != null)
    {
      final int padding = (int)getResources().getDimension(R.dimen.text_size);
      Spinner eventSpinner = activity.findViewById(R.id.event_spinner);
      m_eventSpinnerAdapter = new ArrayAdapter<String>(activity, R.layout.event_spinner_dropdown_item)
      {
        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
        {
          convertView = super.getDropDownView(position, convertView, parent);
          if (position > 0)
          {
            convertView.setPaddingRelative(0, padding, 0, 0);
          }
          return convertView;
        }
      };
      eventSpinner.setAdapter(m_eventSpinnerAdapter);

      EditText bglInput = activity.findViewById(R.id.blood_glucose_level_input);
      CompositeOnFocusChangeListener.applyListenerToView(bglInput, new HintHideOnFocusChangeListener(bglInput, Gravity.CENTER));

      AutoCompleteTextView insulinNameInput = activity.findViewById(R.id.insulin_name_input);
      CompositeOnFocusChangeListener.applyListenerToView(insulinNameInput, new HintHideOnFocusChangeListener(insulinNameInput, Gravity.CENTER));

      EditText insulinDoseInput = activity.findViewById(R.id.insulin_dose_input);
      CompositeOnFocusChangeListener.applyListenerToView(insulinDoseInput, new HintHideOnFocusChangeListener(insulinDoseInput, Gravity.CENTER));

      AutoCompleteTextView additionalNotesInput = activity.findViewById(R.id.additional_notes_input);
      CompositeOnFocusChangeListener.applyListenerToView(additionalNotesInput, new HintHideOnFocusChangeListener(additionalNotesInput, Gravity.START | Gravity.TOP));

      ((EditText)activity.findViewById(R.id.blood_glucose_level_input)).setFilters(
          new InputFilter[] { new DecimalDigitsInputFilter(2, 1) });

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, -5);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, (AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input), calendar.getTimeInMillis());
      calendar.add(Calendar.DAY_OF_MONTH, 5);
      calendar.add(Calendar.MONTH, -1);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, (AutoCompleteTextView)activity.findViewById(R.id.additional_notes_input), calendar.getTimeInMillis());

      LinearLayout foodEatenItemList = activity.findViewById(R.id.food_eaten_inputs_layout);
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
        m_currentEventName = savedInstanceState.getString("current_event_name");
        m_eventNameAutoSelected = savedInstanceState.getBoolean("event_name_auto_selected");

        String[] foodNames = savedInstanceState.getStringArray("food_names");
        if (foodNames != null)
        {
          for (String foodName : foodNames)
          {
            addNewListItemAutoCompleteTextView(activity, foodEatenItemList, R.string.food_item_hint, Food.TAG, foodName);
          }
        }

        updateDateTime(false);
      }
      AutoCompleteTextView emptyFoodEatenItemInput = addNewListItemAutoCompleteTextView(activity, foodEatenItemList, R.string.food_item_hint, Food.TAG, null);
      calendar.add(Calendar.MONTH, -1);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, emptyFoodEatenItemInput, calendar.getTimeInMillis());
    }

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
    outState.putString("current_event_name", m_currentEventName);
    outState.putBoolean("event_name_auto_selected", m_eventNameAutoSelected);

    Activity activity = getActivity();
    if (activity != null)
    {
      outState.putStringArray("food_names", getFoodNames(getActivity()));
    }
  }

  private String[] getFoodNames(Activity activity)
  {
    LinearLayout foodEatenItemsLayout = activity.findViewById(R.id.food_eaten_inputs_layout);
    HashSet<String> foodNames = new HashSet<>();
    for (int i = 0; i < foodEatenItemsLayout.getChildCount(); i++)
    {
      AutoCompleteTextView foodEatenItemInput = (AutoCompleteTextView)foodEatenItemsLayout.getChildAt(i);
      String foodName = foodEatenItemInput.getText().toString().trim();
      if (foodName.length() > 0)
      {
        foodNames.add(foodName);
      }
    }
    return foodNames.toArray(new String[0]);
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
      calender.set(Calendar.SECOND, 0);
      calender.set(Calendar.MILLISECOND, 0);
      m_date = calender.getTime();

      String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(m_date);
      Button dateButton = activity.findViewById(R.id.date_button);
      dateButton.setText(dateString);

      String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(m_date);
      Button timeButton = activity.findViewById(R.id.time_button);
      timeButton.setText(timeString);
    }
  }

  void updateEventSpinner()
  {
    final Activity activity = getActivity();
    if (activity != null)
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          final List<Event> events = AppDatabase.getInstance().eventsDao().getEvents();
          if (events.isEmpty())
          {
            events.addAll(EditEventsActivity.addNHSEvents(activity));
          }
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

              if (m_currentEventName == null || m_eventNameAutoSelected)
              {
                pickBestEvent();
              }
              else
              {
                selectEvent(m_currentEventName);
              }
            }
          });
        }
      });
    }
  }

  private void eventDataReady(Activity activity)
  {
    if ((activity instanceof MainActivity) && ((MainActivity)activity).fragmentActive(getClass()))
    {
      ShowcaseViewHandler.handleAddNewEntryFragmentShowcaseViews((MainActivity)activity);
    }
  }

  private void pickBestEvent()
  {
    final Activity activity = getActivity();
    if (activity != null && !RecoveryForegroundService.isDownloading())
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          Calendar calendar = Calendar.getInstance();
          calendar.clear();
          calendar.set(
              calendar.getMinimum(Calendar.YEAR),
              calendar.getMinimum(Calendar.MONTH),
              calendar.getMinimum(Calendar.DATE),
              m_hour, m_minute,
              calendar.getMinimum(Calendar.SECOND));
          long timeOnlyTimeStamp = calendar.getTimeInMillis();

          final List<Event> events = AppDatabase.getInstance().eventsDao().getEventsTimeOrdered();
          long smallestDifference = Long.MAX_VALUE;
          int closestEventIndex = -1;

          for (int i = 0; i < events.size(); i++)
          {
            long difference = Math.abs(events.get(i).timeInDay - timeOnlyTimeStamp);
            if (difference < smallestDifference)
            {
              closestEventIndex = i;
              smallestDifference = difference;
            }

            // Account for 24 hour roll-over backwards.
            difference = Math.abs(events.get(i).timeInDay - (timeOnlyTimeStamp - 86400000));
            if (difference < smallestDifference)
            {
              closestEventIndex = i;
              smallestDifference = difference;
            }

            // Account for 24 hour roll-over forwards.
            difference = Math.abs(events.get(i).timeInDay - (timeOnlyTimeStamp + 86400000));
            if (difference < smallestDifference)
            {
              closestEventIndex = i;
              smallestDifference = difference;
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
                m_currentEventName = events.get(finalClosestEventIndex).name;
                m_eventNameAutoSelecting = true;
                selectEvent(m_currentEventName);
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
      Spinner eventSpinner = activity.findViewById(R.id.event_spinner);
      boolean found = false;
      for (int i = 0; i < eventSpinner.getCount() && !found; i++)
      {
        if (eventName.equals(eventSpinner.getItemAtPosition(i).toString()))
        {
          eventSpinner.setSelection(i);
          found = true;
        }
      }
      if (!found)
      {
        m_eventSpinnerAdapter.add(eventName);
        eventSpinner.setSelection(eventSpinner.getCount() - 1);
        Toast.makeText(activity, R.string.new_entry_deleted_event_message, Toast.LENGTH_LONG).show();
      }
      eventDataReady(activity);
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
        Activity activity = getActivity();
        if (activity != null)
        {
          Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
          DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener()
          {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth)
            {
              m_year = year;
              m_month = month;
              m_day = dayOfMonth;
              updateDateTime(false);
            }
          };
          final DatePickerDialog datePickerDialog = new DatePickerDialog(activity, onDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
          datePickerDialog.show();
        }
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
        final Activity activity = getActivity();
        if (activity != null)
        {
          Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
          TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener()
          {
            @Override
            public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute)
            {
              m_hour = hourOfDay;
              m_minute = minute;
              updateDateTime(false);
              if (m_currentEventName == null || m_eventNameAutoSelected)
              {
                pickBestEvent();
              }
            }
          };
          final TimePickerDialog timePickerDialog = new TimePickerDialog(activity,
                                                                         timeSetListener,
                                                                         calendar.get(Calendar.HOUR_OF_DAY),
                                                                         calendar.get(Calendar.MINUTE),
                                                                         false);
          timePickerDialog.show();
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
        m_eventNameAutoSelected = m_eventNameAutoSelecting;
        m_currentEventName = eventSpinner.getItemAtPosition(position).toString();
        m_eventNameAutoSelecting = false;
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

          if (((AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_insulin_name).setEnabled(false);
            totalActiveRadioButtons--;
          }

          if (((EditText)activity.findViewById(R.id.insulin_dose_input)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_insulin_dose).setEnabled(false);
            totalActiveRadioButtons--;
          }

          if (((EditText)activity.findViewById(R.id.blood_glucose_level_input)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_blood_glucose_level).setEnabled(false);
            totalActiveRadioButtons--;
          }

          if (getFoodNames(activity).length == 0)
          {
            layout.findViewById(R.id.radio_food_eaten).setEnabled(false);
            totalActiveRadioButtons--;
          }

          if (((AutoCompleteTextView)activity.findViewById(R.id.additional_notes_input)).getText().length() == 0)
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
                .setPositiveButton(R.string.ok_dialog_option, null)
                .create();
            dialog.show();
          }
          else
          {
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_see_previous_criteria)
                .setView(layout)
                .setNegativeButton(R.string.cancel_dialog_option, null)
                .setPositiveButton(R.string.done_dialog_option, new DialogInterface.OnClickListener()
                {
                  @Override
                  public void onClick(DialogInterface dialog, int which)
                  {
                    RadioGroup radioGroup = layout.findViewById(R.id.radio_group_previous_criteria);
                    int radioGroupButtonID = radioGroup.getCheckedRadioButtonId();
                    displaySimilar(activity, layout, radioGroupButtonID);

                    SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
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
        final Activity activity = getActivity();
        if (activity != null)
        {
          AutoCompleteTextViewUtilities.saveAutoCompleteView(activity, (AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input));
          AutoCompleteTextViewUtilities.saveAutoCompleteView(activity, (AutoCompleteTextView)activity.findViewById(R.id.additional_notes_input));
          LinearLayout foodEatenItemList = activity.findViewById(R.id.food_eaten_inputs_layout);
          for (int i = 0; i < foodEatenItemList.getChildCount(); i++)
          {
            AutoCompleteTextView foodEatenItem = (AutoCompleteTextView)foodEatenItemList.getChildAt(i);
            if (foodEatenItem.getText().toString().trim().length() > 0)
            {
              AutoCompleteTextViewUtilities.saveAutoCompleteView(activity, foodEatenItem);
            }
          }
          beginNewEntryProcess(activity, null);
        }
      }
    });
  }

  private void determineCriteria(View layout, Activity activity)
  {
    final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    int defaultSelected = preferences.getInt("previous_criteria", -1);
    @IdRes int[] ids = {
        R.id.radio_blood_glucose_level,
        R.id.radio_insulin_name,
        R.id.radio_insulin_dose,
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
          ((RadioButton)layout.findViewById(id)).setChecked(true);
          break;
        }
      }
    }
  }

  private Pair<Integer, List<DataEntry>> getEntries(Activity activity, View criteriaLayout, int radioGroupButtonID, long timestamp, boolean before)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    DataEntry entry = null;
    if (before)
    {
      if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
      {
        entry = dataEntriesDao.findPreviousEntryWithBloodGlucoseLevel(
            timestamp, Float.parseFloat(((EditText)activity.findViewById(R.id.blood_glucose_level_input)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_name).getId())
      {
        entry = dataEntriesDao.findPreviousEntryWithInsulinName(
            timestamp, '%' + ((AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input)).getText().toString() + '%');
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_dose).getId())
      {
        entry = dataEntriesDao.findPreviousEntryWithInsulinDose(
            timestamp, Integer.parseInt(((EditText)activity.findViewById(R.id.insulin_dose_input)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
      {
        entry = findPreviousEntryWithFood(timestamp, getFoodNames(activity));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_additional_notes).getId())
      {
        entry = dataEntriesDao.findPreviousEntryWithAdditionalNotes(
            timestamp, '%' + ((AutoCompleteTextView)activity.findViewById(R.id.additional_notes_input)).getText().toString() + '%');
      }
    }
    else
    {
      if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
      {
        entry = dataEntriesDao.findNextEntryWithBloodGlucoseLevel(
            timestamp, Float.parseFloat(((EditText) activity.findViewById(R.id.blood_glucose_level_input)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_name).getId())
      {
        entry = dataEntriesDao.findNextEntryWithInsulinName(
            timestamp, '%' + ((AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input)).getText().toString() + '%');
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_insulin_dose).getId())
      {
        entry = dataEntriesDao.findNextEntryWithInsulinDose(
            timestamp, Integer.parseInt(((EditText)activity.findViewById(R.id.insulin_dose_input)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
      {
        entry = findFollowingEntryWithFood(timestamp, getFoodNames(activity));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_additional_notes).getId())
      {
        entry = dataEntriesDao.findNextEntryWithAdditionalNotes(
            timestamp, '%' + ((AutoCompleteTextView) activity.findViewById(R.id.additional_notes_input)).getText().toString() + '%');
      }
    }

    int startIndex = -1;
    List<DataEntry> entries = null;
    if (entry != null)
    {
      entries = new ArrayList<>();
      entries.add(entry);

      startIndex = 0;
      DataEntry surrounding = dataEntriesDao.findFirstAfter(entries.get(0).actualTimestamp);
      if (surrounding != null)
      {
        entries.add(surrounding);
      }
      surrounding = dataEntriesDao.findFirstBefore(entries.get(0).actualTimestamp);
      if (surrounding != null)
      {
        entries.add(0, surrounding);
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
          previousEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, true);
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
                  .setPositiveButton(R.string.ok_dialog_option, null)
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
                  .setNeutralButton(R.string.done_dialog_option, null)
                  .setNegativeButton(R.string.previous_dialog_option, null)
                  .setPositiveButton(R.string.next_dialog_option, null)
                  .create();
              dialog.show();

              if (previousEntries.first == -1)
              {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
              }
              else
              {
                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, dialog, view);
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
            final Pair<Integer, List<DataEntry>> previousEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, true);
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
                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, dialog, layoutView);
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
            final Pair<Integer, List<DataEntry>> followingEntries = getEntries(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, false);
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
                configureViewPreviousButtons(activity, criteriaLayout, radioGroupButtonID, entries.second.get(entries.first).actualTimestamp, dialog, layoutView);
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

  private DataEntry createEntry(Activity activity)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(m_date);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    DataEntry entry = new DataEntry();
    entry.actualTimestamp = m_date.getTime();
    entry.dayTimeStamp = calendar.getTimeInMillis();
    entry.event = ((Spinner)activity.findViewById(R.id.event_spinner)).getSelectedItem().toString();

    String insulinName = ((EditText)activity.findViewById(R.id.insulin_name_input)).getText().toString().trim();
    if (insulinName.length() > 0)
    {
      entry.insulinName = insulinName;
      entry.insulinDose = Integer.parseInt(((EditText)activity.findViewById(R.id.insulin_dose_input)).getText().toString());
    }
    else
    {
      entry.insulinName = null;
      entry.insulinDose = 0;
    }

    entry.bloodGlucoseLevel = Float.parseFloat(((EditText)activity.findViewById(R.id.blood_glucose_level_input)).getText().toString());
    entry.additionalNotes = ((EditText)activity.findViewById(R.id.additional_notes_input)).getText().toString().trim();
    return entry;
  }

  private List<Food> createFoodList(Activity activity, DataEntry associatedEntry)
  {
    List<Food> foodList = new ArrayList<>();
    String[] foodNames = getFoodNames(activity);
    for (String foodName : foodNames)
    {
      Food food = new Food();
      food.dataEntryTimestamp = associatedEntry.actualTimestamp;
      food.name = foodName;
      foodList.add(food);
    }
    return foodList;
  }

  private void updateUIWithNewEntry(final Activity activity)
  {
    if (activity instanceof  EditEntryActivity)
    {
      activity.setResult(EntryListFragment.RESULT_LIST_UPDATE_NEEDED);
      activity.finish();
      Toast.makeText(activity, R.string.changes_saved_message, Toast.LENGTH_LONG).show();
    }
    else
    {
      reset();
      Toast.makeText(activity, R.string.new_entry_added_message, Toast.LENGTH_LONG).show();

      FragmentManager fragmentManager = getFragmentManager();
      if (fragmentManager != null)
      {
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments.size() >= 2 && fragments.get(1) instanceof EntryListFragment)
        {
          ((EntryListFragment) fragments.get(1)).refreshEntryList();
        }
      }
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

      activity.findViewById(R.id.new_entry_content).requestFocus();

      clearText((EditText)activity.findViewById(R.id.blood_glucose_level_input));
      clearText((EditText)activity.findViewById(R.id.insulin_name_input));
      clearText((EditText)activity.findViewById(R.id.insulin_dose_input));
      clearText((EditText)activity.findViewById(R.id.additional_notes_input));

      LinearLayout foodItemsLayout = activity.findViewById(R.id.food_eaten_inputs_layout);
      AutoCompleteTextView foodEatenItemInput = (AutoCompleteTextView)foodItemsLayout.getChildAt(foodItemsLayout.getChildCount() - 1);
      foodItemsLayout.removeAllViews();
      foodItemsLayout.addView(foodEatenItemInput);
      String newHint = getString(R.string.food_item_hint, 1);
      foodEatenItemInput.setHint(newHint);
      foodEatenItemInput.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size));
      foodEatenItemInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_FOOD_EATEN_ITEM_LENGTH) });
      CompositeOnFocusChangeListener compositeOnFocusChangeListener = (CompositeOnFocusChangeListener)foodEatenItemInput.getOnFocusChangeListener();
      HintHideOnFocusChangeListener hintHideOnFocusChangeListener = compositeOnFocusChangeListener.getInstance(HintHideOnFocusChangeListener.class);
      hintHideOnFocusChangeListener.changeOriginalHint(newHint);
      AutoCompleteTextViewUtilities.refreshAutoCompleteView(activity, foodEatenItemInput, null);

      m_currentEventName = null;
      m_eventNameAutoSelected = true;
      pickBestEvent();
    }
  }

  void setValues(Activity activity, DataEntry entry, List<Food> foodList)
  {
    m_currentEventName = entry.event;
    m_eventNameAutoSelected = false;
    updateEventSpinner();

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.actualTimestamp);
    m_year = calendar.get(Calendar.YEAR);
    m_month = calendar.get(Calendar.MONTH);
    m_day = calendar.get(Calendar.DAY_OF_MONTH);
    m_hour = calendar.get(Calendar.HOUR_OF_DAY);
    m_minute = calendar.get(Calendar.MINUTE);
    updateDateTime(false);

    ((EditText)activity.findViewById(R.id.blood_glucose_level_input)).setText(String.valueOf(entry.bloodGlucoseLevel));
    ((AutoCompleteTextView)activity.findViewById(R.id.insulin_name_input)).setText(entry.insulinDose > 0 ? entry.insulinName : "");
    ((EditText)activity.findViewById(R.id.insulin_dose_input)).setText(entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : "");

    if (foodList.size() > 0)
    {
      LinearLayout foodItemList = activity.findViewById(R.id.food_eaten_inputs_layout);
      AutoCompleteTextView lastItem = (AutoCompleteTextView)foodItemList.getChildAt(0);
      ListItemTextWatcher textWatcher = m_foodListTextWatchers.get(lastItem);
      if (textWatcher != null)
      {
        textWatcher.m_ignoreTextChanges = true;
      }
      lastItem.setText(foodList.get(0).name);
      for (int i = 1; i < foodList.size(); i++)
      {
        AutoCompleteTextView newItem = addNewListItemAutoCompleteTextView(activity, foodItemList, R.string.food_item_hint, Food.TAG, foodList.get(i).name);
        lastItem.setNextFocusForwardId(newItem.getId());
        lastItem = newItem;
      }
      AutoCompleteTextView finalEmptyItem = addNewListItemAutoCompleteTextView(activity, foodItemList, R.string.food_item_hint, Food.TAG, null);
      lastItem.setNextFocusForwardId(finalEmptyItem.getId());
    }

    if (entry.additionalNotes.length() > 0)
    {
      AutoCompleteTextView additionalNotes = activity.findViewById(R.id.additional_notes_input);
      additionalNotes.setText(entry.additionalNotes);
      additionalNotes.setGravity(Gravity.START | Gravity.TOP);
    }
  }

  void beginNewEntryProcess(final Activity activity, DataEntry entryToReplace)
  {
    boolean proceed = true;

    String bloodGlucoseLevel = ((EditText)activity.findViewById(R.id.blood_glucose_level_input)).getText().toString();
    String insulinName = ((EditText)activity.findViewById(R.id.insulin_name_input)).getText().toString().trim();
    String insulinDose = ((EditText)activity.findViewById(R.id.insulin_dose_input)).getText().toString();
    if (bloodGlucoseLevel.length() == 0)
    {
      proceed = false;
      Toast.makeText(activity, R.string.new_entry_bgl_empty_message, Toast.LENGTH_LONG).show();
    }
    else if (insulinName.length() == 0)
    {
      if (insulinDose.length() > 0)
      {
        proceed = false;
        Toast.makeText(activity, R.string.new_entry_insulin_name_empty_message, Toast.LENGTH_LONG).show();
      }
    }
    else if (insulinDose.length() == 0)
    {
      proceed = false;
      Toast.makeText(activity, R.string.new_entry_insulin_dose_empty_message, Toast.LENGTH_LONG).show();
    }

    if (proceed)
    {
      final DataEntry entry = createEntry(activity);
      final List<Food> foods = createFoodList(activity, entry);
      checkFutureEntry(activity, entry, entryToReplace, foods);
    }
  }

  private void checkFutureEntry(final Activity activity, final DataEntry entry, final DataEntry entryToReplace, final List<Food> foodList)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, 15);
    if (calendar.getTimeInMillis() < entry.actualTimestamp)
    {
      AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.title_future_entry)
          .setMessage(R.string.message_future_entry)
          .setNegativeButton(R.string.cancel_dialog_option, null)
          .setPositiveButton(R.string.save_dialog_option, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  checkDateMismatch(activity, entry, entryToReplace, foodList);
                }
              });
            }
          })
          .create();
      dialog.show();
    }
    else
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          checkDateMismatch(activity, entry, entryToReplace, foodList);
        }
      });
    }
  }

  private void checkDateMismatch(final Activity activity, final DataEntry entry, final DataEntry entryToReplace, final List<Food> foodList)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    EventsDao eventsDao = AppDatabase.getInstance().eventsDao();

    boolean queryDateMismatch = true;
    Event event = eventsDao.getEvent(entry.event);
    if (event != null)
    {
      queryDateMismatch = false;
      DataEntry previousEntry = dataEntriesDao.findFirstBefore(entry.actualTimestamp);
      if (previousEntry != null)
      {
        Event previousEvent = eventsDao.getEvent(previousEntry.event);
        if (previousEvent != null)
        {
          queryDateMismatch = event.order > previousEvent.order && entry.dayTimeStamp > previousEntry.dayTimeStamp;
        }
        else
        {
          queryDateMismatch = true;
        }
      }

      if (!queryDateMismatch)
      {
        Event firstEvent = eventsDao.getEvents().get(0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry.actualTimestamp);
        calendar.set(
            calendar.getMinimum(Calendar.YEAR),
            calendar.getMinimum(Calendar.MONTH),
            calendar.getMinimum(Calendar.DATE));
        queryDateMismatch = event.id != firstEvent.id && firstEvent.timeInDay > calendar.getTimeInMillis() &&
            (previousEntry != null && !entry.event.equals(previousEntry.event));
      }
    }

    if (queryDateMismatch)
    {
      activity.runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          queryDateMismatch(activity, entry, entryToReplace, foodList);
        }
      });
    }
    else
    {
      checkEventOverlap(activity, entry, entryToReplace, foodList);
    }
  }

  private void queryDateMismatch(final Activity activity, final DataEntry entry, final DataEntry entryToReplace, final List<Food> foodList)
  {
    Date date = new Date(entry.dayTimeStamp);
    String dateString = DateFormat.getDateInstance(DateFormat.SHORT).format(date);

    final Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.dayTimeStamp);
    calendar.add(Calendar.DAY_OF_MONTH, -1);
    Date previousDate = calendar.getTime();
    String previousDateString = DateFormat.getDateInstance(DateFormat.SHORT).format(previousDate);

    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity)
        .setTitle(R.string.title_event_date_mismatch)
        .setMessage(activity.getString(R.string.message_event_date_mismatch, dateString, previousDateString))
        .setPositiveButton(R.string.yes_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialogInterface, int i)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                entry.dayTimeStamp = calendar.getTimeInMillis();
                checkEventOverlap(activity, entry, entryToReplace, foodList);
              }
            });
          }
        })
        .setNegativeButton(R.string.no_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialogInterface, int i)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                checkEventOverlap(activity, entry, entryToReplace, foodList);
              }
            });
          }
        })
        .setNeutralButton(R.string.cancel_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialogInterface, int i)
          {
            Toast.makeText(activity, R.string.new_entry_cancelled_message, Toast.LENGTH_LONG).show();
          }
        })
        .create();
    dialog.show();
  }

  private void checkEventOverlap(final Activity activity, final DataEntry entry, final DataEntry entryToReplace, final List<Food> foodList)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    final DataEntry eventOverlappingEntry = dataEntriesDao.findOverlapping(entry.dayTimeStamp, entry.event);
    final DataEntry timeOverlappingEntry = dataEntriesDao.getEntry(entry.actualTimestamp);
    if (entryToReplace != null)
    {
      if ((eventOverlappingEntry == null || eventOverlappingEntry.actualTimestamp == entryToReplace.actualTimestamp) &&
          (timeOverlappingEntry == null || timeOverlappingEntry.actualTimestamp == entryToReplace.actualTimestamp))
      {
        addEntry(activity, entry, entryToReplace, foodList);
      }
      else
      {
        activity.runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_entry_collision)
                .setMessage(R.string.message_entry_collision)
                .setPositiveButton(R.string.ok_dialog_option, null)
                .create();
            dialog.show();
          }
        });
      }
    }
    else if (eventOverlappingEntry == null && timeOverlappingEntry == null)
    {
      addEntry(activity, entry, null, foodList);
    }
    else
    {
      activity.runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          queryEventOverlap(activity, entry, eventOverlappingEntry, foodList, eventOverlappingEntry != null);
        }
      });
    }
  }

  private void queryEventOverlap(final Activity activity, final DataEntry entry, final DataEntry entryToReplace, final List<Food> foodList, boolean eventRelated)
  {
    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity)
        .setTitle(R.string.title_entry_collision)
        .setMessage(eventRelated ? R.string.message_entry_event_collision_replace : R.string.message_entry_time_collision_replace)
        .setNegativeButton(R.string.cancel_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            Toast.makeText(activity, R.string.new_entry_cancelled_message, Toast.LENGTH_LONG).show();
          }
        })
        .setPositiveButton(R.string.overwrite_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                addEntry(activity, entry, entryToReplace, foodList);
              }
            });
          }
        })
        .create();
    dialog.show();
  }

  private void addEntry(final Activity activity, DataEntry entry, DataEntry entryToReplace, final List<Food> foodList)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    FoodsDao foodsDao = AppDatabase.getInstance().foodsDao();
    if (entryToReplace != null)
    {
      dataEntriesDao.delete(entryToReplace);
    }
    dataEntriesDao.insert(entry);
    for (Food food : foodList)
    {
      foodsDao.insert(food);
    }
    activity.runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        updateUIWithNewEntry(activity);
        tryStartNewEntryBackup(activity);
      }
    });

    if (activity instanceof MainActivity)
    {
      EntryGraphFragment entryGraphFragment = ((MainActivity)activity).getFragment(EntryGraphFragment.class);
      entryGraphFragment.refreshGraph(false, true);
    }
  }

  private void tryStartNewEntryBackup(final Activity activity)
  {
    if (!RecoveryForegroundService.isDownloading())
    {
      Preference.get(activity,
                     new String[] {
                         "automatic_backup", "wifi_only_backup"
                     },
                     new String[] {
                         getString(R.string.automatic_backup_never_option), String.valueOf(true)
                     },
                     new Preference.ResultRunnable()
                     {
                       @Override
                       public void run()
                       {
                         String automaticBackup = getResults().get("automatic_backup");
                         String wifiOnlyBackup = getResults().get("wifi_only_backup");
                         if (automaticBackup != null && wifiOnlyBackup != null &&
                             automaticBackup.equals(getString(R.string.automatic_backup_after_new_entry_option)) &&
                             (!Boolean.parseBoolean(wifiOnlyBackup) || GoogleDriveInterface.hasWifiConnection(activity)) &&
                             GoogleSignIn.getLastSignedInAccount(activity) != null &&
                             !BackupForegroundService.isUploading() && !RecoveryForegroundService.isDownloading())
                         {
                           Intent intent = new Intent(activity, BackupForegroundService.class);
                           activity.startService(intent);
                         }
                       }
                     });
    }
  }

  private AutoCompleteTextView addNewListItemAutoCompleteTextView(Activity activity, LinearLayout owningLayout, @StringRes int hintResourceID, String tag, String text)
  {
    int padding = activity.getResources().getDimensionPixelSize(R.dimen.text_box_padding);
    AutoCompleteTextView newItem = new AutoCompleteTextView(activity);
    newItem.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    newItem.setHint(activity.getString(hintResourceID, owningLayout.getChildCount() + 1));
    newItem.setHintTextColor(((EditText)activity.findViewById(R.id.blood_glucose_level_input)).getHintTextColors()); // Stupid but necessary.
    newItem.setBackgroundResource(R.drawable.back);
    newItem.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    newItem.setImeOptions(EditorInfo.IME_ACTION_NEXT);
    newItem.setPadding(padding, 0, padding, 0);
    newItem.setFilters(new InputFilter[] { new InputFilter.LengthFilter(MAX_FOOD_EATEN_ITEM_LENGTH) });
    ListItemTextWatcher textWatcher = new ListItemTextWatcher(activity, owningLayout, tag, hintResourceID);
    newItem.addTextChangedListener(textWatcher);
    m_foodListTextWatchers.put(newItem, textWatcher);
    newItem.setTag(tag);
    newItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size));
    newItem.setOnEditorActionListener(new ListItemOnEditActionListener(owningLayout));
    CompositeOnFocusChangeListener.applyListenerToView(newItem, new HintHideOnFocusChangeListener(newItem, Gravity.START));
    CompositeOnFocusChangeListener.applyListenerToView(newItem,
                                                       new ListItemOnFocusChangeListener(owningLayout, hintResourceID));
    AutoCompleteTextViewUtilities.refreshAutoCompleteView(activity, newItem, null);
    if (text != null)
    {
      textWatcher.m_ignoreTextChanges = true;
      newItem.setText(text);
    }
    owningLayout.addView(newItem);

    TextView foodEatenLabel = activity.findViewById(R.id.food_eaten_label);
    if (foodEatenLabel != null)
    {
      foodEatenLabel.setLabelFor(newItem.getId());
    }

    return newItem;
  }

  private DataEntry findPreviousEntryWithFood(long before, String[] foodList)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    FoodsDao foodsDao = AppDatabase.getInstance().foodsDao();
    DataEntry entry = null;

    // Get the foods.
    List<Food> foods = new ArrayList<>();
    for (String foodName : foodList)
    {
      String foodSearch = foodName;
      if (foodName.endsWith("s"))
      {
        foodSearch = foodName.substring(0, foodName.length() - 1);
      }
      Food food = foodsDao.getFoodBefore(before, '%' + foodSearch + '%');
      if (food != null)
      {
        foods.add(food);
      }
    }

    if (foods.size() > 0)
    {
      // Find the most recent food.
      Food mostRecentFood = foods.get(0);
      for (int i = 1; i < foods.size(); i++)
      {
        if (foods.get(i).dataEntryTimestamp > mostRecentFood.dataEntryTimestamp)
        {
          mostRecentFood = foods.get(i);
        }
      }
      entry = dataEntriesDao.getEntry(mostRecentFood.dataEntryTimestamp);
    }

    return entry;
  }

  private DataEntry findFollowingEntryWithFood(long after, String[] foodList)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    FoodsDao foodsDao = AppDatabase.getInstance().foodsDao();
    DataEntry entry = null;

    // Get the foods.
    List<Food> foods = new ArrayList<>();
    for (String foodName : foodList)
    {
      String foodSearch = foodName;
      if (foodName.endsWith("s"))
      {
        foodSearch = foodName.substring(0, foodName.length() - 1);
      }
      Food food = foodsDao.getFoodAfter(after, '%' + foodSearch + '%');
      if (food != null)
      {
        foods.add(food);
      }
    }

    if (foods.size() > 0)
    {
      // Find the most recent food.
      Food mostRecentFood = foods.get(0);
      for (int i = 1; i < foods.size(); i++)
      {
        if (foods.get(i).dataEntryTimestamp < mostRecentFood.dataEntryTimestamp)
        {
          mostRecentFood = foods.get(i);
        }
      }
      entry = dataEntriesDao.getEntry(mostRecentFood.dataEntryTimestamp);
    }

    return entry;
  }

  private class ListItemTextWatcher implements TextWatcher
  {
    private final Activity m_activity;
    private final LinearLayout m_owningLayout;
    private final String m_newViewTag;
    private final @StringRes int m_hintResourceID;

    private boolean m_ignoreTextChanges = false;

    ListItemTextWatcher(Activity activity, LinearLayout owningLayout, String newViewTag, @StringRes int hintResourceID)
    {
      m_activity = activity;
      m_owningLayout = owningLayout;
      m_newViewTag = newViewTag;
      m_hintResourceID = hintResourceID;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

    @Override
    public void afterTextChanged(Editable editable)
    {
      if (m_ignoreTextChanges)
      {
        m_ignoreTextChanges = false;
      }
      else if (editable.length() > 0)
      {
        boolean addNew = true;
        AutoCompleteTextView lastItem = null;
        for (int i = 0; i < m_owningLayout.getChildCount() && addNew; i++)
        {
          lastItem = (AutoCompleteTextView)m_owningLayout.getChildAt(i);
          if (lastItem.getText().length() == 0)
          {
            addNew = false;
          }
        }

        if (addNew && lastItem != null)
        {
          AutoCompleteTextView newItem = addNewListItemAutoCompleteTextView(
              m_activity, m_owningLayout, m_hintResourceID, m_newViewTag, null);
          lastItem.setNextFocusForwardId(newItem.getId());

          NestedScrollView scrollView = m_activity.findViewById(R.id.new_entry_root);
          if (scrollView != null)
          {
            scrollView.scrollBy(0, lastItem.getHeight());
          }
        }
      }
    }
  }

  private static class ListItemOnFocusChangeListener implements View.OnFocusChangeListener
  {
    private final LinearLayout m_owningLayout;
    private final @StringRes int m_hintResourceID;

    ListItemOnFocusChangeListener(LinearLayout owningLayout, @StringRes int hintResourceID)
    {
      m_owningLayout = owningLayout;
      m_hintResourceID = hintResourceID;
    }

    @Override
    public void onFocusChange(final View view, boolean hasFocus)
    {
      if (!hasFocus && ((AutoCompleteTextView)view).getText().length() == 0)
      {
        int offset = 0;
        for (int i = 0; i < m_owningLayout.getChildCount(); i++)
        {
          AutoCompleteTextView inputView = (AutoCompleteTextView)m_owningLayout.getChildAt(i);
          if (inputView.getText().length() == 0 && i < m_owningLayout.getChildCount() - 1)
          {
            offset++;
            new Handler().post(new Runnable() // Must be delayed otherwise there are weird layout bugs.
            {
              @Override
              public void run()
              {
                m_owningLayout.removeView(view);
              }
            });
          }
          else if (offset > 0)
          {
            String newHint = view.getContext().getString(m_hintResourceID, i - offset + 1);
            inputView.setHint(newHint);
            CompositeOnFocusChangeListener compositeOnFocusChangeListener = (CompositeOnFocusChangeListener)inputView.getOnFocusChangeListener();
            HintHideOnFocusChangeListener hintHideOnFocusChangeListener = compositeOnFocusChangeListener.getInstance(HintHideOnFocusChangeListener.class);
            hintHideOnFocusChangeListener.changeOriginalHint(newHint);
          }
        }
      }
    }
  }

  private static class ListItemOnEditActionListener implements TextView.OnEditorActionListener
  {
    private final LinearLayout m_owningLayout;

    ListItemOnEditActionListener(LinearLayout owningLayout)
    {
      m_owningLayout = owningLayout;
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent)
    {
      boolean actionHandled = false;
      if (actionID == EditorInfo.IME_ACTION_NEXT &&
          textView.getText().length() == 0 &&
          m_owningLayout.getChildAt(m_owningLayout.getChildCount() - 1) == textView)
      {
        textView.onEditorAction(EditorInfo.IME_ACTION_DONE);
        actionHandled = true;
      }
      return actionHandled;
    }
  }
}
