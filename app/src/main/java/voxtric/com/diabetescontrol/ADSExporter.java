package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.Event;
import voxtric.com.diabetescontrol.database.TargetChange;

public class ADSExporter extends PDFGenerator
{
  private static final float DAY_HEADER_WIDTH = 30.0f;
  private static final float DAY_HEADER_HEIGHT = 80.0f;
  private static final float EVENT_MAX_WIDTH = 60.0f;
  private static final float EVENT_HEADER_HEIGHT = 120.0f;
  private static final float EXTRAS_MIN_WIDTH = 140.0f;
  private static final float DATA_GAP = FONT_SIZE_MEDIUM * 1.6f;

  private final AppDatabase m_database;
  private final List<Week> m_weeks;
  private final String m_fileName;

  ADSExporter(List<DataEntry> entries, AppDatabase database)
  {
    m_database = database;
    m_weeks = Week.splitEntries(entries, database.eventsDao());
    Date startDate = new Date(entries.get(entries.size() - 1).dayTimeStamp);
    Date endDate = new Date(entries.get(0).dayTimeStamp);
    String startDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(startDate);
    String endDateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(endDate);
    if (startDateString.equals(endDateString))
    {
      m_fileName = String.format("%s (ADS).pdf", startDateString);
    }
    else
    {
      m_fileName = String.format("%s - %s (ADS).pdf", startDateString, endDateString);
    }
  }

  @Override
  public String getFileName()
  {
    return m_fileName;
  }

