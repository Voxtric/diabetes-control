package com.voxtric.diabetescontrol.exporting;

import java.util.List;

import com.voxtric.diabetescontrol.database.DataEntry;

public interface IExporter
{
  byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService);
  String getFormatName();
  String getFileExtension();
  String getFileMimeType();
}
