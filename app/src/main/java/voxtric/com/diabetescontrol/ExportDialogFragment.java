package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;

public class ExportDialogFragment extends DialogFragment
{
    public static final String TAG = "ExportDialogFragment";

    private String m_title = null;
    private String m_message = null;
    private int m_stage = 0;

    private boolean m_exportStarted = false;
    private long m_startTimeStamp = -1;
    private long m_endTimeStamp = -1;

    private long m_withinTimePeriodStartTimeStamp = -1;
    private long m_withinTimePeriodEndTimeStamp = -1;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        if (savedInstanceState != null)
        {
            m_title = savedInstanceState.getString("title");
            m_message = savedInstanceState.getString("message");
            m_stage = savedInstanceState.getInt("stage");

            m_exportStarted = savedInstanceState.getBoolean("export_started");
            m_startTimeStamp = savedInstanceState.getLong("start_time_stamp");
            m_endTimeStamp = savedInstanceState.getLong("end_time_stamp");

            m_withinTimePeriodStartTimeStamp = savedInstanceState.getLong("start_time_stamp");
            m_withinTimePeriodEndTimeStamp = savedInstanceState.getLong("end_time_stamp");
        }

        Activity activity = getActivity();
        final AlertDialog alertDialog;
        if (m_stage == 0)
        {
            alertDialog = getFirstStageDialog(activity);
        }
        else
        {
            if (!m_exportStarted && activity instanceof DatabaseActivity)
            {
                performExport(activity, ((DatabaseActivity)activity).getDatabase());
            }
            alertDialog = getSecondStageDialog(activity);
        }
        return alertDialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("title", m_title);
        outState.putString("message", m_message);
        outState.putInt("stage", m_stage);

        outState.putBoolean("export_started", m_exportStarted);
        outState.putLong("start_time_stamp", m_startTimeStamp);
        outState.putLong("end_time_stamp", m_endTimeStamp);

