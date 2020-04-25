package com.voxtric.diabetescontrol.exporting;

import android.annotation.SuppressLint;
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
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import com.voxtric.diabetescontrol.MainActivity;
import com.voxtric.diabetescontrol.R;
import com.voxtric.diabetescontrol.database.Preference;

public class ExportDurationDialogFragment extends DialogFragment
{
  public static final String TAG = "ExportDurationDialogFragment";

  private AlertDialog m_alertDialog = null;
  private Button m_startDateButton = null;
  private Button m_endDateButton = null;

  private Intent m_exportIntent;
  private long m_lastExportTimestamp;

  private long m_withinTimePeriodStartTimeStamp = -1;
  private long m_withinTimePeriodEndTimeStamp = -1;

  public ExportDurationDialogFragment(Intent exportIntent, long lastExportTimestamp)
  {
    m_exportIntent = exportIntent;
    m_lastExportTimestamp = lastExportTimestamp;
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

    final MainActivity activity = (MainActivity)getActivity();
    if (activity != null)
    {
      final View view = View.inflate(activity, R.layout.dialog_export_duration, null);
      m_startDateButton = view.findViewById(R.id.button_start_date);
      m_endDateButton = view.findViewById(R.id.button_end_date);
      initialiseDateButton(m_startDateButton);
      initialiseDateButton(m_endDateButton);

      if (m_lastExportTimestamp == -1)
      {
        view.findViewById(R.id.radio_button_since_last_export).setEnabled(false);
      }

      final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
      int lastChosen = preferences.getInt("last_export_duration_chosen", 0);
      if ((lastChosen == 1) && (m_lastExportTimestamp == -1))
      {
        lastChosen = 0;
      }
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
              int exportType = m_exportIntent.getIntExtra("export_type", -1);
              @SuppressLint("DefaultLocale") final String lastExportTimestampPreference = String.format("last_%d_export_timestamp", exportType);
              Preference.put(lastExportTimestampPreference, String.valueOf(System.currentTimeMillis()));

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
                m_exportIntent.putExtra("export_start", m_lastExportTimestamp);
                m_exportIntent.putExtra("export_end", Long.MAX_VALUE);
              }
              else if (selectedID == view.findViewById(R.id.radio_button_within_time_period).getId())
              {
                preferences.edit().putInt("last_export_duration_chosen", 2).apply();
                m_exportIntent.putExtra("export_start", m_withinTimePeriodStartTimeStamp);
                m_exportIntent.putExtra("export_end", m_withinTimePeriodEndTimeStamp);
              }

              @StringRes int titleId;
              switch (exportType)
              {
              case R.id.navigation_export_nhs:
                titleId = R.string.exporting_nhs_notification_title;
                break;
              case R.id.navigation_export_ads:
                titleId = R.string.exporting_ads_notification_title;
                break;
              case R.id.navigation_export_csv:
                titleId = R.string.exporting_csv_notification_title;
                break;
              default:
                titleId = R.string.title_undefined;
              }

              activity.startService(m_exportIntent);
              activity.launchExportProgressDialog(getString(titleId));
            }
          })
          .create();

      RadioButton withinTimePeriodRadioButton = view.findViewById(R.id.radio_button_within_time_period);
      withinTimePeriodRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean checked)
        {
          view.findViewById(R.id.text_view_start_date).setAlpha(checked ? 1.0f : 0.3f);
          m_startDateButton.setAlpha(checked ? 1.0f : 0.3f);
          m_startDateButton.setEnabled(checked);

          view.findViewById(R.id.text_view_end_date).setAlpha(checked ? 1.0f : 0.3f);
          m_endDateButton.setAlpha(checked ? 1.0f : 0.3f);
          m_endDateButton.setEnabled(checked);

          m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!checked || (m_startDateButton.getText().length() > 0 && m_endDateButton.getText().length() > 0));
        }
      });
    }

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

  public void initialiseExportButton()
  {
    if (m_alertDialog != null)
    {
      Button button = m_alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
      if (button != null)
      {
        button.setEnabled(!((RadioButton)m_alertDialog.findViewById(R.id.radio_button_within_time_period)).isChecked());
      }
    }
  }
}
