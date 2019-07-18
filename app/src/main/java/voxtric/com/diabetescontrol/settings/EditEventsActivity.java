package voxtric.com.diabetescontrol.settings;

import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Event;
import voxtric.com.diabetescontrol.database.EventsDao;
import voxtric.com.diabetescontrol.settings.fragments.EventsSettingsFragment;
import voxtric.com.diabetescontrol.utilities.CompositeOnFocusChangeListener;
import voxtric.com.diabetescontrol.utilities.HintHideOnFocusChangeListener;

public class EditEventsActivity extends DatabaseActivity
{
  private static final int MAX_EVENT_COUNT = 8;

  private EditEventsRecyclerViewAdapter m_adapter = null;
  private boolean m_popupOptionClicked = false;

  private boolean m_eventMoving = false;

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

    final RecyclerView recyclerView = findViewById(R.id.recycler_view_event_list);
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

  @Override
  public boolean dispatchTouchEvent(MotionEvent event)
  {
    LinearLayout movementButtons = m_adapter.getActiveMovementButtons();
    if (movementButtons != null)
    {
      Rect viewRect = new Rect();
      movementButtons.getGlobalVisibleRect(viewRect);
      if (!m_eventMoving && !viewRect.contains((int)event.getRawX(), (int)event.getRawY()))
      {
        highlightSelected(null);
        LinearLayout activeMovementButtons = m_adapter.getActiveMovementButtons();
        setMoveButtonVisible(getDataView(activeMovementButtons), false);
        return true;
      }
    }
    return super.dispatchTouchEvent(event);
  }

  ViewGroup getDataView(View view)
  {
    ViewGroup dataView = null;
    View priorView = view;
    View parentView = (View)view.getParent();
    while (dataView == null)
    {
      if (parentView instanceof RecyclerView)
      {
        dataView = (ViewGroup)priorView;
      }
      else
      {
        priorView = parentView;
        parentView = (View)parentView.getParent();
      }
    }
    return dataView;
  }

