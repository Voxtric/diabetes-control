package com.voxtric.diabetescontrol.exporting;

import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntry;

import java.util.List;

public abstract class PdfExporter extends PdfGenerator implements IExporter
{
  List<Week> m_weeks = null;

  @Override
  public byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService)
  {
    m_weeks = Week.splitEntries(entries, AppDatabase.getInstance().eventsDao());
    return createPDF(exportForegroundService);
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
}
