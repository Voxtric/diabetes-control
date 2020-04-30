package com.voxtric.diabetescontrol.exporting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Event;
import com.voxtric.diabetescontrol.database.EventsDao;

class Week
{
  static List<Week> splitEntries(List<DataEntry> entries, EventsDao eventsDao)
  {
    List<Week> weeks = new ArrayList<>();
    Week currentWeek = null;
    for (int i = entries.size() - 1; i >= 0; i--)
    {
      DataEntry entry = entries.get(i);
      if (currentWeek == null || entry.dayTimeStamp > currentWeek.weekEnding)
      {
        currentWeek = new Week(entry, eventsDao);
        weeks.add(currentWeek);
      }
      else
      {
        currentWeek.addEntry(entry, eventsDao);
      }
    }

    return weeks;
  }

  final long weekBeginning;
  final long weekEnding;
  final List<DataEntry> entries = new ArrayList<>();
  final Set<String> insulinNames = new HashSet<>();
  public final Set<Event> events = new TreeSet<>(new Comparator<Event>()
  {
    @Override
    public int compare(Event eventA, Event eventB)
    {
      if (eventA.name.equals(eventB.name))
      {
        return 0;
      }
      else
      {
        if (eventA.order == eventB.order)
        {
          if (eventA.timeInDay < eventB.timeInDay)
          {
            return -1;
          }
          else
          {
            return 1;
          }
        }
        else if (eventA.order < eventB.order)
        {
          return -1;
        }
        else
        {
          return 1;
        }
      }
    }
  });

  private Week(DataEntry entry, EventsDao eventsDao)
  {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(entry.actualTimestamp);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

    weekBeginning = calendar.getTimeInMillis();
    calendar.add(Calendar.WEEK_OF_YEAR, 1);
    weekEnding = calendar.getTimeInMillis() - 1;
    addEntry(entry, eventsDao);
  }

  private void addEntry(DataEntry entry, EventsDao eventsDao)
  {
    entries.add(entry);
    if (entry.insulinDose > 0)
    {
      insulinNames.add(entry.insulinName);
    }

    Event event = eventsDao.getEvent(entry.event);
    if (event == null)
    {
      Calendar baseCalendar = Calendar.getInstance();
      baseCalendar.clear();
      baseCalendar.set(
          baseCalendar.getMinimum(Calendar.YEAR),
          baseCalendar.getMinimum(Calendar.MONTH),
          baseCalendar.getMinimum(Calendar.DATE),
          baseCalendar.getMinimum(Calendar.HOUR_OF_DAY),
          baseCalendar.getMinimum(Calendar.MINUTE),
          baseCalendar.getMinimum(Calendar.SECOND));

      Calendar entryCalendar = Calendar.getInstance();
      entryCalendar.setTimeInMillis(entry.actualTimestamp);
      baseCalendar.set(Calendar.HOUR_OF_DAY, entryCalendar.get(Calendar.HOUR_OF_DAY));
      baseCalendar.set(Calendar.MINUTE, entryCalendar.get(Calendar.MINUTE));
      baseCalendar.set(Calendar.SECOND, entryCalendar.get(Calendar.SECOND));

      event = new Event();
      event.name = entry.event;
      event.timeInDay = baseCalendar.getTimeInMillis();
      event.order = -1;
      for (Event eventIt : events)
      {
        if (event.timeInDay < eventIt.timeInDay)
        {
          event.order = eventIt.order;
          break;
        }
      }
      if (event.order == -1)
      {
        event.order = eventsDao.countEvents() - 1;
      }
    }
    events.add(event);
  }
}
