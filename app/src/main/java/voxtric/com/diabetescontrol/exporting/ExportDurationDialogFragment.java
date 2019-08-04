package voxtric.com.diabetescontrol.exporting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import voxtric.com.diabetescontrol.R;

public class ExportDurationDialogFragment extends DialogFragment
{
  public static final String TAG = "ExportDurationDialogFragment";

  private AlertDialog m_alertDialog = null;
  private Button m_startDateButton = null;
  private Button m_endDateButton = null;

  private Intent m_exportIntent;

  private long m_withinTimePeriodStartTimeStamp = -1;
  private long m_withinTimePeriodEndTimeStamp = -1;

  public ExportDurationDialogFragment(Intent exportIntent)
  {
    m_exportIntent = exportIntent;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      m_exportIntent = savedInstanceState.getParcelable("export_intent");
      m_withinTimePeriodStartTimeStamp = savedInstanceState.getLong("start_time_stamp");
      m_withinTimePeriodEndTimeStamp = savedInstanceState.getLong("end_time_stamp");
    }

    final Activity activity = getActivity();
    final View view = View.inflate(activity, R.layout.dialog_export_duration, null);
    m_startDateButton = view.findViewById(R.id.button_start_date);
    m_endDateButton = view.findViewById(R.id.button_end_date);
    initialiseDateButton(m_startDateButton);
    initialiseDateButton(m_endDateButton);

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
    m_startDateButton.setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
    m_startDateButton.setEnabled(enableWithinTimePeriod);

    view.findViewById(R.id.text_view_end_date).setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
    m_endDateButton.setAlpha(enableWithinTimePeriod ? 1.0f : 0.3f);
    m_endDateButton.setEnabled(enableWithinTimePeriod);

    m_alertDialog = new AlertDialog.Builder(activity)
        .setTitle(R.string.title_export_data)
        .setView(view)
        .setNegativeButton(R.string.cancel_dialog_option, null)
        .setPositiveButton(R.string.export_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            preferences.edit().putLong("last_export_time_stamp", System.currentTimeMillis()).apply();

            int selectedID = ((RadioGroup)view.findViewById(R.id.radio_group_duration)).getCheckedRadioButtonId();
            if (selectedID == view.findViewById(R.id.radio_button_all_recorded).getId())
            {
              preferences.edit().putInt("last_export_duration_chosen", 0).apply();
              m_exportIntent.putExtra("export_start", 0L);
              m_exportIntent.putExtra("export_end", Long.MAX_VALUE);
            }
            else if (selectedID == view.findViewById(R.id.radio_button_since_last_export).getId())
            {
              preferences.edit().putInt("last_export_duration_chosen", 1).apply();
              m_exportIntent.putExtra("export_start", lastExportTimeStamp);
              m_exportIntent.putExtra("export_end", Long.MAX_VALUE);
            }
            else if (selectedID == view.findViewById(R.id.radio_button_within_time_period).getId())
            {
              preferences.edit().putInt("last_export_duration_chosen", 2).apply();
              m_exportIntent.putExtra("export_start", m_withinTimePeriodStartTimeStamp);
              m_exportIntent.putExtra("export_end", m_withinTimePeriodEndTimeStamp);
            }

            activity.startService(m_exportIntent);
          }
        })
        .create();

    ((RadioButton)view.findViewById(R.id.radio_button_within_time_period)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
      {
        view.findViewById(R.id.text_view_start_date).setAlpha(isChecked ? 1.0f : 0.3f);
        m_startDateButton.setAlpha(isChecked ? 1.0f : 0.3f);
        m_startDateButton.setEnabled(isChecked);

        view.findViewById(R.id.text_view_end_date).setAlpha(isChecked ? 1.0f : 0.3f);
        m_endDateButton.setAlpha(isChecked ? 1.0f : 0.3f);
        m_endDateButton.setEnabled(isChecked);

        m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
            !isChecked || (m_startDateButton.getText().length() > 0 && m_endDateButton.getText().length() > 0));
      }
    });

    return m_alertDialog;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putParcelable("export_intent", m_exportIntent);
    outState.putLong("within_time_period_start_time_stamp", m_withinTimePeriodStartTimeStamp);
    outState.putLong("within_time_period_end_time_stamp", m_withinTimePeriodEndTimeStamp);
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
            .setPositiveButton(R.string.done_dialog_option, new DialogInterface.OnClickListener()
            {
              @Override
              public void onClick(DialogInterface dialog, int which)
              {
                Calendar calendar = Calendar.getInstance();
                calendar.clear();
                calendar.set(
                    calendar.getMinimum(Calendar.YEAR),
                    calendar.getMinimum(Calendar.MONTH),
                    calendar.getMinimum(Calendar.DATE),
                    calendar.getMinimum(Calendar.HOUR_OF_DAY),
                    calendar.getMinimum(Calendar.MINUTE),
                    calendar.getMinimum(Calendar.SECOND));
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

                m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
                    m_startDateButton.getText().length() > 0 && m_endDateButton.getText().length() > 0);
              }
            })
            .create();
        dialog.show();
      }
    });
  }
}
