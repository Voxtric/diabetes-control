package voxtric.com.diabetescontrol.exporting;

import java.util.List;

import voxtric.com.diabetescontrol.database.DataEntry;

public interface IExporter
{
  byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService);
  String getFormatName();
  String getFileExtension();
  String getFileMimeType();
}
