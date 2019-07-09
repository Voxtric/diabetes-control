package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Event;
import voxtric.com.diabetescontrol.database.EventsDao;

public class EditEventsActivity extends DatabaseActivity
{
  private static final int MAX_EVENT_COUNT = 8;

  private EditEventsRecyclerViewAdapter m_adapter = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_events);

    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    final RecyclerView recyclerView = findViewById(R.id.recycler_view_entry_list);
    recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final List<Event> events = m_database.eventsDao().getEvents();
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            m_adapter = new EditEventsRecyclerViewAdapter(events, EditEventsActivity.this);
            recyclerView.setAdapter(m_adapter);
            findViewById(R.id.button_add_new_event).setEnabled(events.size() < MAX_EVENT_COUNT);
            if (actionBar != null)
            {
              actionBar.setTitle(getString(R.string.edit_events_name, events.size(), MAX_EVENT_COUNT));
            }
          }
        });
      }
    });
  }

  private void moveEvent(final View dataView, int direction)
  {
    final Event eventA = m_adapter.getEvent(dataView);
    final Event eventB = m_adapter.getEvent(eventA.order + direction);
    int tempOrder = eventA.order;
    eventA.order = eventB.order;
    eventB.order = tempOrder;
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        m_database.eventsDao().updateEvent(eventA);
        m_database.eventsDao().updateEvent(eventB);
        final List<Event> allEvents = m_database.eventsDao().getEvents();

        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            m_adapter.updateAllEvents(allEvents);
          }
        });
      }
    });
  }

  public void editEventName(final View dataView, final boolean newEvent)
  {
    final Event event = m_adapter.getEvent(dataView);
    final View view = View.inflate(this, R.layout.dialog_edit_event_name, null);
    final EditText input = view.findViewById(R.id.edit_text_event_name);
    input.append(event.name);

    final DialogInterfaceOnDismissListener onDismissListener = new DialogInterfaceOnDismissListener(dataView, event);
    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_edit_event_name)
        .setView(view)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            onDismissListener.keepEditedEvent = true;
            String newEventName = input.getText().toString();
            if (!newEventName.equals(event.name))
            {
              event.name = newEventName;
              m_adapter.updateEvent(dataView, event, false);
              setResult(MainActivity.RESULT_EVENTS_CHANGED);
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  m_database.eventsDao().updateEvent(event);
                  if (!newEvent)
                  {
                    displayMessage(R.string.event_name_changed_message);
                  }
                }
              });
            }
          }
        })
        .create();
    dialog.show();

    // Ensure that saving isn't possible if the edit text is blank.
    input.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void afterTextChanged(Editable s) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(s.length() > 0);
      }
    });

    if (newEvent)
    {
      dialog.setOnDismissListener(onDismissListener);
    }
  }

  public void editEventTime(final View dataView, final boolean newEvent)
  {
    final Event event = m_adapter.getEvent(dataView);
    final TimePicker timePicker = new TimePicker(this);
    final Calendar calendar = Calendar.getInstance();
    if (event.timeInDay != -1)
    {
      calendar.setTimeInMillis(event.timeInDay);
    }
    else
    {
      calendar.clear();
      calendar.set(
          calendar.getMinimum(Calendar.YEAR),
          calendar.getMinimum(Calendar.MONTH),
          calendar.getMinimum(Calendar.DATE),
          calendar.getMinimum(Calendar.HOUR_OF_DAY),
          calendar.getMinimum(Calendar.MINUTE),
          calendar.getMinimum(Calendar.SECOND));
    }
    timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
    timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));

    final DialogInterfaceOnDismissListener onDismissListener = new DialogInterfaceOnDismissListener(dataView, event);
    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setView(timePicker)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            onDismissListener.keepEditedEvent = true;
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
            calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
            if (calendar.getTimeInMillis() != event.timeInDay)
            {
              event.timeInDay = calendar.getTimeInMillis();
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  if (!newEvent)
                  {
                    m_database.eventsDao().updateEvent(event);
                    displayMessage(R.string.event_time_changed_message);
                  }
                  else
                  {
                    List<Event> allEvents = m_database.eventsDao().getEventsTimeOrdered();
                    int index = 0;
                    while (index < allEvents.size() && event.timeInDay > allEvents.get(index).timeInDay)
                    {
                      index++;
                    }
                    int order = allEvents.get(index).order;
                    m_database.eventsDao().shuffleOrders(1, order - 1);
                    event.order = order;
                    m_database.eventsDao().updateEvent(event);

                    displayMessage(R.string.new_event_added_message);
                  }

                  final List<Event> allEvents = m_database.eventsDao().getEvents();
                  runOnUiThread(new Runnable()
                  {
                    @Override
                    public void run()
                    {
                      m_adapter.updateAllEvents(allEvents);
                      setResult(MainActivity.RESULT_EVENTS_CHANGED);
                    }
                  });
                }
              });
            }
          }
        })
        .create();
    dialog.show();

    // Ensure time picker displays correctly.
    Point point = new Point();
    getWindowManager().getDefaultDisplay().getSize(point);
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

    if (newEvent)
    {
      dialog.setOnDismissListener(onDismissListener);
    }
  }

  private void deleteEvent(final View dataView)
  {
    if (m_adapter.getItemCount() == 1)
    {
      Toast.makeText(this, R.string.event_delete_final_message, Toast.LENGTH_LONG).show();
    }
    else
    {
      final Event event = m_adapter.getEvent(dataView);
      AlertDialog dialog = new AlertDialog.Builder(this)
          .setTitle(R.string.title_delete_event)
          .setMessage(getString(R.string.message_delete_event, event.name))
          .setNegativeButton(R.string.cancel, null)
          .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              m_adapter.deleteEvent(dataView);
              findViewById(R.id.button_add_new_event).setEnabled(m_adapter.getItemCount() < MAX_EVENT_COUNT);
              ActionBar actionBar = getSupportActionBar();
              if (actionBar != null)
              {
                actionBar.setTitle(getString(R.string.edit_events_name, m_adapter.getItemCount(), MAX_EVENT_COUNT));
              }
              setResult(MainActivity.RESULT_EVENTS_CHANGED);
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  m_database.eventsDao().deleteEvent(event);
                  m_database.eventsDao().shuffleOrders(-1, event.order);
                  displayMessage(R.string.event_deleted_message);
                }
              });
            }
          })
          .create();
      dialog.show();
    }
  }

  public void openEventMoreMenu(View view)
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

    PopupMenu menu = new PopupMenu(view.getContext(), view);
    menu.getMenuInflater().inflate(R.menu.event_more, menu.getMenu());
    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
    {
      @Override
      public boolean onMenuItemClick(MenuItem item)
      {
        switch (item.getItemId())
        {
          case R.id.action_move_up:
            moveEvent(dataView, -1);
            return true;
          case R.id.action_move_down:
            moveEvent(dataView, 1);
            return true;
          case R.id.navigation_edit_name:
            editEventName(dataView, false);
            return true;
          case R.id.navigation_edit_time:
            editEventTime(dataView, false);
            return true;
          case R.id.navigation_delete:
            deleteEvent(dataView);
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
    Event event = m_adapter.getEvent(dataView);
    if (event.order <= 0)
    {
      menu.getMenu().findItem(R.id.action_move_up).setEnabled(false);
    }
    else if (event.order >= m_adapter.getItemCount() - 1)
    {
      menu.getMenu().findItem(R.id.action_move_down).setEnabled(false);
    }
  }

  public void resetEvents(View view)
  {
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_reset_events)
        .setMessage(R.string.message_reset_events)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.reset, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialogInterface, int i)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                EventsDao eventsDao = m_database.eventsDao();
                List<Event> events = eventsDao.getEvents();
                for (Event event : events)
                {
                  eventsDao.deleteEvent(event);
                }
                final List<Event> nhsEvents = addNHSEvents(m_database, EditEventsActivity.this);

                runOnUiThread(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    m_adapter = new EditEventsRecyclerViewAdapter(nhsEvents, EditEventsActivity.this);
                    RecyclerView recyclerView = findViewById(R.id.recycler_view_entry_list);
                    recyclerView.setAdapter(m_adapter);
                    Toast.makeText(EditEventsActivity.this, R.string.events_reset_message, Toast.LENGTH_LONG).show();
                  }
                });
              }
            });
          }
        })
        .create();
    dialog.show();
  }

  public void addNewEvent(View view)
  {
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final Event event = new Event();
        event.name = "";
        event.timeInDay = -1L;
        event.order = m_adapter.getItemCount();
        m_database.eventsDao().insert(event);
        final List<Event> events = m_database.eventsDao().getEvents();
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            m_adapter = new EditEventsRecyclerViewAdapter(events, EditEventsActivity.this);
            RecyclerView recyclerView = findViewById(R.id.recycler_view_entry_list);
            recyclerView.setAdapter(m_adapter);
            findViewById(R.id.button_add_new_event).setEnabled(events.size() < MAX_EVENT_COUNT);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
            {
              actionBar.setTitle(getString(R.string.edit_events_name, events.size(), MAX_EVENT_COUNT));
            }
            setResult(MainActivity.RESULT_EVENTS_CHANGED);
          }
        });
      }
    });
  }

  private void displayMessage(final @StringRes int messageStringID)
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        Toast.makeText(EditEventsActivity.this, messageStringID, Toast.LENGTH_LONG).show();
      }
    });
  }

  public static List<Event> addNHSEvents(AppDatabase database, Activity activity)
  {
    String[] nhsEventNames = activity.getResources().getStringArray(R.array.nhs_event_names);
    EventsDao eventsDao = database.eventsDao();
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(
        calendar.getMinimum(Calendar.YEAR),
        calendar.getMinimum(Calendar.MONTH),
        calendar.getMinimum(Calendar.DATE),
        calendar.getMinimum(Calendar.HOUR_OF_DAY),
        calendar.getMinimum(Calendar.MINUTE),
        calendar.getMinimum(Calendar.SECOND));

    {
      calendar.set(Calendar.HOUR_OF_DAY, 8);
      Event beforeBreakfastEvent = new Event();
      beforeBreakfastEvent.name = nhsEventNames[0];
      beforeBreakfastEvent.timeInDay = calendar.getTimeInMillis();
      beforeBreakfastEvent.order = 0;
      eventsDao.insert(beforeBreakfastEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 10);
      Event twoHoursAfterBreakfastEvent = new Event();
      twoHoursAfterBreakfastEvent.name = nhsEventNames[1];
      twoHoursAfterBreakfastEvent.timeInDay = calendar.getTimeInMillis();
      twoHoursAfterBreakfastEvent.order = 1;
      eventsDao.insert(twoHoursAfterBreakfastEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 13);
      Event beforeMiddayMealEvent = new Event();
      beforeMiddayMealEvent.name = nhsEventNames[2];
      beforeMiddayMealEvent.timeInDay = calendar.getTimeInMillis();
      beforeMiddayMealEvent.order = 2;
      eventsDao.insert(beforeMiddayMealEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 15);
      Event twoHoursAfterMiddayMealEvent = new Event();
      twoHoursAfterMiddayMealEvent.name = nhsEventNames[3];
      twoHoursAfterMiddayMealEvent.timeInDay = calendar.getTimeInMillis();
      twoHoursAfterMiddayMealEvent.order = 3;
      eventsDao.insert(twoHoursAfterMiddayMealEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 18);
      Event beforeEveningMealEvent = new Event();
      beforeEveningMealEvent.name = nhsEventNames[4];
      beforeEveningMealEvent.timeInDay = calendar.getTimeInMillis();
      beforeEveningMealEvent.order = 4;
      eventsDao.insert(beforeEveningMealEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 20);
      Event twoHoursAfterEveningMealEvent = new Event();
      twoHoursAfterEveningMealEvent.name = nhsEventNames[5];
      twoHoursAfterEveningMealEvent.timeInDay = calendar.getTimeInMillis();
      twoHoursAfterEveningMealEvent.order = 5;
      eventsDao.insert(twoHoursAfterEveningMealEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 22);
      Event beforeBedEvent = new Event();
      beforeBedEvent.name = nhsEventNames[6];
      beforeBedEvent.timeInDay = calendar.getTimeInMillis();
      beforeBedEvent.order = 6;
      eventsDao.insert(beforeBedEvent);
    }

    {
      calendar.set(Calendar.HOUR_OF_DAY, 2);
      Event duringNightEvent = new Event();
      duringNightEvent.name = nhsEventNames[7];
      duringNightEvent.timeInDay = calendar.getTimeInMillis();
      duringNightEvent.order = 7;
      eventsDao.insert(duringNightEvent);
    }

    return eventsDao.getEvents();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      finish();
      return true;
    }
    return false;
  }

  class DialogInterfaceOnDismissListener implements DialogInterface.OnDismissListener
  {
    boolean keepEditedEvent = false;

    private View dataView;
    private Event event;

    DialogInterfaceOnDismissListener(View dataView, Event event)
    {
      this.dataView = dataView;
      this.event = event;
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
      if (!keepEditedEvent)
      {
        m_adapter.deleteEvent(dataView);
        findViewById(R.id.button_add_new_event).setEnabled(m_adapter.getItemCount() < MAX_EVENT_COUNT);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
          actionBar.setTitle(getString(R.string.edit_events_name, m_adapter.getItemCount(), MAX_EVENT_COUNT));
        }
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {
            m_database.eventsDao().deleteEvent(event);
            displayMessage(R.string.new_event_cancelled_message);
          }
        });
      }
    }
  }
}
