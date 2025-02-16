package com.voxtric.timegraph;

public class GraphData
{
  public final long timestamp;
  public final float value;

  public GraphData(long timestamp, float value)
  {
    this.timestamp = timestamp;
    this.value = value;
  }
}
