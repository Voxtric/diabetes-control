package com.voxtric.diabetescontrol.exporting;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Event;
import com.voxtric.diabetescontrol.database.Food;
import com.voxtric.diabetescontrol.database.Preference;
import com.voxtric.diabetescontrol.database.PreferencesDao;
import com.voxtric.diabetescontrol.database.TargetChange;

public class AdsExporter extends PdfGenerator implements IExporter
{
  private static final float DAY_HEADER_WIDTH = 30.0f;
  private static final float DAY_HEADER_HEIGHT = 80.0f;
  private static final float EVENT_MAX_WIDTH = 60.0f;
  private static final float EVENT_HEADER_HEIGHT = 120.0f;
  private static final float EXTRAS_MIN_WIDTH = 140.0f;
  private static final float DATA_GAP = FONT_SIZE_MEDIUM * 1.6f;

  private List<Week> m_weeks = null;

  @Override
  public byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService)
  {
    m_weeks = Week.splitEntries(entries, AppDatabase.getInstance().eventsDao());
    return createPDF(exportForegroundService);
  }

  @Override
  public String getFormatName()
  {
    return "ADS";
  }

  @Override
  public String getFileExtension()
  {
    return "pdf";
  }

  @Override
  public String getFileMimeType()
  {
    return "application/pdf";
  }

  @Override
  public byte[] createPDF(ExportForegroundService exportForegroundService)
  {
    byte[] pdfData = null;
    try
    {
      for (final Week week : m_weeks)
      {
        addPage(exportForegroundService, week);
        exportForegroundService.incrementProgress(week.entries.size());
      }
      pdfData = getOutputStream().toByteArray();
    }
    catch (IOException exception)
    {
      exception.printStackTrace();
    }
    return pdfData;
  }

  private void showExtras(Context context, StringBuilder foodEatenStringBuilder, StringBuilder additionalNotesStringBuilder, float startX, float availableSpace, float height) throws IOException
  {
    float minimumHeight = height - DAY_HEADER_HEIGHT;
    if (foodEatenStringBuilder.length() > 0)
    {
      height = drawText(FONT_BOLD, FONT_SIZE_SMALL, context.getString(R.string.food_eaten_label), startX + LINE_SPACING, height);
      String foodEatenString = foodEatenStringBuilder.substring(0, foodEatenStringBuilder.length() - 3);
      height = drawTextParagraphed(FONT, FONT_SIZE_SMALL, foodEatenString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height, minimumHeight);
      foodEatenStringBuilder.setLength(0);
    }
    if (additionalNotesStringBuilder.length() > 0)
    {
      height = drawText(FONT_BOLD, FONT_SIZE_SMALL, context.getString(R.string.additional_notes_label), startX + LINE_SPACING, height);
      String additionalNotesString = additionalNotesStringBuilder.substring(0, additionalNotesStringBuilder.length() - 1);
      drawTextParagraphed(FONT, FONT_SIZE_SMALL, additionalNotesString, startX + LINE_SPACING, startX + availableSpace - LINE_SPACING, height, minimumHeight);
      additionalNotesStringBuilder.setLength(0);
    }
  }

  private void addPage(Context context, Week week) throws IOException
  {
    super.addPage(PDRectangle.A4, BORDER);
    final String[] DAYS = context.getResources().getStringArray(R.array.days);
    float height = PDRectangle.A4.getHeight() - BORDER;

    // Week Commencing.
    Date date = new Date(week.weekBeginning);
    String dateString = context.getString(R.string.week_commencing, DateFormat.getDateInstance(DateFormat.SHORT).format(date));

    drawBox(BORDER, height, 130.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, BLUE);
    drawBox(130.0f, height, 190.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    drawText(FONT, FONT_SIZE_MEDIUM, dateString, BORDER + 1.0f, height);


    // Pre-meal and post-meal targets.
    String targetString = null;
    TargetChange targetChange = AppDatabase.getInstance().targetChangesDao().findChangeBetween(week.weekBeginning, week.weekEnding);
    if (targetChange == null)
    {
      targetChange = AppDatabase.getInstance().targetChangesDao().findFirstBefore(week.weekBeginning);
      if (targetChange == null)
      {
        targetString = context.getString(R.string.blood_glucose_targets_empty);
      }
    }
    if (targetChange != null)
    {
      targetString = context.getString(R.string.blood_glucose_targets, targetChange.preMealLower,
          targetChange.preMealUpper, targetChange.postMealLower, targetChange.postMealUpper);
    }

    drawBox(BORDER + (m_writableWidth / 3.0f) - 3.0f, height, 369.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, BLUE);
    drawBox(369.0f, height, 461.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    drawBox(461.0f, height, BORDER + m_writableWidth, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    height = drawText(FONT, FONT_SIZE_MEDIUM, targetString, BORDER + (m_writableWidth / 3.0f) - 2.0f, height);

    // Event boxes for each day.
    Map<String, Float> eventStartXMap = new HashMap<>();
    height -= VERTICAL_SPACE;
    float eventWidth = Math.min((m_writableWidth - DAY_HEADER_WIDTH - EXTRAS_MIN_WIDTH) / week.events.size(), EVENT_MAX_WIDTH);
    float startX = BORDER + DAY_HEADER_WIDTH;
    for (Event event : week.events)
    {
      eventStartXMap.put(event.name, startX);
      drawBox(startX, height, startX + eventWidth, height - EVENT_HEADER_HEIGHT, WHITE, BLUE);
      //drawTextCentered(FONT, FONT_SIZE_LARGE, event.first, 90.0f, startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f));
      drawCenteredTextParagraphed(FONT, FONT_SIZE_LARGE, event.name, 90.0f,
          startX + (eventWidth / 2.0f), height - (EVENT_HEADER_HEIGHT / 2.0f), EVENT_HEADER_HEIGHT - (LINE_SPACING * 2.0f));

      for (int i = 0; i < DAYS.length; i++)
      {
        float dayHeight = height - EVENT_HEADER_HEIGHT - (DAY_HEADER_HEIGHT * (float)i);
        drawBox(startX, dayHeight, startX + eventWidth, dayHeight - DAY_HEADER_HEIGHT, BLUE, null);

        dayHeight -= 4.0f;
        dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, context.getString(R.string.reading), startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
        dayHeight = drawTextCenterAligned(FONT, FONT_SIZE_SMALL, context.getString(R.string.time), startX + (eventWidth / 2.0f), dayHeight) - DATA_GAP;
        drawTextCenterAligned(FONT, FONT_SIZE_SMALL, context.getString(R.string.dose), startX + (eventWidth / 2.0f), dayHeight);
      }

      startX += eventWidth;
    }
    height -= EVENT_HEADER_HEIGHT;
    float availableSpace = m_writableWidth - startX + BORDER;

    // Day headers and extras.
    float tempHeight = height + (FONT_SIZE_LARGE * 2.2f);
    drawBox(startX, tempHeight, startX + availableSpace, height, WHITE, BLUE);
    tempHeight = drawTextCenterAligned(FONT, FONT_SIZE_LARGE, context.getString(R.string.food_eaten_title), startX + (availableSpace / 2.0f), tempHeight);
    drawTextCenterAligned(FONT, FONT_SIZE_LARGE, context.getString(R.string.additional_notes_title), startX + (availableSpace / 2.0f), tempHeight);
    for (int i = 0; i < DAYS.length; i++)
    {
      float dayHeight = height - (DAY_HEADER_HEIGHT * i);
      drawBox(BORDER, dayHeight, BORDER + DAY_HEADER_WIDTH, dayHeight - DAY_HEADER_HEIGHT, WHITE, BLUE);
      drawTextCentered(FONT, FONT_SIZE_LARGE, DAYS[i], 90.0f, BORDER + (DAY_HEADER_WIDTH / 2.0f), dayHeight - (DAY_HEADER_HEIGHT / 2.0f));
      drawBox(startX, dayHeight, startX + availableSpace, dayHeight - DAY_HEADER_HEIGHT, BLUE, null);
    }

    // Data
    float dataHeightStart = height;
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
      float dayHeight = dayStartHeight - FONT_SIZE_SMALL - (FONT_SIZE_MEDIUM * 0.2f) - 3.0f;
      Float eventStartXValue = eventStartXMap.get(entry.event);
      float eventStartX = eventStartXValue != null ? eventStartXValue : 0.0f;
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, String.valueOf(entry.bloodGlucoseLevel), eventStartX + (eventWidth / 2.0f), dayHeight);

      dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
      date = new Date(entry.actualTimestamp);
      String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, timeString, eventStartX + (eventWidth / 2.0f), dayHeight);

      dayHeight -= FONT_SIZE_SMALL + DATA_GAP;
      drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : context.getString(R.string.not_applicable), eventStartX + (eventWidth / 2.0f), dayHeight);

      // Food eaten and additional notes.
      if (lastDayOfWeek != -1 && lastDayOfWeek != dayOfWeek)
      {
        showExtras(context, foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, height - (DAY_HEADER_HEIGHT * lastDayOfWeek));
      }
      lastDayOfWeek = dayOfWeek;

      List<Food> foodList = AppDatabase.getInstance().foodsDao().getFoods(entry.actualTimestamp);
      if (foodList.size() > 0)
      {
        foodEatenStringBuilder.append(foodList.get(0).name);
        for (int j = 1; j < foodList.size(); j++)
        {
          foodEatenStringBuilder.append(", ");
          foodEatenStringBuilder.append(foodList.get(j).name);
        }
        foodEatenStringBuilder.append(" | ");
      }
      if (entry.additionalNotes.length() > 0)
      {
        additionalNotesStringBuilder.append(entry.additionalNotes);
        additionalNotesStringBuilder.append('\n');
      }
    }
    showExtras(context, foodEatenStringBuilder, additionalNotesStringBuilder, startX, availableSpace, dayStartHeight);
    height -= DAY_HEADER_HEIGHT * DAYS.length;

    drawBox(BORDER + DAY_HEADER_WIDTH, dataHeightStart, startX, height, BLUE, null);

    // Insulin used
    height -= VERTICAL_SPACE;
    String insulinUsedString = "";
    if (!week.insulinNames.isEmpty())
    {
      StringBuilder insulinUsedStringBuilder = new StringBuilder();
      for (String insulinName : week.insulinNames)
      {
        insulinUsedStringBuilder.append(insulinName);
        insulinUsedStringBuilder.append(", ");
      }
      insulinUsedString = insulinUsedStringBuilder.substring(0, insulinUsedStringBuilder.length() - 2);
    }
    drawBox(BORDER, height, 95.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, BLUE);
    drawBox(95.0f, height, BORDER + m_writableWidth, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    height = drawText(FONT, FONT_SIZE_MEDIUM, context.getString(R.string.insulin_used, insulinUsedString), BORDER + 1.0f, height);

    // Contact Details
    height -= VERTICAL_SPACE / 2.0f;

    PreferencesDao preferencesDao = AppDatabase.getInstance().preferencesDao();
    Preference preference = preferencesDao.getPreference(context.getResources().getResourceName(R.id.edit_text_contact_name));
    String contactName = preference != null && preference.value.length() > 0 ? preference.value : "...........................................";
    drawBox(BORDER, height, 105.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, BLUE);
    drawBox(105.0f, height, BORDER + (m_writableWidth / 2.0f), height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    drawText(FONT, FONT_SIZE_MEDIUM, context.getString(R.string.contact_name, contactName), BORDER + 1.0f, height);

    preference = preferencesDao.getPreference(context.getResources().getResourceName(R.id.edit_text_contact_number));
    String contactNumber = preference != null && preference.value.length() > 0  ? preference.value : "...........................................";
    drawBox(BORDER + (m_writableWidth / 2.0f), height, 383.0f, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, BLUE);
    drawBox(383.0f, height, BORDER + m_writableWidth, height - FONT_SIZE_MEDIUM - LINE_SPACING, BLUE, null);
    drawText(FONT, FONT_SIZE_MEDIUM, context.getString(R.string.contact_number, contactNumber), BORDER + (m_writableWidth / 2.0f) + 1.0f, height);
  }
}
