package voxtric.com.diabetescontrol.exporting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import voxtric.com.diabetescontrol.R;
import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.exporting.ADSExporter;

public class ExportDialogFragment extends DialogFragment
{
  public static final String TAG = "ExportDialogFragment";

  private AlertDialog m_alertDialog = null;
  private ProgressBar m_progressBar = null;

  private String m_title = null;
  private String m_startMessage = null;
  private String m_endMessage = null;

  private long m_startTimeStamp = -1;
  private long m_endTimeStamp = -1;

  private boolean m_exportStarted = false;
  private boolean m_exportFinished = false;

  private File m_file = null;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      m_title = savedInstanceState.getString("title");
      m_startMessage = savedInstanceState.getString("start_message");
      m_endMessage = savedInstanceState.getString("end_message");

      m_startTimeStamp = savedInstanceState.getLong("start_time_stamp");
      m_endTimeStamp = savedInstanceState.getLong("end_time_stamp");

      m_exportStarted = savedInstanceState.getBoolean("export_started");
      m_exportFinished = savedInstanceState.getBoolean("export_finished");
    }

    final Activity activity = getActivity();
    View view = View.inflate(activity, R.layout.dialog_export_pdf, null);
    ((TextView) view.findViewById(R.id.text_view_message)).setText(m_startMessage);
    m_progressBar = view.findViewById(R.id.progress_bar_export);
    m_alertDialog = new AlertDialog.Builder(activity)
        .setTitle(m_title)
        .setView(view)
        .setNegativeButton("Finish", null)
        .setPositiveButton("Share", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            if(m_file.exists() && activity != null)
            {
              intentShareFile.setType("application/pdf");
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
              {
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(m_file));
              }
              else
              {
                Uri uri = FileProvider.getUriForFile(activity,activity.getPackageName() + ".provider", m_file);
                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
                intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
              }
              intentShareFile.putExtra(Intent.EXTRA_SUBJECT, m_file.getName());
              intentShareFile.putExtra(Intent.EXTRA_TEXT, "Shared from 'Diabetes Control' app.");
              startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
          }
        })
        .create();
    m_alertDialog.setCancelable(m_exportFinished);
    m_alertDialog.setCanceledOnTouchOutside(m_exportFinished);
    m_alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface dialog)
      {
        m_alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(m_exportFinished);
        m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(m_exportFinished);
      }
    });

    if (savedInstanceState != null)
    {
      view.findViewById(R.id.progress_bar_export_indefinite).setVisibility(savedInstanceState.getInt("progress_bar_export_indefinite_visibility"));
      ((TextView)view.findViewById(R.id.text_view_message)).setText(savedInstanceState.getString("text_view_message_text"));
      m_progressBar.setMax(savedInstanceState.getInt("progress_bar_export_max"));
    }

    if (!m_exportStarted)
    {
      performExport(activity);
    }

    return m_alertDialog;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putString("title", m_title);
    outState.putString("start_message", m_startMessage);
    outState.putString("end_message", m_endMessage);

    outState.putLong("start_time_stamp", m_startTimeStamp);
    outState.putLong("end_time_stamp", m_endTimeStamp);

    outState.putBoolean("export_started", m_exportStarted);
    outState.putBoolean("export_finished", m_exportFinished);

    outState.putInt("progress_bar_export_indefinite_visibility", m_alertDialog.findViewById(R.id.progress_bar_export_indefinite).getVisibility());
    outState.putString("text_view_message_text", ((TextView)m_alertDialog.findViewById(R.id.text_view_message)).getText().toString());
    outState.putInt("progress_bar_export_max", m_progressBar.getMax());
  }

  public void setText(String title, String startMessage, String endMessage)
  {
    m_title = title;
    m_startMessage = startMessage;
    m_endMessage = endMessage;
  }

  void setTime(long startTimeStamp, long endTimeStamp)
  {
    m_startTimeStamp = startTimeStamp;
    m_endTimeStamp = endTimeStamp;
  }

  AlertDialog getAlertDialog()
  {
    return m_alertDialog;
  }

  private void finishExport(Activity activity, final long exportTime)
  {
    activity.runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        m_alertDialog.findViewById(R.id.progress_bar_export_indefinite).setVisibility(View.GONE);
        String durationString;
        if (exportTime > 1000)
        {
          durationString = String.format(Locale.getDefault(), "%.2fs", exportTime / 1000.0);
        }
        else
        {
          durationString = String.format(Locale.getDefault(), "%dms", exportTime);
        }
        ((TextView)m_alertDialog.findViewById(R.id.text_view_message)).setText(String.format("%s (%s)", m_endMessage, durationString));
        m_progressBar.setProgress(m_progressBar.getMax());

        m_exportFinished = true;
        m_alertDialog.setCancelable(true);
        m_alertDialog.setCanceledOnTouchOutside(true);
        m_alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
        m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
      }
    });
  }

  private void performExport(final Activity activity)
  {
    m_exportStarted = true;
    AsyncTask.execute(new Runnable()
    {
      @Override
      public void run()
      {
        long exportStart = System.currentTimeMillis();
        final List<DataEntry> entries = AppDatabase.getInstance().dataEntriesDao().findAllBetween(m_startTimeStamp, m_endTimeStamp);
        activity.runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            m_progressBar.setMax(entries.size());
          }
        });
        try
        {
          if (entries.isEmpty())
          {
            throw new Exception("No entries to be exported.");
          }

          String fileName;
          ByteArrayOutputStream byteArrayOutputStream;
          if (m_title.equals("ADS Export"))
          {
            ADSExporter exporter = new ADSExporter(entries);
            fileName = exporter.getFileName();
            byteArrayOutputStream = exporter.createPDF(activity);
          }
          else
          {
            throw new Exception("Unrecognised export format.");
          }

          if (byteArrayOutputStream != null)
          {
            File directory;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
            {
              directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Diabetes Control Exports");
            }
            else
            {
              directory = new File(Environment.getExternalStorageDirectory() + "/Documents/Diabetes Control Exports");
            }

            if (!directory.exists() && !directory.mkdirs())
            {
              throw new IOException("Failed to find documents directory.");
            }

            m_file = new File(directory, fileName);
            OutputStream outputStream = new FileOutputStream(m_file);
            byteArrayOutputStream.writeTo(outputStream);
            outputStream.close();
            long exportTime = System.currentTimeMillis() - exportStart;
            finishExport(activity, exportTime);
          }
        }
        catch (Exception exception)
        {
          Log.e("Export", "Export failed.", exception);
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              dismiss();
              AlertDialog alertDialog = new AlertDialog.Builder(activity)
                  .setTitle("Export Failed")
                  .setMessage("Exporting of your data failed.\n\nPlease ensure there is plenty of space on your device, and try again.")
                  .setPositiveButton(R.string.ok, null)
                  .create();
              alertDialog.show();
            }
          });
        }
      }
    });
  }
}
