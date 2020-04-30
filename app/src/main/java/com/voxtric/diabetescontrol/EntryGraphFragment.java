package com.voxtric.diabetescontrol;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
  private static final long DEFAULT_DISPLAY_DURATION = 86400000L * 5L;
  private static final long STATISTICS_RAISE_DURATION = 200L;

  private boolean m_calculatingNewStatistics = false;

  private boolean m_calculateNewStatistics = false;
  private long m_periodStartTimestamp = 0L;
  private long m_periodEndTimestamp = 0L;
  private int m_consecutiveFailedGraphDataGets = 0;

  private float m_maxValue = 0.0f;

  private float m_moveDirection = 1.0f;

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
    setGraphListeners(graph);

    final LinearLayout statisticsContentView = view.findViewById(R.id.statistics_content);
    if (statisticsContentView != null)
    {
      statisticsContentView.post(new Runnable()
      {
        @Override
        public void run()
        {
          statisticsContentView.animate().translationYBy(statisticsContentView.getHeight()).setDuration(0L).start();
        }
      });
    }

    if (savedInstanceState == null)
    {
      defaultGraphInitialise(view);
    }
    else
    {
      m_maxValue = savedInstanceState.getFloat("m_maxValue");
    }

    return view;
  }

  private void defaultGraphInitialise(View rootView)
  {
    final TimeGraph graph = rootView.findViewById(R.id.graph);
    final Activity activity = getActivity();
    if (activity != null)
    {
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();

          m_maxValue = Math.max(dataEntriesDao.getMaxBloodGlucoseLevel(), 16.0f);
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              graph.setValueAxisMax(m_maxValue, false);
            }
          });
          setGraphHighlighting(graph, m_maxValue);

          DataEntry lastEntry = dataEntriesDao.findFirstBefore(Long.MAX_VALUE);
          if (lastEntry != null)
          {
            long endTimestamp = lastEntry.actualTimestamp;
            long startTimestamp = endTimestamp - DEFAULT_DISPLAY_DURATION;
            DataEntry firstEntry = dataEntriesDao.findFirstBefore(startTimestamp + 1);
            if (firstEntry == null)
            {
              firstEntry = dataEntriesDao.findFirstAfter(startTimestamp);
            }

            if (firstEntry != null)
            {
              startTimestamp = firstEntry.actualTimestamp;
            }

            graph.setVisibleDataPeriod(startTimestamp, endTimestamp, EntryGraphFragment.this, true);
          }
        }
      });
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    MainActivity activity = (MainActivity)getActivity();
    if (activity != null)
    {
      //noinspection deprecation
      if (isVisible() && getUserVisibleHint())
      {
        ShowcaseViewHandler.handleEntryGraphFragmentShowcaseViews(activity);
      }
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
  {
    savedInstanceState.putFloat("m_maxValue", m_maxValue);
  }

  @Override
  public void onResume()
  {
    super.onResume();
    refreshGraph(true, false);
  }

  @Override
  public GraphData[] getData(TimeGraph graph, long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
  {
    DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
    List<DataEntry> entries = dataEntriesDao.findAllBetween(startTimestamp, endTimestamp);

    GraphData[] graphData = new GraphData[entries.size()];
    int graphDataIndex = 0;
    for (int i = 0; i < graphData.length; i++)
    {
      DataEntry entry = entries.get(graphData.length - 1 - i);
      //if (entry.bloodGlucoseLevel > 0.0f)
      {
        graphData[graphDataIndex] = new GraphData(entry.actualTimestamp, entry.bloodGlucoseLevel);
        graphDataIndex++;
      }
    }
    if (graphDataIndex != graphData.length)
    {
      GraphData[] oldGraphData = graphData;
      graphData = new GraphData[graphDataIndex];
      System.arraycopy(oldGraphData, 0, graphData, 0, graphDataIndex);
    }

    if (graphData.length < 2)
    {
      if (m_consecutiveFailedGraphDataGets < 1)
      {
        final Activity activity = getActivity();
        if (activity != null)
        {
          graph.post(new Runnable()
          {
            @Override
            public void run()
            {
              defaultGraphInitialise(activity.findViewById(R.id.entry_graph_content));
            }
          });
        }
      }
      m_consecutiveFailedGraphDataGets++;
    }
    else
    {
      m_consecutiveFailedGraphDataGets = 0;
    }

    return graphData;
  }

  @Override
  public TimeAxisLabelData[] getLabelsForData(GraphData[] data)
  {
    return TimeAxisLabelData.autoLabel(data);
  }

  void refreshGraph(final boolean animate, boolean moveToEnd)
  {
    final Activity activity = getActivity();
    if (activity != null)
    {
      final TimeGraph graph = activity.findViewById(R.id.graph);
      if (!moveToEnd)
      {
        graph.refresh(this, animate);
      }
      else
      {
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {
            DataEntriesDao dataEntriesDao = AppDatabase.getInstance().dataEntriesDao();
            DataEntry lastEntry = dataEntriesDao.findFirstBefore(Long.MAX_VALUE);
            m_maxValue = Math.max(dataEntriesDao.getMaxBloodGlucoseLevel(), 16.0f);
            if (lastEntry != null)
            {
              final long endTimestamp = lastEntry.actualTimestamp;
              final long startTimestamp = endTimestamp - DEFAULT_DISPLAY_DURATION;
              activity.runOnUiThread(new Runnable()
              {
                @Override
                public void run()
                {
                  graph.setVisibleDataPeriod(startTimestamp, endTimestamp, EntryGraphFragment.this, animate);
                }
              });
            }
          }
        });
      }
    }
  }

  void updateRangeHighlighting()
  {
    Activity activity = getActivity();
    if (activity != null)
    {
      final TimeGraph graph = activity.findViewById(R.id.graph);
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          setGraphHighlighting(graph, m_maxValue);
        }
      });
    }
  }

  private void calculateNewStatistics(final boolean disableStatisticsViews)
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
                TextView averageBgl = activity.findViewById(R.id.average_bgl);
                TextView minimumBgl = activity.findViewById(R.id.minimum_bgl);
                TextView maximumBgl = activity.findViewById(R.id.maximum_bgl);
                averageBgl.setText(activity.getString(R.string.bgl_postfix, periodAverageBgl));
                minimumBgl.setText(activity.getString(R.string.bgl_postfix, periodMinBgl));
                maximumBgl.setText(activity.getString(R.string.bgl_postfix, periodMaxBgl));
                averageBgl.setEnabled(!disableStatisticsViews);
                minimumBgl.setEnabled(!disableStatisticsViews);
                maximumBgl.setEnabled(!disableStatisticsViews);
              }
            });
          }
        }
        m_calculatingNewStatistics = false;
      }
    });
  }

  void toggleStatisticsVisibility(Activity activity)
  {
    LinearLayout headerView = activity.findViewById(R.id.statistics_layout);
    LinearLayout contentView = activity.findViewById(R.id.statistics_content);
    float moveBy = -contentView.getHeight() * m_moveDirection;
    headerView.animate().translationYBy(moveBy).setDuration(STATISTICS_RAISE_DURATION).start();
    contentView.animate().translationYBy(moveBy).setDuration(STATISTICS_RAISE_DURATION).start();
    headerView.getChildAt(1).animate().rotationBy(-90.0f * m_moveDirection).setDuration(STATISTICS_RAISE_DURATION).start();

    m_moveDirection *= -1.0f;
  }

  private void setGraphListeners(TimeGraph graph)
  {
    graph.setOnDataPointClickedListener(new TimeGraph.OnDataPointClickedListener()
    {
      @Override
      public void onDataPointClicked(TimeGraph graph, long timestamp, float value)
      {
        MainActivity activity = (MainActivity)getActivity();
        if (activity != null)
        {
          activity.navigateToPageFragment(activity.getFragmentIndex(EntryListFragment.class));
          EntryListFragment entryListFragment = activity.getFragment(EntryListFragment.class);
          entryListFragment.selectView(activity, timestamp);
        }
      }
    });

    graph.setOnPeriodChangedListener(new TimeGraph.OnPeriodChangeListener()
    {
      @Override
      public void onPeriodChanged(TimeGraph view, long startTimestamp, long endTimestamp)
      {
        m_periodStartTimestamp = startTimestamp;
        m_periodEndTimestamp = endTimestamp;
        if (!m_calculatingNewStatistics)
        {
          calculateNewStatistics(!view.hasEnoughData());
        }
        else
        {
          m_calculateNewStatistics = true;
        }
      }
    });

    graph.setOnRefreshListener(new TimeGraph.OnRefreshListener()
    {
      @Override
      public void onRefresh(TimeGraph view, long startTimestamp, long endTimestamp, GraphData[] data)
      {
        if (!m_calculatingNewStatistics)
        {
          calculateNewStatistics(!view.hasEnoughData());
        }
        else
        {
          m_calculateNewStatistics = true;
        }
      }
    });
  }

  @SuppressWarnings("ConstantConditions")
  private static void setGraphHighlighting(TimeGraph graph, float maxValue)
  {
    PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();

    Preference idealMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE);
    Preference highMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE);
    Preference actionRequiredMinPreference = preferencesDao.getPreference(BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE);
    float idealMin = idealMinPreference != null ? Float.valueOf(idealMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE);
    float highMin = highMinPreference != null ? Float.valueOf(highMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE);
    float actionRequiredMin = actionRequiredMinPreference != null ? Float.valueOf(actionRequiredMinPreference.value) : BglHighlightingSettingsFragment.DEFAULT_VALUES.get(BglHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE);

    graph.setValueAxisMidLabels(new float[]{ idealMin, highMin, actionRequiredMin });

    Resources resources = graph.getResources();
    float[] rangeValues = new float[]{ 0.0f, idealMin, highMin, actionRequiredMin, maxValue };
    int[] rangeColors = new int[] { resources.getColor(R.color.red), resources.getColor(R.color.green), resources.getColor(R.color.graph_yellow), resources.getColor(R.color.red) };
    graph.setRangeHighlights(rangeValues, rangeColors, TimeGraph.DISPLAY_MODE_UNDERLINE_WITH_FADE, false);
  }
}
