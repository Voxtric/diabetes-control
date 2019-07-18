package voxtric.com.diabetescontrol.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.Event;

public class EditEventsRecyclerViewAdapter extends RecyclerView.Adapter<EditEventsRecyclerViewAdapter.ViewHolder>
{
  private List<Event> m_values;
  private final HashMap<View, Integer> m_valueMap = new HashMap<>();
  private EditEventsActivity m_activity;

  private Event m_eventToHighlight = null;
  private boolean m_showButtonsOnHighlightedEvent = false;
  private LinearLayout m_activeMovementButtons = null;

  public EditEventsRecyclerViewAdapter(List<Event> items, EditEventsActivity activity)
  {
    m_values = items;
    m_activity = activity;
  }

  @NonNull
  @Override
  public EditEventsRecyclerViewAdapter.ViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, int viewType)
  {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.event_row, parent, false);
    return new EditEventsRecyclerViewAdapter.ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull final EditEventsRecyclerViewAdapter.ViewHolder holder, int position)
  {
    Event event = m_values.get(position);
    holder.setItem(event);
    m_valueMap.put(holder.itemView, position);

    if (event.name.length() == 0)
    {
      EditEventsActivity activity = (EditEventsActivity)holder.itemView.getContext();
      activity.editEventName(holder.itemView, true);
    }
    else if (event.timeInDay == -1)
    {
      event.timeInDay = 0L;
      m_values.set(position, event);
      EditEventsActivity activity = (EditEventsActivity)holder.itemView.getContext();
      activity.editEventTime(holder.itemView, true);
    }
  }

  @Override
  public int getItemCount()
  {
    return m_values.size();
  }

  public Event getEvent(View view)
  {
    Event event = null;
    Integer position = m_valueMap.get(view);
    if (position != null)
    {
      event = m_values.get(position);
    }
    return event;
  }

  public Event getEvent(int position)
  {
    return m_values.get(position);
  }

  public void updateAllEvents(List<Event> items, Event eventToHighlight, boolean showButtonsOnHighlightedEvent)
  {
    m_values = items;
    m_eventToHighlight = eventToHighlight;
    m_showButtonsOnHighlightedEvent = showButtonsOnHighlightedEvent;

    m_valueMap.clear();
    notifyDataSetChanged();
  }

  public void setEventToHighlight(Event event)
  {
    m_eventToHighlight = event;
  }

  public void setActiveMovementButtons(LinearLayout activeMovementButtons)
  {
    m_activeMovementButtons = activeMovementButtons;
  }

  public LinearLayout getActiveMovementButtons()
  {
    return m_activeMovementButtons;
  }

  class ViewHolder extends RecyclerView.ViewHolder
  {
    private final TextView m_eventNameTextView;
    private final TextView m_eventTimeTextView;
    private final ImageButton m_eventMoreImageButton;
    private final LinearLayout m_movementButtons;

    ViewHolder(View view)
    {
      super(view);
      m_eventNameTextView = view.findViewById(R.id.text_view_event_name);
      m_eventTimeTextView = view.findViewById(R.id.text_view_event_time);
      m_eventMoreImageButton = view.findViewById(R.id.image_button_event_more);
      m_movementButtons = view.findViewById(R.id.movement_buttons);

      view.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          m_activity.openEventMoreMenu(v);
          return true;
        }
      });
    }

    void setItem(Event event)
    {
      m_eventNameTextView.setText(event.name);

      if (event.timeInDay != -1)
      {
        Date date = new Date(event.timeInDay);
        String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        m_eventTimeTextView.setText(timeString);
      }
      else
      {
        m_eventTimeTextView.setText("");
      }

      @DrawableRes int drawableRes = R.drawable.back;
      int movementButtonVisibility = View.GONE;
      if (m_eventToHighlight != null)
      {
        if (m_eventToHighlight.id != event.id)
        {
          drawableRes = R.drawable.blank;
        }
        else if (m_showButtonsOnHighlightedEvent)
        {
          movementButtonVisibility = View.VISIBLE;
          m_activeMovementButtons = m_movementButtons;
        }
      }
      m_eventNameTextView.setBackgroundResource(drawableRes);
      m_eventTimeTextView.setBackgroundResource(drawableRes);
      m_eventMoreImageButton.setBackgroundResource(drawableRes);
      m_movementButtons.setVisibility(movementButtonVisibility);
    }
  }
}