package voxtric.com.diabetescontrol;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import voxtric.com.diabetescontrol.database.DataEntry;

public class EntryListRecyclerViewAdapter extends RecyclerView.Adapter<EntryListRecyclerViewAdapter.ViewHolder>
{
    private final List<DataEntry> m_values;
    private HashMap<View, Integer> m_valueMap = new HashMap<>();
    private MainActivity m_activity;

    EntryListRecyclerViewAdapter(List<DataEntry> items, MainActivity activity)
    {
        m_values = items;
        m_activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entry_row, parent, false);
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

            view.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View v)
                {
                    m_activity.openEntryMoreMenu(v);
                    return true;
                }
            });
        }

        void setItem(DataEntry entry)
        {
            Date date = new Date(entry.timeStamp);
            String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
            String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

            if (m_activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            {
                String dayString = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
                m_timeStampTextView.setText(String.format("%s\n%s\n%s", dayString, dateString, timeString));
            }
            else
            {
                m_timeStampTextView.setText(String.format("%s\n%s", dateString, timeString));
            }
            m_bloodGlucoseLevelTextView.setText(String.valueOf(entry.bloodGlucoseLevel));
            m_insulinDoseTextView.setText(entry.insulinDose);
            m_eventTextView.setText(entry.event);
            if (m_insulinNameTextView != null)
            {
                m_insulinNameTextView.setText(entry.insulinName);
            }


            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(m_activity);
            boolean highlightEntry = preferences.getBoolean("highlight_entries", false);
            if (highlightEntry)
            {
                float greenStart = preferences.getFloat("bgl_green_start", 4.0f);
                float yellowStart = preferences.getFloat("bgl_yellow_start", 7.0f);
                float orangeStart = preferences.getFloat("bgl_orange_start", 10.0f);
                float redStart = preferences.getFloat("bgl_red_start", 13.0f);
                @ColorRes final int color;
                if (entry.bloodGlucoseLevel >= greenStart && entry.bloodGlucoseLevel <= yellowStart)
                {
                    color = R.color.green;
                }
                else if (entry.bloodGlucoseLevel > yellowStart && entry.bloodGlucoseLevel <= orangeStart)
                {
                    color = R.color.yellow;
                }
                else if (entry.bloodGlucoseLevel > orangeStart && entry.bloodGlucoseLevel <= redStart)
                {
                    color = R.color.orange;
                }
                else
                {
                    color = R.color.red;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    m_bloodGlucoseLevelTextView.setTextColor(m_activity.getColor(color));
                }
                else
                {
                    m_bloodGlucoseLevelTextView.setTextColor(m_activity.getResources().getColor(color));
                }
            }
        }
    }
}