        outState.putLong("within_time_period_start_time_stamp", m_withinTimePeriodStartTimeStamp);
        outState.putLong("within_time_period_end_time_stamp", m_withinTimePeriodEndTimeStamp);
    }

    public void setText(String title, String message)
    {
        m_title = title;
        m_message = message;
    }

    private AlertDialog getFirstStageDialog(Activity activity)
    {
        final View view = View.inflate(activity, R.layout.dialog_choose_export_duration, null);
        final Button startDateButton = view.findViewById(R.id.button_start_date);
        final Button endDateButton = view.findViewById(R.id.button_end_date);
        initialiseDateButton(startDateButton);
        initialiseDateButton(endDateButton);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        final long lastExportTimeStamp = preferences.getLong("last_export_time_stamp", -1);
        if (lastExportTimeStamp == -1)
        {
            view.findViewById(R.id.radio_button_since_last_export).setEnabled(false);
        }
        int lastChosen = preferences.getInt("last_export_duration_chosen", 0);
        switch (lastChosen)
        {
        default:
        case 0:
            ((RadioButton)view.findViewById(R.id.radio_button_all_recorded)).setChecked(true);
            break;
        case 1:
            ((RadioButton)view.findViewById(R.id.radio_button_since_last_export)).setChecked(true);
            break;
        case 2:
            ((RadioButton)view.findViewById(R.id.radio_button_within_time_period)).setChecked(true);
            break;
        }
        boolean enableWithinTimePeriod = lastChosen == 2;
        view.findViewById(R.id.text_view_start_date).setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
        view.findViewById(R.id.text_view_end_date).setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
        view.findViewById(R.id.button_start_date).setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
        view.findViewById(R.id.button_end_date).setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
        startDateButton.setEnabled(enableWithinTimePeriod);
        endDateButton.setEnabled(enableWithinTimePeriod);
        ((RadioButton)view.findViewById(R.id.radio_button_within_time_period)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                view.findViewById(R.id.text_view_start_date).setAlpha(isChecked ? 1.0f : 0.3f);
                view.findViewById(R.id.text_view_end_date).setAlpha(isChecked ? 1.0f : 0.3f);
                view.findViewById(R.id.button_start_date).setAlpha(isChecked ? 1.0f : 0.3f);
                view.findViewById(R.id.button_end_date).setAlpha(isChecked ? 1.0f : 0.3f);
                startDateButton.setEnabled(isChecked);
                endDateButton.setEnabled(isChecked);
            }
        });

        return new AlertDialog.Builder(activity)
                .setTitle("Export data")
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        AsyncTask.execute(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                preferences.edit().putLong("last_export_time_stamp", System.currentTimeMillis()).apply();
                                int selectedID = ((RadioGroup)view.findViewById(R.id.radio_group_duration)).getCheckedRadioButtonId();
                                if (selectedID == view.findViewById(R.id.radio_button_all_recorded).getId())
                                {
                                    preferences.edit().putInt("last_export_duration_chosen", 0).apply();
                                    m_startTimeStamp = 0;
                                    m_endTimeStamp = Long.MAX_VALUE;
                                }
                                else if (selectedID == view.findViewById(R.id.radio_button_since_last_export).getId())
                                {
                                    preferences.edit().putInt("last_export_duration_chosen", 1).apply();
                                    m_startTimeStamp = lastExportTimeStamp;
                                    m_endTimeStamp = System.currentTimeMillis();
                                }
                                else if (selectedID == view.findViewById(R.id.radio_button_within_time_period).getId())
                                {
                                    preferences.edit().putInt("last_export_duration_chosen", 2).apply();
                                    m_startTimeStamp = m_withinTimePeriodStartTimeStamp;
                                    m_endTimeStamp = m_withinTimePeriodEndTimeStamp;
                                }
                            }
                        });

                        m_stage = 1;
                        dismiss();
                        FragmentManager fragmentManager = getFragmentManager();
                        if (fragmentManager != null)
                        {
                            show(fragmentManager, TAG);
                        }
                    }
                })
                .create();
    }

    private AlertDialog getSecondStageDialog(Activity activity)
    {
        View view = View.inflate(activity, R.layout.dialog_export_pdf, null);
        ((TextView) view.findViewById(R.id.text_view_message)).setText(m_message);
        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setTitle(m_title)
                .setView(view)
                .setNegativeButton("Finish", null)
                .setPositiveButton("Share", null)
                .create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface dialog)
            {
                alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        return alertDialog;
    }

    private void initialiseDateButton(final Button dateButton)
    {
        dateButton.setTypeface(null, Typeface.NORMAL);
        dateButton.setAllCaps(false);
        dateButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final DatePicker datePicker = new DatePicker(getActivity());
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setView(datePicker)
                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(0);
                                calendar.set(Calendar.YEAR, datePicker.getYear());
                                calendar.set(Calendar.MONTH, datePicker.getMonth());
                                calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                                Date date = calendar.getTime();
                                String dateString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
                                dateButton.setText(dateString);

                                if (dateButton.getId() == R.id.button_start_date)
                                {
                                    m_withinTimePeriodStartTimeStamp = calendar.getTimeInMillis();
                                }
                                else
                                {
                                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                                    m_withinTimePeriodEndTimeStamp = calendar.getTimeInMillis() - 1;
                                }
                            }
                        })
                        .create();
                dialog.show();
            }
        });
    }

    private void performExport(final Activity activity, final AppDatabase database)
    {
        m_exportStarted = true;
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {
                List<DataEntry> entries = database.dataEntriesDao().findAllBetween(m_startTimeStamp, m_endTimeStamp);
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
                        ADSExporter exporter = new ADSExporter(entries, database);
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
                            directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                        }
                        else
                        {
                            directory = new File(Environment.getExternalStorageDirectory() + "/Documents");
                        }

                        if (!directory.exists() && !directory.mkdirs())
                        {
                            throw new IOException("Failed to find documents directory.");
                        }

                        File file = new File(directory, fileName);
                        OutputStream outputStream = new FileOutputStream(file);
                        byteArrayOutputStream.writeTo(outputStream);
                        outputStream.close();
                        activity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                dismiss();
                            }
                        });
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
                            android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(activity)
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
