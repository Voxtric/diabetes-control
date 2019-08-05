package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;

public class EntryListRecyclerViewAdapter extends RecyclerView.Adapter<EntryListRecyclerViewAdapter.ViewHolder>
{
  private MainActivity m_activity;

  private final List<DataEntry> m_values;
  private HashMap<View, Integer> m_valueMap = new HashMap<>();
  private boolean m_loadingMore = false;

  private final float[] m_highlightingValues;

  EntryListRecyclerViewAdapter(MainActivity activity, List<DataEntry> items, float[] highlightingValues)
  {
    m_activity = activity;
    m_values = items;
    m_highlightingValues = highlightingValues;
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

  void loadMore(final Activity activity)
  {
    if (!m_loadingMore && !RecoveryForegroundService.isDownloading())
    {
      m_loadingMore = true;
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          List<DataEntry> newEntries = AppDatabase.getInstance().dataEntriesDao().getPreviousEntries(
              m_values.get(m_values.size() - 1).actualTimestamp, EntryListFragment.LOAD_COUNT);
          m_values.addAll(newEntries);
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              notifyDataSetChanged();
              m_loadingMore = false;
            }
          });
        }
      });
    }
  }

  DataEntry getEntry(View view)
  {
    DataEntry entry = null;
    Integer position = m_valueMap.get(view);
    if (position != null)
    {
      entry = m_values.get(position);
    }
    return entry;
  }

  void deleteEntry(View view)
  {
    Integer position = m_valueMap.get(view);
    if (position != null)
    {
      m_values.remove((int)position);
      m_valueMap.clear();
      notifyDataSetChanged();
    }
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
      Date date = new Date(entry.actualTimestamp);
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
      m_insulinDoseTextView.setText(entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : "N/A");
      m_eventTextView.setText(entry.event);
      if (m_insulinNameTextView != null)
      {
        m_insulinNameTextView.setText(entry.insulinName);
      }

      if (m_highlightingValues != null)
      {
        @ColorRes int color;
        if (entry.bloodGlucoseLevel >= m_highlightingValues[0])
        {
          color = R.color.green;
          if (entry.bloodGlucoseLevel >= m_highlightingValues[1])
          {
            color = R.color.yellow;
            if (entry.bloodGlucoseLevel >= m_highlightingValues[2])
            {
              color = R.color.red;
            }
          }
        }
        else
        {
          color = R.color.red;
        }
        m_bloodGlucoseLevelTextView.setTextColor(m_activity.getResources().getColor(color));
      }
    }
  }
}
