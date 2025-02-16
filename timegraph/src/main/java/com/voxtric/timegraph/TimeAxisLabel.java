package com.voxtric.timegraph;

import android.widget.TextView;

class TimeAxisLabel
{
  final long timestamp;
  float offset;
  final TextView view;

  TimeAxisLabel(TextView view)
  {
    this.timestamp = 0L;
    this.offset = 0.0f;
    this.view = view;
  }

  TimeAxisLabel(long timestamp, TextView view)
  {
    this.timestamp = timestamp;
    this.offset = 0.0f;
    this.view = view;
  }
}
