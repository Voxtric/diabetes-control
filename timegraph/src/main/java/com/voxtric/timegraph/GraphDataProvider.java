package com.voxtric.timegraph;

public interface GraphDataProvider
{
  GraphData[] getData(TimeGraph graph, long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp);
  TimeAxisLabelData[] getLabelsForData(GraphData[] data);
}
