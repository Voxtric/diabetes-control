package voxtric.com.diabetescontrol.exporting;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import voxtric.com.diabetescontrol.database.DataEntry;

public class CsvExporter implements IExporter
{
  private StringBuilder m_fileContents = new StringBuilder();

  @Override
  public byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService)
  {
    for (int i = entries.size() - 1; i >= 0; i--)
    {
      addEntry(entries.get(i));
    }
    int lastCommaIndex = m_fileContents.lastIndexOf(",");
    m_fileContents.replace(lastCommaIndex, lastCommaIndex + 1, "");
    return m_fileContents.toString().trim().getBytes();
  }

  @Override
  public String getFormatName()
  {
    return "CSV";
  }

  @Override
  public String getFileExtension()
  {
    return "csv";
  }

  @Override
  public String getFileMimeType()
  {
    return "text/csv";
  }

  private void addEntry(DataEntry entry)
  {
    Date date = new Date(entry.actualTimestamp);

    append(new SimpleDateFormat("EEEE", Locale.getDefault()).format(date));

    append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(date));

    append(DateFormat.getTimeInstance(DateFormat.SHORT).format(date));

    append(entry.bloodGlucoseLevel);

    append(entry.insulinName);

    append(entry.insulinDose);

    append(entry.additionalNotes);

    int lastCommaIndex = m_fileContents.lastIndexOf(",");
    m_fileContents.replace(lastCommaIndex, lastCommaIndex + 1, "\n");
  }

  private void append(String string)
  {
    if (string != null && !string.isEmpty())
    {
      m_fileContents.append(String.format("\"%s\",", string));
    }
    else
    {
      m_fileContents.append(',');
    }
  }

  private void append(float value)
  {
    m_fileContents.append(String.format(Locale.getDefault(), "%.1f,", value));
  }

  private void append(int value)
  {
    m_fileContents.append(String.format(Locale.getDefault(), "%d,", value));
  }
}
