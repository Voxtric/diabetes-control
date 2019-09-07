package com.voxtric.diabetescontrol.exporting;

import com.voxtric.diabetescontrol.database.DataEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Day
{
  static List<Day> splitEntries(List<DataEntry> entries)
  {
    List<Day> days = new ArrayList<>();
    Day currentDay = null;
    for (int i = entries.size() - 1; i >= 0; i--)
    {
      DataEntry entry = entries.get(i);
      if (currentDay == null || entry.dayTimeStamp > currentDay.dayEnding)
      {
        currentDay = new Day(entry);
        days.add(currentDay);
      }
      else
      {
        currentDay.entries.add(entry);
      }
    }

    return days;
  }

  final long dayBeginning;
  final long dayEnding;
  final List<DataEntry> entries = new ArrayList<>();

  private Day(DataEntry entry)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.dayTimeStamp);

    dayBeginning = entry.dayTimeStamp;
    calendar.add(Calendar.DATE, 1);
    dayEnding = calendar.getTimeInMillis() - 1;
    entries.add(entry);
  }
}
