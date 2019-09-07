package com.voxtric.diabetescontrol.exporting;

public class NhsExporter extends PdfExporter
{
  @Override
  public String getFormatName()
  {
    return "NHS";
  }

  @Override
  public byte[] createPDF(ExportForegroundService exportForegroundService)
  {
    return new byte[0];
  }
}
