package voxtric.com.diabetescontrol;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import voxtric.com.diabetescontrol.database.Event;

public class EditEventsRecyclerViewAdapter extends RecyclerView.Adapter<EditEventsRecyclerViewAdapter.ViewHolder>
{
  private List<Event> m_values;
  private final HashMap<View, Integer> m_valueMap = new HashMap<>();
  private EditEventsActivity m_activity;

  EditEventsRecyclerViewAdapter(List<Event> items, EditEventsActivity activity)
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

  Event getEvent(View view)
  {
    Event event = null;
    Integer position = m_valueMap.get(view);
    if (position != null)
    {
      event = m_values.get(position);
    }
    return event;
  }

  Event getEvent(int position)
  {
    return m_values.get(position);
  }

  void updateEvent(View view, Event event)
  {
    Integer position = m_valueMap.get(view);
    if (position != null)
    {
      m_values.set(position, event);
      notifyItemChanged(position);
    }
  }

  void updateAllEvents(List<Event> items)
  {
    m_values = items;
    m_valueMap.clear();
    notifyDataSetChanged();
  }

  class ViewHolder extends RecyclerView.ViewHolder
  {
    private final TextView m_eventNameTextView;
    private final TextView m_eventTimeTextView;

    ViewHolder(View view)
    {
      super(view);
      m_eventNameTextView = view.findViewById(R.id.text_view_event_name);
      m_eventTimeTextView = view.findViewById(R.id.text_view_event_time);
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
    }
  }
}