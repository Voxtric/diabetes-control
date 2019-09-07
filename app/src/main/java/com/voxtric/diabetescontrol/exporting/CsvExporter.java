package com.voxtric.diabetescontrol.exporting;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.opencsv.CSVWriter;
import com.voxtric.diabetescontrol.database.AppDatabase;
import com.voxtric.diabetescontrol.database.DataEntriesDao;
import com.voxtric.diabetescontrol.database.DataEntry;
import com.voxtric.diabetescontrol.database.Food;

public class CsvExporter implements IExporter
{
  private static final String TAG = "CsvExporter";
  private static final String[] HEADINGS = new String[] {
      "Day", "Date", "Time", "Blood Glucose Level", "Insulin Name", "Insulin Dose", "Food Eaten", "Additional Notes"
  };

  private CSVWriter m_fileContents = null;

  @Override
  public byte[] export(List<DataEntry> entries, ExportForegroundService exportForegroundService)
  {
    try
    {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      m_fileContents = new CSVWriter(new OutputStreamWriter(outputStream), ',', '"', '"', "\n");

      m_fileContents.writeNext(HEADINGS, false);
      for (int i = entries.size() - 1; i >= 0; i--)
      {
        addEntry(entries.get(i));
      }

      m_fileContents.close();
      return outputStream.toByteArray();
    }
    catch (IOException exception)
    {
      Log.e(TAG, "CSV Export IO Exception", exception);
      return null;
    }
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

    List<Food> foods = AppDatabase.getInstance().foodsDao().getFoods(entry.actualTimestamp);
    StringBuilder foodStringBuilder = new StringBuilder();
    if (foods != null && !foods.isEmpty())
    {
      int foodCount = foods.size();
      foodStringBuilder.append(foods.get(0).name);
      for (int i = 1; i < foodCount; i++)
      {
        foodStringBuilder.append('\n');
        foodStringBuilder.append(foods.get(i).name);
      }
    }

    String[] line = new String[]{
        new SimpleDateFormat("EEEE", Locale.getDefault()).format(date),
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(date),
        DateFormat.getTimeInstance(DateFormat.SHORT).format(date),
        String.valueOf(entry.bloodGlucoseLevel),
        entry.insulinName,
        entry.insulinDose > 0 ? String.valueOf(entry.insulinDose) : "",
        foodStringBuilder.toString(),
        entry.additionalNotes
    };

    m_fileContents.writeNext(line, false);
  }
}