  public void moveEvent(final View view)
  {
    final ViewGroup dataView = getDataView(view);
    final int direction = view.getId() == R.id.button_down ? 1 : -1;
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
            final long ANIMATION_DURATION = 200;

            View swappingView = null;
            RecyclerView recyclerView = findViewById(R.id.recycler_view_event_list);
            for (int i = 0; i < recyclerView.getChildCount(); i++)
            {
              if (recyclerView.getChildAt(i) == dataView)
              {
                swappingView = recyclerView.getChildAt(i + direction);
                break;
              }
            }

            if (swappingView == null)
            {
              m_adapter.updateAllEvents(allEvents, eventA, true);
            }
            else
            {
              m_eventMoving = true;
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
              {
                dataView.setZ(1.0f);
              }
              else
              {
                dataView.bringToFront();
                recyclerView.requestLayout();
                recyclerView.invalidate();
              }
              dataView.animate().translationYBy(swappingView.getHeight() * direction).setDuration(ANIMATION_DURATION).start();
              swappingView.animate().translationYBy(dataView.getHeight() * -direction).setDuration(ANIMATION_DURATION).start();

              final View finalSwappingView = swappingView;
              new Handler().postDelayed(new Runnable()
              {
                @Override
                public void run()
                {
                  dataView.animate().translationY(0.0f).setDuration(0).start();
                  finalSwappingView.animate().translationY(0.0f).setDuration(0).start();
                  m_eventMoving = false;

                  m_adapter.updateAllEvents(allEvents, eventA, true);
                }
              }, ANIMATION_DURATION);
            }
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
    CompositeOnFocusChangeListener.applyListenerToView(input, new HintHideOnFocusChangeListener(input, Gravity.CENTER));
    input.append(event.name);

    final DialogInterfaceOnDismissListener onDismissListener = new DialogInterfaceOnDismissListener(event, newEvent);
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
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  EventsDao eventsDao = m_database.eventsDao();
                  eventsDao.updateEvent(event);
                  final List<Event> allEvents = eventsDao.getEvents();
                  runOnUiThread(new Runnable()
                  {
                    @Override
                    public void run()
                    {
                      m_adapter.updateAllEvents(allEvents, event, false);
                      setResult(EventsSettingsFragment.RESULT_UPDATE_EVENTS);
                    }
                  });
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
      public void onTextChanged(final CharSequence text, int start, int before, int count)
      {
        boolean enableButton = text.length() > 0;
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enableButton);
        if (enableButton)
        {
          AsyncTask.execute(new Runnable()
          {
            @Override
            public void run()
            {
              EventsDao eventsDao = m_database.eventsDao();
              final boolean asyncEnableButton = eventsDao.getEvent(text.toString()) == null;
              runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(asyncEnableButton);
                }
              });
            }
          });
        }
      }
    });

    dialog.setOnDismissListener(onDismissListener);
  }

  public void editEventTime(final View dataView, final boolean newEvent)
  {
    final Event event = m_adapter.getEvent(dataView);

    if (newEvent)
    {
      highlightSelected(dataView);
    }

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

    final DialogInterfaceOnDismissListener onDismissListener = new DialogInterfaceOnDismissListener(event, newEvent);
    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setView(timePicker)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            onDismissListener.keepEditedEvent = true;
            calendar.clear();
            calendar.set(
                calendar.getMinimum(Calendar.YEAR),
                calendar.getMinimum(Calendar.MONTH),
                calendar.getMinimum(Calendar.DATE),
                timePicker.getCurrentHour(),
                timePicker.getCurrentMinute(),
                calendar.getMinimum(Calendar.SECOND));
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
                    Log.e(String.valueOf(event.timeInDay), String.valueOf(allEvents.get(index).timeInDay));
                    index++;
                  }
                  if (index == allEvents.size())
                  {
                    event.order = allEvents.size();
                  }
                  else
                  {
                    event.order = allEvents.get(index).order;
                    m_database.eventsDao().shuffleOrders(1, event.order - 1);
                  }
                  m_database.eventsDao().updateEvent(event);

                  displayMessage(R.string.new_event_added_message);
                }

                final List<Event> allEvents = m_database.eventsDao().getEvents();
                runOnUiThread(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    m_adapter.updateAllEvents(allEvents, null, false);
                    setResult(EventsSettingsFragment.RESULT_UPDATE_EVENTS);
                  }
                });
              }
            });
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

    timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
    {
      @Override
      public void onTimeChanged(TimePicker tp, int i, int i1)
      {
        final Calendar validateCalendar = Calendar.getInstance();
        validateCalendar.clear();
        validateCalendar.set(
            validateCalendar.getMinimum(Calendar.YEAR),
            validateCalendar.getMinimum(Calendar.MONTH),
            validateCalendar.getMinimum(Calendar.DATE),
            timePicker.getCurrentHour(),
            timePicker.getCurrentMinute(),
            validateCalendar.getMinimum(Calendar.SECOND));
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {

            EventsDao eventsDao = m_database.eventsDao();
            final boolean enableButton = eventsDao.getEvent(validateCalendar.getTimeInMillis() - 999, validateCalendar.getTimeInMillis() + 999) == null;
            runOnUiThread(new Runnable()
            {
              @Override
              public void run()
              {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enableButton);
              }
            });
          }
        });
      }
    });

    dialog.setOnDismissListener(onDismissListener);
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
              AsyncTask.execute(new Runnable()
              {
                @Override
                public void run()
                {
                  EventsDao eventsDao = m_database.eventsDao();
                  eventsDao.deleteEvent(event);
                  eventsDao.shuffleOrders(-1, event.order);
                  final List<Event> events = eventsDao.getEvents();
                  displayMessage(R.string.event_deleted_message);
                  runOnUiThread(new Runnable()
                  {
                    @Override
                    public void run()
                    {
                      m_adapter.updateAllEvents(events, null, false);
                      findViewById(R.id.button_add_new_event).setEnabled(m_adapter.getItemCount() < MAX_EVENT_COUNT);
                      ActionBar actionBar = getSupportActionBar();
                      if (actionBar != null)
                      {
                        actionBar.setTitle(getString(R.string.edit_events_name, m_adapter.getItemCount(), MAX_EVENT_COUNT));
                      }
                      setResult(EventsSettingsFragment.RESULT_UPDATE_EVENTS);
                    }
                  });
                }
              });
            }
          })
          .create();
      dialog.show();
    }
  }

  private void setMoveButtonVisible(View dataView, boolean visible)
  {
    LinearLayout movementButtons = dataView.findViewById(R.id.movement_buttons);
    if (visible)
    {
      movementButtons.setVisibility(View.VISIBLE);
      m_adapter.setActiveMovementButtons(movementButtons);
    }
    else
    {
      movementButtons.setVisibility(View.GONE);
      m_adapter.setActiveMovementButtons(null);
    }
  }

  private void highlightSelected(View dataView)
  {
    m_adapter.setEventToHighlight(m_adapter.getEvent(dataView));

    ViewGroup listView = findViewById(R.id.recycler_view_event_list);
    for (int i = 0; i < listView.getChildCount(); i++)
    {
      LinearLayout layout = listView.getChildAt(i).findViewById(R.id.contents);
      @DrawableRes int drawableRes = listView.getChildAt(i) == dataView || dataView == null ? R.drawable.back : R.drawable.blank;
      for (int j = 0; j < layout.getChildCount(); j++)
      {
        layout.getChildAt(j).setBackgroundResource(drawableRes);
      }
    }
  }

  public void openEventMoreMenu(View view)
  {
    final ViewGroup dataView = getDataView(view);
    highlightSelected(dataView);

    PopupMenu menu = new PopupMenu(view.getContext(), view);
    menu.getMenuInflater().inflate(R.menu.event_more, menu.getMenu());
    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
    {
      @Override
      public boolean onMenuItemClick(MenuItem item)
      {
        m_popupOptionClicked = true;
        switch (item.getItemId())
        {
          case R.id.action_move:
            setMoveButtonVisible(dataView, true);
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
          default:
            m_popupOptionClicked = false;
            return false;
        }
      }
    });

    menu.setOnDismissListener(new PopupMenu.OnDismissListener()
    {
      @Override
      public void onDismiss(PopupMenu popupMenu)
      {
        if (!m_popupOptionClicked)
        {
          highlightSelected(null);
        }
        m_popupOptionClicked = false;
      }
    });

    menu.show();
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
                final List<Event> nhsEvents = addNHSEvents(EditEventsActivity.this);

                runOnUiThread(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    m_adapter.updateAllEvents(nhsEvents, null, false);
                    Toast.makeText(EditEventsActivity.this, R.string.events_reset_message, Toast.LENGTH_LONG).show();
                    setResult(EventsSettingsFragment.RESULT_UPDATE_EVENTS);
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
            m_adapter.updateAllEvents(events, events.get(events.size() - 1), false);
            findViewById(R.id.button_add_new_event).setEnabled(events.size() < MAX_EVENT_COUNT);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
            {
              actionBar.setTitle(getString(R.string.edit_events_name, events.size(), MAX_EVENT_COUNT));
            }

            setResult(EventsSettingsFragment.RESULT_UPDATE_EVENTS);
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

  public static List<Event> addNHSEvents(DatabaseActivity activity)
  {
    String[] nhsEventNames = activity.getResources().getStringArray(R.array.nhs_event_names);
    EventsDao eventsDao = activity.getDatabase().eventsDao();
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
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
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

    private final Event m_event;
    private final boolean m_newEvent;

    DialogInterfaceOnDismissListener(Event event, boolean newEvent)
    {
      m_event = event;
      m_newEvent = newEvent;
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
      highlightSelected(null);

      if (m_newEvent && !keepEditedEvent)
      {
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {
            EventsDao eventsDao = m_database.eventsDao();
            eventsDao.deleteEvent(m_event);
            final List<Event> events = eventsDao.getEvents();
            displayMessage(R.string.new_event_cancelled_message);
            runOnUiThread(new Runnable()
            {
              @Override
              public void run()
              {
                m_adapter.updateAllEvents(events, null, false);
                findViewById(R.id.button_add_new_event).setEnabled(m_adapter.getItemCount() < MAX_EVENT_COUNT);
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null)
                {
                  actionBar.setTitle(getString(R.string.edit_events_name, m_adapter.getItemCount(), MAX_EVENT_COUNT));
                }
              }
            });
          }
        });
      }
    }
  }
}
