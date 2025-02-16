package com.voxtric.timegraph;

public class ClickableDataPoint
{
  public final float normalisedX;
  public final float normalisedY;

  public final long timestamp;
  public final float value;

  ClickableDataPoint(float normalisedX, float normalisedY, GraphData dataPoint)
  {
    this.normalisedX = normalisedX;
    this.normalisedY = normalisedY;

    this.timestamp = dataPoint.timestamp;
    this.value = dataPoint.value;
  }
}