  @Override
  public ByteArrayOutputStream createPDF(final Activity activity)
  {
    try
    {
      for (final Week week : m_weeks)
      {
        addPage(week, activity);
        if (activity instanceof FragmentActivity)
        {
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              FragmentManager fragmentManager = ((FragmentActivity)activity).getSupportFragmentManager();
              ExportDialogFragment exportDialogFragment = (ExportDialogFragment)fragmentManager.findFragmentByTag(ExportDialogFragment.TAG);
              if (exportDialogFragment != null)
              {
                ((ProgressBar)exportDialogFragment.getAlertDialog().findViewById(R.id.progress_bar_export)).incrementProgressBy(week.entries.size());
              }
            }
          });
        }
      }
      return getOutputStream();
    }
    catch (IOException exception)
    {
      exception.printStackTrace();
      return null;
    }
  }

  private void showExtras(Activity activity, StringBuilder foodEatenStringBuilder, StringBuilder additionalNotesStringBuilder, float startX, float availableSpace, float height) throws IOException
  {
    float minimumHeight = height - DAY_HEADER_HEIGHT;
    if (foodEatenStringBuilder.length() > 0)
    {
      height = drawText(FONT_BOLD, FONT_SIZE_SMALL, activity.getString(R.string.food_eaten_label), startX + LINE_SPACING, height);
      String foodEatenString = foodEatenStringBuilder.substring(0, foodEatenStringBuilder.length() - 3);
      height = drawTextParagraphed(FONT, FONT_SIZE_SMALL, foodEatenString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height, minimumHeight);
      foodEatenStringBuilder.setLength(0);
    }
    if (additionalNotesStringBuilder.length() > 0)
    {
      height = drawText(FONT_BOLD, FONT_SIZE_SMALL, activity.getString(R.string.additional_notes_label), startX + LINE_SPACING, height);
      String additionalNotesString = additionalNotesStringBuilder.substring(0, additionalNotesStringBuilder.length() - 1);
      drawTextParagraphed(FONT, FONT_SIZE_SMALL, additionalNotesString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height, minimumHeight);
      additionalNotesStringBuilder.setLength(0);
    }
  }

  private void addPage(Week week, Activity activity) throws IOException
  {
    super.addPage();
    final String[] DAYS = activity.getResources().getStringArray(R.array.days);
    float height = PDRectangle.A4.getHeight() - BORDER;

    // Week Commencing.
    Date date = new Date(week.weekBeginning);
    String dateString = activity.getString(R.string.week_commencing, DateFormat.getDateInstance(DateFormat.SHORT).format(date));
    drawText(FONT, FONT_SIZE_MEDIUM, dateString, BORDER, height);

    // Pre-meal and post-meal targets.
    String targetString = null;
    TargetChange targetChange = m_database.targetChangesDao().findChangeBetween(week.weekBeginning, week.weekEnding);
    if (targetChange == null)
    {
      targetChange = m_database.targetChangesDao().findFirstBefore(week.weekBeginning);
      if (targetChange == null)
      {
        targetString = activity.getString(R.string.blood_glucose_targets_empty);
      }
    }
    if (targetChange != null)
    {
      targetString = activity.getString(R.string.blood_glucose_targets, targetChange.preMealLower,
          targetChange.preMealUpper, targetChange.postMealLower, targetChange.postMealUpper);
    }
    height = drawText(FONT, FONT_SIZE_MEDIUM, targetString, BORDER + (m_pageWidth / 3.0f), height);

    // Event boxes for each day.
    Map<String, Float> eventStartXMap = new HashMap<>();
    height -= VERTICAL_SPACE;
    float eventWidth = Math.min((m_pageWidth - DAY_HEADER_WIDTH - EXTRAS_MIN_WIDTH) / week.events.size(), EVENT_MAX_WIDTH);
    float startX = BORDER + DAY_HEADER_WIDTH;
    for (Event event : week.events)
    {
      eventStartXMap.put(event.name, startX);
      drawBox(startX, height, startX + eventWidth, height - EVENT_HEADER_HEIGHT, BLACK, null);
      //drawTextCentered(FONT, FONT_SIZE_LARGE, event.first, 90.0f, startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f));
      drawCenteredTextParagraphed(FONT, FONT_SIZE_LARGE, event.name, 90.0f,
          startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f), EVENT_HEADER_HEIGHT - (LINE_SPACING * 2.0f));

      for (int i = 0; i < DAYS.length; i++)
      {
        float dayHeight = height - EVENT_HEADER_HEIGHT - (DAY_HEADER_HEIGHT * (float)i);
        drawBox(startX, dayHeight, startX + eventWidth, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);

        dayHeight -= 1.0f;
        dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, activity.getString(R.string.reading), startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
        dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, activity.getString(R.string.time), startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
        drawTextCenterAligned(FONT, FONT_SIZE_SMALL, activity.getString(R.string.dose), startX + (eventWidth / 2.0f), dayHeight);
      }

      startX += eventWidth;
    }
    height -= EVENT_HEADER_HEIGHT;
    float availableSpace = m_pageWidth - startX + BORDER;

    // Day headers and extras.
    float tempHeight = height + (FONT_SIZE_LARGE * 2.2f);
    drawBox(startX, tempHeight, startX + availableSpace, height, BLACK, null);
    tempHeight = drawTextCenterAligned(FONT, FONT_SIZE_LARGE, activity.getString(R.string.food_eaten_title), startX + (availableSpace / 2.0f), tempHeight);
    drawTextCenterAligned(FONT, FONT_SIZE_LARGE, activity.getString(R.string.additional_notes_title), startX + (availableSpace / 2.0f), tempHeight);
    for (int i = 0; i < DAYS.length; i++)
    {
      float dayHeight = height - (DAY_HEADER_HEIGHT * i);
      drawBox(BORDER, dayHeight, BORDER + DAY_HEADER_WIDTH, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);
      drawTextCentered(FONT, FONT_SIZE_LARGE, DAYS[i], 90.0f, BORDER + (DAY_HEADER_WIDTH / 2.0f), dayHeight - (DAY_HEADER_HEIGHT / 2.0f));
      drawBox(startX, dayHeight, startX + availableSpace, dayHeight - DAY_HEADER_HEIGHT, BLACK, null);
    }

    // Data
    int lastDayOfWeek = -1;
    float dayStartHeight = height;
    StringBuilder foodEatenStringBuilder = new StringBuilder();
    StringBuilder additionalNotesStringBuilder = new StringBuilder();
    for (int i = 0; i < week.entries.size(); i++)
    {
      DataEntry entry = week.entries.get(i);
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(entry.dayTimeStamp);

      int dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % DAYS.length;
      dayStartHeight = height - (DAY_HEADER_HEIGHT * dayOfWeek);
      float dayHeight = dayStartHeight - FONT_SIZE_SMALL - (FONT_SIZE_MEDIUM * 0.2f);
      float eventStartX = eventStartXMap.get(entry.event);
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, String.valueOf(entry.bloodGlucoseLevel), eventStartX + (eventWidth / 2.0f), dayHeight);

      dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
      date = new Date(entry.actualTimestamp);
      String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, timeString, eventStartX + (eventWidth / 2.0f), dayHeight);

      dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, entry.insulinDose, eventStartX + (eventWidth / 2.0f), dayHeight);

      // Food eaten and additional notes.
      if (lastDayOfWeek != -1 && lastDayOfWeek != dayOfWeek)
      {
        showExtras(activity, foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, height - (DAY_HEADER_HEIGHT * lastDayOfWeek));
      }
      lastDayOfWeek = dayOfWeek;

      if (entry.foodEaten.length() > 0)
      {
        foodEatenStringBuilder.append(entry.foodEaten.replaceAll("\n", ", "));
        foodEatenStringBuilder.append(" | ");
      }
      if (entry.additionalNotes.length() > 0)
      {
        additionalNotesStringBuilder.append(entry.additionalNotes);
        additionalNotesStringBuilder.append('\n');
      }
    }
    showExtras(activity, foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, dayStartHeight);
    height -= DAY_HEADER_HEIGHT * DAYS.length;

    // Insulin used
    height -= VERTICAL_SPACE;
    StringBuilder insulinUsedStringBuilder = new StringBuilder(" ");
    for (String insulinName : week.insulinNames)
    {
      insulinUsedStringBuilder.append(insulinName);
      insulinUsedStringBuilder.append(", ");
    }
    String insulinUsedString = insulinUsedStringBuilder.substring(0, insulinUsedStringBuilder.length() - 2);
    height = drawText(FONT, FONT_SIZE_MEDIUM, activity.getString(R.string.insulin_used, insulinUsedString), BORDER, height);

    // Contact Details
    height -= VERTICAL_SPACE / 2.0f;
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
    String contactName = preferences.getString("contact_name", null);
    if (contactName == null)
    {
      contactName = "...........................................";
    }
    drawText(FONT, FONT_SIZE_MEDIUM, activity.getString(R.string.contact_name, contactName), BORDER, height);

    String contactNumber = preferences.getString("contact_number", null);
    if (contactNumber == null)
    {
      contactNumber = "...........................................";
    }
    drawText(FONT, FONT_SIZE_MEDIUM, activity.getString(R.string.contact_number, contactNumber), BORDER + (m_pageWidth / 2.0f), height);
  }
}
