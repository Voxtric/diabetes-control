package com.voxtric.diabetescontrol.exporting;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.TargetChange;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class NhsExporter extends PdfGenerator implements IExporter
{
  private static final PDRectangle DIMENSIONS = new PDRectangle(190.0f * PdfGenerator.MM_PER_INCH, 210.0f * PdfGenerator.MM_PER_INCH);
  private static final int MAX_DAYS_PER_PAGE = 11;

  private static final float FONT_SIZE_MEDIUM = 10.0f;
  private static final float FONT_SIZE_TINY = 5.0f;

  private static final float BORDER = 10.0f;
  private static final float NORMAL_COLUMN_WIDTH = FONT_SIZE_MEDIUM * 3.4f;
  private static final PDFColor WHITE = new PDFColor(255, 255, 255);
  private static final PDFColor BLUE = new PDFColor(183, 230, 251);

  private List<Day> m_days = null;

  @Override
  public byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService)
  {
    m_days = Day.splitEntries(entries);
    return createPDF(exportForegroundService);
  }

  @Override
  public String getFormatName()
  {
    return "NHS";
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
    try
    {
      int dayCount = m_days.size();
      int index = 0;
      while (index < dayCount)
      {
        int entriesToAdd = Math.min(MAX_DAYS_PER_PAGE, dayCount - index);
        addPage(exportForegroundService, index, entriesToAdd);
        exportForegroundService.incrementProgress(entriesToAdd);
        index += entriesToAdd;
      }
      return getOutputStream().toByteArray();
    }
    catch (IOException exception)
    {
      exception.printStackTrace();
      return null;
    }
  }

  private void addPage(Context context, int startIndex, int entriesToAdd) throws IOException
  {
    super.addPage(DIMENSIONS, BORDER);
    float height = DIMENSIONS.getHeight() - BORDER;

    // Pre-meal and post-meal targets.
    String preMealTargetString = "";
    String postMealTargetString = "";
    TargetChange targetChange = AppDatabase.getInstance().targetChangesDao().findChangeBetween(
        m_days.get(startIndex).dayBeginning,
        m_days.get(startIndex + entriesToAdd - 1).dayEnding);
    if (targetChange == null)
    {
      targetChange = AppDatabase.getInstance().targetChangesDao().findFirstBefore(m_days.get(startIndex).dayBeginning);
    }
    if (targetChange != null)
    {
      preMealTargetString = String.format(Locale.getDefault(), "%.1f - %.1f", targetChange.preMealLower, targetChange.preMealUpper);
      postMealTargetString = String.format(Locale.getDefault(), "%.1f - %.1f", targetChange.postMealLower, targetChange.postMealUpper);
    }

    float lastQuarter = DIMENSIONS.getWidth() * 0.75f;
    String emptyTargetRangeString = context.getString(R.string.target_blood_glucose_range_empty);
    drawText(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.target_pre_meal_blood_glucose_range), BORDER, height);
    drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, preMealTargetString, lastQuarter, height + 2.0f);
    height = drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, emptyTargetRangeString, lastQuarter, height) - FONT_SIZE_MEDIUM;
    drawText(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.target_post_meal_blood_glucose_range), BORDER, height);
    drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, postMealTargetString, lastQuarter, height + 2.0f);
    height = drawTextCenterAligned(FONT, FONT_SIZE_MEDIUM, emptyTargetRangeString, lastQuarter, height) - FONT_SIZE_MEDIUM;


    // Primary data.
    float boxStartHeight = height;

    int endIndex = Math.min(startIndex + MAX_DAYS_PER_PAGE, m_days.size()) - 1;
    List<DataEntry> data = AppDatabase.getInstance().dataEntriesDao().findAllBetween(m_days.get(startIndex).dayBeginning, m_days.get(endIndex).dayEnding);
    HashSet<String> insulinUsed = new HashSet<>();
    StringBuilder insulinUsedStringBuilder = new StringBuilder();
    for (DataEntry dataEntry : data)
    {
      if (dataEntry.insulinName != null)
      {
        String insulinName = dataEntry.insulinName.trim();
        if (!insulinName.equals(context.getString(R.string.not_applicable)) && !insulinUsed.contains(dataEntry.insulinName))
        {
          insulinUsed.add(dataEntry.insulinName);
          insulinUsedStringBuilder.append(dataEntry.insulinName);
          insulinUsedStringBuilder.append(", ");
        }
      }
    }
    String insulinUsedString = insulinUsedStringBuilder.substring(0, insulinUsedStringBuilder.length() - 2);

    // Headers.
    float priorHeight = height;
    float afterHeight = height - (FONT_SIZE_MEDIUM * 2.2f) - LINE_SPACING;
    drawBox(BORDER, priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), afterHeight, BLUE, null);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), priorHeight, m_writableWidth + BORDER, afterHeight, BLUE, null);

    float centeredHeaderHeight = height - (FONT_SIZE_MEDIUM * 1.1f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.insulin_details_header), 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 3.0f), centeredHeaderHeight, NORMAL_COLUMN_WIDTH * 4.0f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.blood_glucose_levels_header), 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), centeredHeaderHeight - (FONT_SIZE_MEDIUM * 0.5f), NORMAL_COLUMN_WIDTH * 8.0f);
    float remainingSpace = m_writableWidth - (NORMAL_COLUMN_WIDTH * 13.0f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.key_events_header), 0.0f, (BORDER + m_writableWidth) - (remainingSpace * 0.5f), centeredHeaderHeight, remainingSpace);
    height = afterHeight;

    // Sub-headers
    float SUBHEADING_LENGTH = 72.0f;
    priorHeight = height;
    afterHeight = height - SUBHEADING_LENGTH - 4.0f;
    height -= 2.0f + (SUBHEADING_LENGTH * 0.5f);
    drawBox(BORDER, priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 6.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 6.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 7.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 7.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 8.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 8.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 10.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 10.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 11.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 11.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 12.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 12.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), afterHeight, BLUE, null);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), priorHeight, m_writableWidth + BORDER, afterHeight, BLUE, null);

    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, context.getString(R.string.date), 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 0.6f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, insulinUsedString, 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 3.0f), height, NORMAL_COLUMN_WIDTH * 4.0f);

    String[] eventStrings = context.getResources().getStringArray(R.array.nhs_event_names);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[0], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 5.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[1], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 6.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[2], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 7.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[3], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 8.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[4], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 9.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[5], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 10.45f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[6], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 11.6f), height, SUBHEADING_LENGTH);
    drawCenteredTextParagraphed(FONT, FONT_SIZE_MEDIUM, eventStrings[7], 90.0f, BORDER + (NORMAL_COLUMN_WIDTH * 12.6f), height, SUBHEADING_LENGTH);

    height = afterHeight;

    // Dose and BGL
    float ROW_HEIGHT = 32.0f;
    for (int dayIndex = startIndex; dayIndex <= endIndex; dayIndex++)
    {
      priorHeight = height;
      afterHeight = height - ROW_HEIGHT;
      float halfHeight = height - (ROW_HEIGHT * 0.5f);

      drawBox(BORDER, priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 6.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 6.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 7.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 7.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 8.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 8.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 10.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 10.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 11.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 11.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 12.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 12.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), afterHeight, WHITE, BLUE);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 2.0f), afterHeight, BLUE, null);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 2.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 3.0f), afterHeight, BLUE, null);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 3.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 4.0f), afterHeight, BLUE, null);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 4.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), afterHeight, BLUE, null);
      drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), priorHeight, m_writableWidth + BORDER, afterHeight, BLUE, null);

      Date date = new Date(m_days.get(dayIndex).dayBeginning);
      String dateString = new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date);
      drawTextCentered(FONT, FONT_SIZE_SMALL, dateString, 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 0.5f), halfHeight);

      StringBuilder additionalNotesStringBuilder = new StringBuilder();

      int doseColumnIndex = 0;
      for (DataEntry entry : m_days.get(dayIndex).entries)
      {
        if (entry.insulinDose > 0)
        {
          date = new Date(entry.actualTimestamp);
          String timeString = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
          String doseTimeString = String.format(Locale.getDefault(), "\n%d\n\n%s", entry.insulinDose, timeString);
          drawCenteredTextParagraphed(FONT, FONT_SIZE_SMALL, doseTimeString, 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * (doseColumnIndex + 1.5f)), height - (ROW_HEIGHT * 0.25f), NORMAL_COLUMN_WIDTH);
          drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_SMALL, "Dose:\n\nTime:\n", 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * (doseColumnIndex + 1.5f)), height - (ROW_HEIGHT * 0.25f), NORMAL_COLUMN_WIDTH);
          doseColumnIndex++;
        }

        int eventColumn = getEventIndex(eventStrings, entry.event);
        if (eventColumn != -1)
        {
          drawTextCentered(FONT, FONT_SIZE_MEDIUM, String.valueOf(entry.bloodGlucoseLevel), 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * (eventColumn + 5.5f)), halfHeight);
        }

        String additionalNotes = entry.additionalNotes.trim();
        if (additionalNotes.length() > 0)
        {
          additionalNotesStringBuilder.append(additionalNotes);
          additionalNotesStringBuilder.append(" | ");
        }
      }

      if (additionalNotesStringBuilder.length() > 0)
      {
        String additionalNotesString = additionalNotesStringBuilder.substring(0, additionalNotesStringBuilder.length() - 3);
        drawTextParagraphed(FONT, FONT_SIZE_TINY, additionalNotesString, BORDER + (NORMAL_COLUMN_WIDTH * 13.0f) + LINE_SPACING, BORDER + m_writableWidth - LINE_SPACING, priorHeight, afterHeight);
      }

      height = afterHeight;
    }

    drawBox(BORDER, boxStartHeight, m_writableWidth + BORDER, height, BLUE, null);

    // Bottom Text
    height -= FONT_SIZE_LARGE;
    drawTextParagraphed(FONT, 6.0f, context.getString(R.string.bottom_text_a), BORDER + NORMAL_COLUMN_WIDTH, BORDER + (NORMAL_COLUMN_WIDTH * 4.0f), height, BORDER);
    drawTextParagraphed(FONT, 6.0f, context.getString(R.string.bottom_text_b), BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), BORDER + (NORMAL_COLUMN_WIDTH * 8.0f), height, BORDER);
    drawTextParagraphed(FONT, 6.0f, context.getString(R.string.bottom_text_c), BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), BORDER + (NORMAL_COLUMN_WIDTH * 12.0f), height, BORDER);
    drawTextParagraphed(FONT, 6.0f, context.getString(R.string.bottom_text_d), BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), BORDER + m_writableWidth, height, BORDER);
  }

  private static int getEventIndex(String[] eventStrings, String event)
  {
    int eventIndex = -1;
    for (int i = 0; (i < eventStrings.length) && (eventIndex == -1); i++)
    {
      if (eventStrings[i].equals(event))
      {
        eventIndex = i;
      }
    }
    return eventIndex;
  }
}
