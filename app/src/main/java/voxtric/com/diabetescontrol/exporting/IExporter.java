package voxtric.com.diabetescontrol.exporting;

public interface IExporter
{
  byte[] export(ExportForegroundService exportForegroundService);
  String getFormatName();
  String getFileExtension();
  String getFileMimeType();
}
