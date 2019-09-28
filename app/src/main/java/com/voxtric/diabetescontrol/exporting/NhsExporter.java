package com.voxtric.diabetescontrol.exporting;

import android.content.Context;

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.TargetChange;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class NhsExporter extends PdfGenerator implements IExporter
{
  private static final PDRectangle DIMENSIONS = new PDRectangle(190.0f * PdfGenerator.MM_PER_INCH, 210.0f * PdfGenerator.MM_PER_INCH);
  private static final int MAX_DAYS_PER_PAGE = 11;

  private static final float FONT_SIZE_MEDIUM = 10.0f;

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
    final String[] DAYS = context.getResources().getStringArray(R.array.days);
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

    // Headers.
    float priorHeight = height;
    float afterHeight = height - (FONT_SIZE_MEDIUM * 2.2f) - LINE_SPACING;
    drawBox(BORDER, priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 1.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), afterHeight, BLUE, null);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 5.0f), priorHeight, BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), afterHeight, WHITE, BLUE);
    drawBox(BORDER + (NORMAL_COLUMN_WIDTH * 13.0f), priorHeight, m_writableWidth + BORDER, afterHeight, BLUE, null);

    float centeredHeaderHeight = height - (FONT_SIZE_MEDIUM * 1.1f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.insulin_details_header), 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 3.0f), centeredHeaderHeight, NORMAL_COLUMN_WIDTH * 4.0f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.blood_glucose_levels_header), 0.0f, BORDER + (NORMAL_COLUMN_WIDTH * 9.0f), centeredHeaderHeight - (FONT_SIZE_MEDIUM * 0.5f), NORMAL_COLUMN_WIDTH * 8.0f);
    float remainingSpace = m_writableWidth - (NORMAL_COLUMN_WIDTH * 13.0f);
    drawCenteredTextParagraphed(FONT_BOLD, FONT_SIZE_MEDIUM, context.getString(R.string.key_events_header), 0.0f, (BORDER + m_writableWidth) - (remainingSpace * 0.5f), centeredHeaderHeight, remainingSpace);
    height = afterHeight;




    drawBox(BORDER, boxStartHeight, m_writableWidth + BORDER, height, BLUE, null);
  }
}
