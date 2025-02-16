package com.voxtric.timegraph;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TimeAxisLabelData
{
  private static final int MILLISECONDS_IN_MINUTE = 60000;
  private static final int MILLISECONDS_IN_HOUR = MILLISECONDS_IN_MINUTE * 60;
  private static final int MILLISECONDS_IN_DAY = MILLISECONDS_IN_HOUR * 24;
  private static final int MILLISECONDS_IN_WEEK = MILLISECONDS_IN_DAY * 7;

  private static final int MAX_LABELS = 200;

  final long timestamp;
  final String label;

  private TimeAxisLabelData(long timestamp, String label)
  {
    this.timestamp = timestamp;
    this.label = label;
  }

  public static TimeAxisLabelData[] autoLabel(@NonNull GraphData[] data)
  {
    TimeAxisLabelData[] labelData;
    if (data.length > 0)
    {
      long timeDifference = data[data.length - 1].timestamp - data[0].timestamp;
      if (timeDifference / MILLISECONDS_IN_MINUTE < MAX_LABELS)
      {
        labelData = labelMinutes(data);
      }
      else if (timeDifference / MILLISECONDS_IN_HOUR < MAX_LABELS)
      {
        labelData = labelHours(data);
      }
      else if (timeDifference / MILLISECONDS_IN_DAY < MAX_LABELS)
      {
        labelData = labelDays(data);
      }
      else
      {
        labelData = labelMonths(data);
      }
    }
    else
    {
      labelData = new TimeAxisLabelData[0];
    }
    return labelData;
  }

  private static TimeAxisLabelData[] labelMonths(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.DAY_OF_MONTH, calendar.getMinimum(Calendar.DAY_OF_MONTH));
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.MONTH, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  public static TimeAxisLabelData[] labelWeeks(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.DAY_OF_WEEK, calendar.getMinimum(Calendar.DAY_OF_WEEK));
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  private static TimeAxisLabelData[] labelDays(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.DATE, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  private static TimeAxisLabelData[] labelHours(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.HOUR_OF_DAY, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  private static TimeAxisLabelData[] labelMinutes(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.MINUTE, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }
}
