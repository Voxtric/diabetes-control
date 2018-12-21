package voxtric.com.diabetescontrol;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import voxtric.com.diabetescontrol.database.DataEntry;

public class Week
{
    public static List<Week> splitEntries(List<DataEntry> entries)
    {
        List<Week> weeks = new ArrayList<>();
        Week currentWeek = null;
        for (int i = entries.size() - 1; i >= 0; i--)
        {
            DataEntry entry = entries.get(i);
            if (currentWeek == null || entry.timeStamp > currentWeek.weekEnding)
            {
                currentWeek = new Week(entry);
                weeks.add(currentWeek);
            }
            else
            {
                currentWeek.addEntry(entry);
            }
        }

        return weeks;
    }

    public final long weekBeginning;
    public final long weekEnding;
    public final List<DataEntry> entries = new ArrayList<>();
    public final Set<String> insulinNames = new HashSet<>();
    public final Set<Pair<String, Long>> events = new TreeSet<>(new Comparator<Pair<String, Long>>()
    {
        @Override
        public int compare(Pair<String, Long> o1, Pair<String, Long> o2)
        {
            if (o1.first.equals(o2.first))
            {
                return 0;
            }
            else if (o1.second < o2.second)
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
    });

    private Week(DataEntry entry)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry.timeStamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());

        weekBeginning = calendar.getTimeInMillis();
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        weekEnding = calendar.getTimeInMillis() - 1;
        addEntry(entry);
    }

    private void addEntry(DataEntry entry)
    {
        entries.add(entry);
        insulinNames.add(entry.insulinName);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(entry.timeStamp);
        calendar.set(Calendar.YEAR, 1);
        calendar.set(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        events.add(new Pair<>(entry.event, calendar.getTimeInMillis()));
    }
}
