package com.voxtric.diabetescontrol;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntriesDao;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.database.PreferencesDao;
import com.voxtric.diabetescontrol.settings.fragments.BglHighlightingSettingsFragment;
import com.voxtric.timegraph.GraphData;
import com.voxtric.timegraph.GraphDataProvider;
import com.voxtric.timegraph.TimeAxisLabelData;
import com.voxtric.timegraph.TimeGraph;

import java.util.List;

public class EntryGraphFragment extends Fragment implements GraphDataProvider
{
  private static final Long DEFAULT_DISPLAY_DURATION = 86400000L * 5L;

  private boolean m_calculatingNewStatistics = false;

  private boolean m_calculateNewStatistics = false;
  private long m_periodStartTimestamp = 0L;
  private long m_periodEndTimestamp = 0L;

  public EntryGraphFragment()
  {
    // Required empty public constructor
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    ConstraintLayout view = (ConstraintLayout)inflater.inflate(R.layout.fragment_entry_graph, container, false);

    final TimeGraph graph = view.findViewById(R.id.graph);
    graph.setDisallowHorizontalScrollViews(new ViewGroup[] { container.findViewById(R.id.fragment_container) });
    graph.setOnPeriodChangedListener(new TimeGraph.OnPeriodChangeListener()
    {
      @Override
      public void onPeriodChanged(long startTimestamp, long endTimestamp)
      {
        m_periodStartTimestamp = startTimestamp;
        m_periodEndTimestamp = endTimestamp;
        if (!m_calculatingNewStatistics)
        {
          calculateNewStatistics();
        }
        else
        {
          m_calculateNewStatistics = true;
        }
      }
    });

    if (savedInstanceState == null)
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();

          float maxValue = Math.max(dataEntriesDao.getMaxBloodGlucoseLevel(), 16.0f);
          graph.setValueAxisMax(maxValue, false);
          setGraphHighlighting(graph, maxValue);

          DataEntry lastEntry = dataEntriesDao.findFirstBefore(Long.MAX_VALUE);
          if (lastEntry != null)
          {
            long endTimestamp = lastEntry.actualTimestamp;
            long startTimestamp = endTimestamp - DEFAULT_DISPLAY_DURATION;
            graph.setVisibleDataPeriod(startTimestamp, endTimestamp, EntryGraphFragment.this, true);
          }
        }
      });
    }

    return view;
  }

  @Override
  public void onResume()
  {
    super.onResume();
    refreshGraph(true, false);
  }

  @Override
  public GraphData[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
  {
    GraphData[] graphData = null;

    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    List<DataEntry> entries = dataEntriesDao.findAllBetween(startTimestamp, endTimestamp);
    if (!entries.isEmpty())
    {
      if (entries.size() == 1)
      {
        DataEntry prior = dataEntriesDao.findFirstBefore(entries.get(0).actualTimestamp);
        if (prior != null)
        {
          entries.add(prior);
        }
      }

      graphData = new GraphData[entries.size()];
      for (int i = 0; i < graphData.length; i++)
      {
        DataEntry entry = entries.get(graphData.length - 1 - i);
        graphData[i] = new GraphData(entry.actualTimestamp, entry.bloodGlucoseLevel);
      }
    }
    return graphData;
  }

  @Override
  public TimeAxisLabelData[] getLabelsForData(GraphData[] data)
  {
    return TimeAxisLabelData.autoLabel(data);
  }

  void refreshGraph(boolean animate, boolean moveToEnd)
  {
    Activity activity = getActivity();
    if (activity != null)
    {
      TimeGraph graph = activity.findViewById(R.id.graph);
      if (!moveToEnd)
      {
        graph.refresh(this, animate);
      }
      else
      {
        DataEntry lastEntry = AppDatabase.getInstance().dataEntriesDao().findFirstBefore(Long.MAX_VALUE);
        if (lastEntry != null)
        {
          long endTimestamp = lastEntry.actualTimestamp;
          long startTimestamp = endTimestamp - DEFAULT_DISPLAY_DURATION;
          graph.setVisibleDataPeriod(startTimestamp, endTimestamp, EntryGraphFragment.this, true);
        }
      }
    }
  }

  @SuppressWarnings("ConstantConditions")
  private void setGraphHighlighting(TimeGraph graph, float maxValue)
  {
    PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();

    Preference idealMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE);
    Preference highMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE);
    Preference actionRequiredMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE);
    float idealMin = idealMinPreference != null ? Float.valueOf(idealMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE);
    float highMin = idealMinPreference != null ? Float.valueOf(highMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE);
    float actionRequiredMin = idealMinPreference != null ? Float.valueOf(actionRequiredMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE);

    graph.setValueAxisMidLabels(new float[]{ idealMin, highMin, actionRequiredMin });

    Resources resources = getResources();
    float[] rangeValues = new float[]{ 0.0f, idealMin, highMin, actionRequiredMin, maxValue };
    int[] rangeColors = new int[] { resources.getColor(R.color.red), resources.getColor(R.color.green), resources.getColor(R.color.graph_yellow), resources.getColor(R.color.red) };
    graph.setRangeHighlights(rangeValues, rangeColors, TimeGraph.DISPLAY_MODE_UNDERLINE_WITH_FADE, false);
  }

  private void calculateNewStatistics()
  {
    m_calculatingNewStatistics = true;

    m_calculateNewStatistics = true;
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        while (m_calculateNewStatistics)
        {
          m_calculateNewStatistics = false;

          DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
          final float periodAverageBgl = dataEntriesDao.getAverageBloodGlucoseLevel(m_periodStartTimestamp, m_periodEndTimestamp);
          final float periodMinBgl = dataEntriesDao.getMinBloodGlucoseLevel(m_periodStartTimestamp, m_periodEndTimestamp);
          final float periodMaxBgl = dataEntriesDao.getMaxBloodGlucoseLevel(m_periodStartTimestamp, m_periodEndTimestamp);

          final Activity activity = getActivity();
          if (activity != null)
          {
            activity.runOnUiThread(new Runnable()
            {
              @Override
              public void run()
              {
                ((TextView)activity.findViewById(R.id.average_bgl)).setText(activity.getString(R.string.bgl_postfix, periodAverageBgl));
                ((TextView)activity.findViewById(R.id.minimum_bgl)).setText(activity.getString(R.string.bgl_postfix, periodMinBgl));
                ((TextView)activity.findViewById(R.id.maximum_bgl)).setText(activity.getString(R.string.bgl_postfix, periodMaxBgl));
              }
            });
          }
        }
        m_calculatingNewStatistics = false;
      }
    });
  }
}
