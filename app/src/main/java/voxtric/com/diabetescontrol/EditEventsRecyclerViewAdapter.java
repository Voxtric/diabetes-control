package voxtric.com.diabetescontrol;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import voxtric.com.diabetescontrol.database.Event;

public class EditEventsRecyclerViewAdapter extends RecyclerView.Adapter<EditEventsRecyclerViewAdapter.ViewHolder>
{
    private final List<Event> m_values;
    private HashMap<View, Integer> m_valueMap = new HashMap<>();

    EditEventsRecyclerViewAdapter(List<Event> items)
    {
        m_values = items;
    }

    @NonNull
    @Override
    public EditEventsRecyclerViewAdapter.ViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_event, parent, false);
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
        return m_values.get(m_valueMap.get(view));
    }

    public void updateEvent(View view, Event event, boolean orderChanged)
    {
        int position = m_valueMap.get(view);
        m_values.set(position, event);
        if (orderChanged)
        {
            Collections.sort(m_values, new Comparator<Event>()
            {
                @Override
                public int compare(Event o1, Event o2)
                {
                    if (o1.timeInDay < o2.timeInDay)
                    {
                        return -1;
                    }
                    else if (o1.timeInDay > o2.timeInDay)
                    {
                        return 1;
                    }
                    else
                    {
                        return 0;
                    }
                }
            });
            m_valueMap.clear();
            notifyDataSetChanged();
        }
        else
        {
            notifyItemChanged(position);
        }
    }

    public void deleteEvent(View view)
    {
        int position = m_valueMap.get(view);
        m_values.remove(position);
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