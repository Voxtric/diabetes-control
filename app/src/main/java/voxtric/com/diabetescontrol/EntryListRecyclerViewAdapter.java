package voxtric.com.diabetescontrol;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import voxtric.com.diabetescontrol.database.DataEntry;

public class EntryListRecyclerViewAdapter extends RecyclerView.Adapter<EntryListRecyclerViewAdapter.ViewHolder>
{
    private final List<DataEntry> m_values;
    private HashMap<View, Integer> m_valueMap = new HashMap<>();

    EntryListRecyclerViewAdapter(List<DataEntry> items)
    {
        m_values = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
    {
        holder.setItem(m_values.get(position));
        m_valueMap.put(holder.itemView, position);
    }

    @Override
    public int getItemCount()
    {
        return m_values.size();
    }

    public DataEntry getEntry(View view)
    {
        return m_values.get(m_valueMap.get(view));
    }

    public void deleteEntry(View view)
    {
        int position = m_valueMap.get(view);
        m_values.remove(position);
        m_valueMap.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {

        private final TextView m_timeStampTextView;
        private final TextView m_bloodGlucoseLevelTextView;
        private final TextView m_insulinDoseTextView;
        private final TextView m_eventTextView;

        // Extended view.
        private final TextView m_insulinNameTextView;

        ViewHolder(View view)
        {
            super(view);
            m_timeStampTextView = view.findViewById(R.id.text_view_time_stamp);
            m_bloodGlucoseLevelTextView = view.findViewById(R.id.text_view_blood_glucose_level);
            m_insulinDoseTextView = view.findViewById(R.id.text_view_insulin_dose);
            m_eventTextView = view.findViewById(R.id.text_view_event);

            // Extended view.
            m_insulinNameTextView = view.findViewById(R.id.text_view_insulin_name);
        }

        void setItem(DataEntry entry)
        {
            Date date = new Date(entry.timeStamp);
            String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
            String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

            m_timeStampTextView.setText(String.format("%s\n%s", dateString, timeString));
            m_bloodGlucoseLevelTextView.setText(String.valueOf(entry.bloodGlucoseLevel));
            m_insulinDoseTextView.setText(entry.insulinDose);
            m_eventTextView.setText(entry.event);

            // Extended view.
            if (m_insulinNameTextView != null)
            {
                m_insulinNameTextView.setText(entry.insulinName);
            }
        }
    }
}
