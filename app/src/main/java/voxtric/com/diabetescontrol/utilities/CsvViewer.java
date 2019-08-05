package voxtric.com.diabetescontrol.utilities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import voxtric.com.diabetescontrol.R;

public class CsvViewer extends AppCompatActivity
{
  private static final String TAG = "CsvViewer";

  private TextView m_fileContent;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_csv_viewer);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    m_fileContent = findViewById(R.id.file_content);
    m_fileContent.setHorizontallyScrolling(true);

    Uri fileUri = getIntent().getData();
    if (fileUri != null)
    {
      try
      {
        InputStream inputStream = getContentResolver().openInputStream(fileUri);
        if (inputStream != null)
        {
          InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
          BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

          StringBuilder fileContentBuilder = new StringBuilder();
          String line = bufferedReader.readLine();
          while (line != null)
          {
            fileContentBuilder.append(line);
            fileContentBuilder.append('\n');
            line = bufferedReader.readLine();
          }
          m_fileContent.setText(fileContentBuilder);
          Log.e(TAG, fileContentBuilder.toString());
        }
        else
        {
          Log.e(TAG, "CSV Read couldn't open input stream");
        }
      }
      catch (FileNotFoundException exception)
      {
        Log.e(TAG, "CSV Read File Not Found Exception", exception);
      }
      catch (IOException exception)
      {
        Log.e(TAG, "CSV Read IO Exception", exception);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      finish();
      return true;
    }
    return false;
  }
}
