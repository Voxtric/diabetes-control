package voxtric.com.diabetescontrol.exporting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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

  private String m_title = null;
  private String m_startMessage = null;
  private String m_endMessage = null;

  private long m_withinTimePeriodStartTimeStamp = -1;
  private long m_withinTimePeriodEndTimeStamp = -1;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    if (savedInstanceState != null)
    {
      m_title = savedInstanceState.getString("title");
      m_startMessage = savedInstanceState.getString("start_message");
      m_endMessage = savedInstanceState.getString("end_message");

      m_withinTimePeriodStartTimeStamp = savedInstanceState.getLong("start_time_stamp");
      m_withinTimePeriodEndTimeStamp = savedInstanceState.getLong("end_time_stamp");
    }

    Activity activity = getActivity();
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
        .setTitle("Export data")
        .setView(view)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.next, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            preferences.edit().putLong("last_export_time_stamp", System.currentTimeMillis()).apply();
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null)
            {
              ExportDialogFragment exportDialog = new ExportDialogFragment();
              exportDialog.setText(m_title, m_startMessage, m_endMessage);
              int selectedID = ((RadioGroup)view.findViewById(R.id.radio_group_duration)).getCheckedRadioButtonId();
              if (selectedID == view.findViewById(R.id.radio_button_all_recorded).getId())
              {
                preferences.edit().putInt("last_export_duration_chosen", 0).apply();
                exportDialog.setTime(0, Long.MAX_VALUE);
              }
              else if (selectedID == view.findViewById(R.id.radio_button_since_last_export).getId())
              {
                preferences.edit().putInt("last_export_duration_chosen", 1).apply();
                exportDialog.setTime(lastExportTimeStamp, System.currentTimeMillis());
              }
              else if (selectedID == view.findViewById(R.id.radio_button_within_time_period).getId())
              {
                preferences.edit().putInt("last_export_duration_chosen", 2).apply();
                exportDialog.setTime(m_withinTimePeriodStartTimeStamp, m_withinTimePeriodEndTimeStamp);
              }
              exportDialog.showNow(fragmentManager, ExportDialogFragment.TAG);
            }
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
    outState.putString("title", m_title);
    outState.putString("start_message", m_startMessage);
    outState.putString("end_message", m_endMessage);

    outState.putLong("within_time_period_start_time_stamp", m_withinTimePeriodStartTimeStamp);
    outState.putLong("within_time_period_end_time_stamp", m_withinTimePeriodEndTimeStamp);
  }

  public void setText(String title, String startMessage, String endMessage)
  {
    m_title = title;
    m_startMessage = startMessage;
    m_endMessage = endMessage;
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
