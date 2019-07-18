package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.shuhart.bubblepagerindicator.BubblePageIndicator;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntriesDao;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Event;
import voxtric.com.diabetescontrol.database.EventsDao;
import voxtric.com.diabetescontrol.settings.EditEventsActivity;
import voxtric.com.diabetescontrol.utilities.AutoCompleteTextViewUtilities;
import voxtric.com.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import voxtric.com.diabetescontrol.utilities.DecimalDigitsInputFilter;
import voxtric.com.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class NewEntryFragment extends Fragment
{
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
  private AppDatabase m_database = null;
  private ArrayAdapter<String> m_eventSpinnerAdapter = null;

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

      EditText bglInput = activity.findViewById(R.id.edit_text_blood_glucose_level);
      CompositeOnFocusChangeListener.applyListenerToView(bglInput, new HintHideOnFocusChangeListener(bglInput, Gravity.CENTER));

      AutoCompleteTextView insulinNameInput = activity.findViewById(R.id.auto_complete_insulin_name);
      CompositeOnFocusChangeListener.applyListenerToView(insulinNameInput, new HintHideOnFocusChangeListener(insulinNameInput, Gravity.CENTER));

      EditText insulinDoseInput = activity.findViewById(R.id.edit_text_insulin_dose);
      CompositeOnFocusChangeListener.applyListenerToView(insulinDoseInput, new HintHideOnFocusChangeListener(insulinDoseInput, Gravity.CENTER));

      AutoCompleteTextView foodEatenItemInput = activity.findViewById(R.id.food_eaten_item);
      CompositeOnFocusChangeListener.applyListenerToView(foodEatenItemInput, new HintHideOnFocusChangeListener(foodEatenItemInput, Gravity.START));

      AutoCompleteTextView additionalNotesInput = activity.findViewById(R.id.auto_complete_additional_notes);
      CompositeOnFocusChangeListener.applyListenerToView(additionalNotesInput, new HintHideOnFocusChangeListener(additionalNotesInput, Gravity.START | Gravity.TOP));

      ((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).setFilters(
          new InputFilter[] { new DecimalDigitsInputFilter(2, 1) });

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_MONTH, -5);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, (AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name), calendar.getTimeInMillis());
      calendar.add(Calendar.DAY_OF_MONTH, 5);
      calendar.add(Calendar.MONTH, -2);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, (AutoCompleteTextView)activity.findViewById(R.id.food_eaten_item), calendar.getTimeInMillis());
      calendar.add(Calendar.MONTH, 1);
      AutoCompleteTextViewUtilities.clearAgedValuesAutoCompleteValues(activity, (AutoCompleteTextView)activity.findViewById(R.id.auto_complete_additional_notes), calendar.getTimeInMillis());

      LinearLayout foodEatenItemList = activity.findViewById(R.id.food_eaten_items);
      foodEatenItemInput.setHint(getString(R.string.food_item_hint, 1));
      foodEatenItemInput.addTextChangedListener(new ListItemTextWatcher(activity, foodEatenItemList, "food_item", R.string.food_item_hint));
      CompositeOnFocusChangeListener.applyListenerToView(foodEatenItemInput, new ListItemOnFocusChangeListener(foodEatenItemList, R.string.food_item_hint));
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
      m_currentEventName = savedInstanceState.getString("current_event_name");
      updateDateTime(false);
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

  void updateEventSpinner()
  {
    final DatabaseActivity activity = (DatabaseActivity)getActivity();
    if (activity != null)
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          final List<Event> events = m_database.eventsDao().getEvents();
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
          Calendar calendar = Calendar.getInstance();
          calendar.clear();
          calendar.set(
              calendar.getMinimum(Calendar.YEAR),
              calendar.getMinimum(Calendar.MONTH),
              calendar.getMinimum(Calendar.DATE),
              m_hour, m_minute,
              calendar.getMinimum(Calendar.SECOND));
          long timeOnlyTimeStamp = calendar.getTimeInMillis();

          final List<Event> events = m_database.eventsDao().getEventsTimeOrdered();
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
      Spinner eventSpinner = activity.findViewById(R.id.spinner_event);
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
                if (m_currentEventName == null || m_eventNameAutoSelected)
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
          if (((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_insulin_name).setEnabled(false);
            totalActiveRadioButtons--;
          }
          if (((EditText)activity.findViewById(R.id.edit_text_insulin_dose)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_insulin_dose).setEnabled(false);
            totalActiveRadioButtons--;
          }
          if (((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().length() == 0)
          {
            layout.findViewById(R.id.radio_blood_glucose_level).setEnabled(false);
            totalActiveRadioButtons--;
          }
          if (((AutoCompleteTextView)((LinearLayout)activity.findViewById(R.id.food_eaten_items)).getChildAt(0)).getText().length() == 0)
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
        final Activity activity = getActivity();
        if (activity instanceof DatabaseActivity)
        {
          AutoCompleteTextViewUtilities.saveAutoCompleteView(activity, (AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name));
          AutoCompleteTextViewUtilities.saveAutoCompleteView(activity, (AutoCompleteTextView)activity.findViewById(R.id.auto_complete_additional_notes));

          boolean proceed = true;

          String bloodGlucoseLevel = ((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString();
          String insulinName = ((EditText)activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString();
          String insulinDose = ((EditText)activity.findViewById(R.id.edit_text_insulin_dose)).getText().toString();
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
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                checkDateMismatch((DatabaseActivity)activity, entry, null);
              }
            });
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
            timeStamp, '%' + ((EditText) activity.findViewById(R.id.edit_text_insulin_dose)).getText().toString() + '%');
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
      {
        entries = m_database.dataEntriesDao().findPreviousEntryWithBloodGlucoseLevel(
            timeStamp, Float.valueOf(((EditText) activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
      {
        // TODO: Do something here.
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
            timeStamp, '%' + ((EditText) activity.findViewById(R.id.edit_text_insulin_dose)).getText().toString() + '%');
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_blood_glucose_level).getId())
      {
        entries = m_database.dataEntriesDao().findNextEntryWithBloodGlucoseLevel(
            timeStamp, Float.valueOf(((EditText) activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString()));
      }
      else if (radioGroupButtonID == criteriaLayout.findViewById(R.id.radio_food_eaten).getId())
      {
        // TODO: Do something here.
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
      DataEntry surrounding = m_database.dataEntriesDao().findFirstAfter(entries.get(0).actualTimestamp);
      if (surrounding != null)
      {
        entries.add(surrounding);
      }
      surrounding = m_database.dataEntriesDao().findFirstBefore(entries.get(0).actualTimestamp);
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

  DataEntry createEntry(Activity activity)
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
    entry.event = ((Spinner)activity.findViewById(R.id.spinner_event)).getSelectedItem().toString();
    entry.insulinName = ((EditText)activity.findViewById(R.id.auto_complete_insulin_name)).getText().toString();
    entry.insulinDose = ((EditText)activity.findViewById(R.id.edit_text_insulin_dose)).getText().toString();
    entry.bloodGlucoseLevel = Float.parseFloat(((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).getText().toString());
    // TODO: Set food eaten.
    entry.foodEaten = "";
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

      activity.findViewById(R.id.grid_layout).requestFocus();

      clearText((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level));
      clearText((EditText)activity.findViewById(R.id.auto_complete_insulin_name));
      clearText((EditText)activity.findViewById(R.id.edit_text_insulin_dose));
      // TODO: Clear food eaten text boxes.
      clearText((EditText)activity.findViewById(R.id.auto_complete_additional_notes));

      m_currentEventName = null;
      m_eventNameAutoSelected = true;
      pickBestEvent();
    }
  }

  void setValues(DataEntry entry, Activity activity)
  {
    m_currentEventName = entry.event;
    updateEventSpinner();

    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.actualTimestamp);
    m_year = calendar.get(Calendar.YEAR);
    m_month = calendar.get(Calendar.MONTH);
    m_day = calendar.get(Calendar.DAY_OF_MONTH);
    m_hour = calendar.get(Calendar.HOUR_OF_DAY);
    m_minute = calendar.get(Calendar.MINUTE);
    updateDateTime(false);

    ((EditText)activity.findViewById(R.id.edit_text_blood_glucose_level)).setText(String.valueOf(entry.bloodGlucoseLevel));
    ((AutoCompleteTextView)activity.findViewById(R.id.auto_complete_insulin_name)).setText(entry.insulinName);
    ((EditText)activity.findViewById(R.id.edit_text_insulin_dose)).setText(entry.insulinDose);
    // TODO: Set food eaten text boxes.
    if (entry.additionalNotes.length() > 0)
    {
      AutoCompleteTextView additionalNotes = activity.findViewById(R.id.auto_complete_additional_notes);
      additionalNotes.setText(entry.additionalNotes);
      additionalNotes.setGravity(Gravity.START | Gravity.TOP);
    }
  }

  void checkDateMismatch(final DatabaseActivity activity, final DataEntry entry, final DataEntry entryToReplace)
  {
    AppDatabase database = activity.getDatabase();
    DataEntriesDao dataEntriesDao = database.dataEntriesDao();
    EventsDao eventsDao = database.eventsDao();

    Event event = eventsDao.getEvent(entry.event);

    boolean possibilityA = false;
    DataEntry previousEntry = dataEntriesDao.findFirstBefore(entry.actualTimestamp);
    if (previousEntry != null)
    {
      Event previousEvent = eventsDao.getEvent(previousEntry.event);
      possibilityA = event.order > previousEvent.order && entry.dayTimeStamp > previousEntry.dayTimeStamp;
    }

    Event firstEvent = eventsDao.getEvents().get(0);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.actualTimestamp);
    calendar.set(
        calendar.getMinimum(Calendar.YEAR),
        calendar.getMinimum(Calendar.MONTH),
        calendar.getMinimum(Calendar.DATE));
    boolean possibilityB = event.id != firstEvent.id && firstEvent.timeInDay > calendar.getTimeInMillis();

    if (possibilityA || possibilityB)
    {
      activity.runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          queryDateMismatch(activity, entry, entryToReplace);
        }
      });
    }
    else
    {
      checkEventOverlap(activity, entry, entryToReplace);
    }
  }

  private void queryDateMismatch(final DatabaseActivity activity, final DataEntry entry, final DataEntry entryToReplace)
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
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
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
                checkEventOverlap(activity, entry, entryToReplace);
              }
            });
          }
        })
        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialogInterface, int i)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                checkEventOverlap(activity, entry, entryToReplace);
              }
            });
          }
        })
        .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener()
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

  private void checkEventOverlap(final DatabaseActivity activity, final DataEntry entry, final DataEntry entryToReplace)
  {
    final DataEntry overlappingEntry = activity.getDatabase().dataEntriesDao().findOverlapping(entry.dayTimeStamp, entry.event);
    if (entryToReplace != null)
    {
      if (overlappingEntry == null || overlappingEntry.actualTimestamp == entryToReplace.actualTimestamp)
      {
        addEntry(activity, entry, entryToReplace);
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
                .setMessage(R.string.message_event_collision)
                .setPositiveButton(R.string.ok, null)
                .create();
            dialog.show();
          }
        });
      }
    }
    else if (overlappingEntry == null)
    {
      addEntry(activity, entry, null);
    }
    else
    {
      activity.runOnUiThread(new Runnable()
      {
        @Override
        public void run()
        {
          queryEventOverlap(activity, entry, overlappingEntry);
        }
      });
    }
  }

  private void queryEventOverlap(final DatabaseActivity activity, final DataEntry entry, final DataEntry entryToReplace)
  {
    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity)
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
                addEntry(activity, entry, entryToReplace);
              }
            });
          }
        })
        .create();
    dialog.show();
  }

  private void addEntry(final DatabaseActivity activity, DataEntry entry, DataEntry entryToReplace)
  {
    DataEntriesDao dataEntriesDao = activity.getDatabase().dataEntriesDao();
    if (entryToReplace != null)
    {
      dataEntriesDao.delete(entryToReplace);
    }
    dataEntriesDao.insert(entry);
    activity.runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        updateUIWithNewEntry(activity);
      }
    });
  }

  private class ListItemTextWatcher implements TextWatcher
  {
    private final Activity m_activity;
    private final LinearLayout m_owningLayout;
    private final String m_newViewTag;
    private final @StringRes int m_hintResourceID;

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
      if (editable.length() > 0)
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
          int padding = m_owningLayout.getResources().getDimensionPixelSize(R.dimen.text_box_padding);
          AutoCompleteTextView newItem = new AutoCompleteTextView(m_owningLayout.getContext());
          newItem.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
          newItem.setHint(getString(m_hintResourceID, m_owningLayout.getChildCount() + 1));
          newItem.setBackgroundResource(R.drawable.back);
          newItem.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
          newItem.setImeOptions(EditorInfo.IME_ACTION_DONE);
          newItem.setPadding(padding, 0, padding, 0);
          newItem.addTextChangedListener(new ListItemTextWatcher(m_activity, m_owningLayout, m_newViewTag, m_hintResourceID));
          newItem.setTag(m_newViewTag);
          CompositeOnFocusChangeListener.applyListenerToView(newItem, new HintHideOnFocusChangeListener(newItem, Gravity.START));
          CompositeOnFocusChangeListener.applyListenerToView(newItem, new ListItemOnFocusChangeListener(m_owningLayout, m_hintResourceID));
          AutoCompleteTextViewUtilities.refreshAutoCompleteView(m_activity, newItem, null);

          m_owningLayout.addView(newItem);
          lastItem.setImeOptions(EditorInfo.IME_ACTION_NEXT);
          lastItem.setNextFocusForwardId(newItem.getId());
          newItem.requestFocus();
          final AutoCompleteTextView finalLastItem = lastItem;
          new Handler().post(new Runnable()
          {
            @Override
            public void run()
            {
              finalLastItem.requestFocus();
            }
          });
        }
      }
    }
  }

  private class ListItemOnFocusChangeListener implements View.OnFocusChangeListener
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
            new Handler().post(new Runnable()
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
            String newHint = getString(m_hintResourceID, i - offset + 1);
            inputView.setHint(newHint);
            CompositeOnFocusChangeListener compositeOnFocusChangeListener = (CompositeOnFocusChangeListener)inputView.getOnFocusChangeListener();
            HintHideOnFocusChangeListener hintHideOnFocusChangeListener = compositeOnFocusChangeListener.getInstance(HintHideOnFocusChangeListener.class);
            hintHideOnFocusChangeListener.changeOriginalHint(newHint);
          }
        }
      }
    }
  }
}
