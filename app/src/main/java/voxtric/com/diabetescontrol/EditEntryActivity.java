package voxtric.com.diabetescontrol;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.concurrent.CountDownLatch;

import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;

public class EditEntryActivity extends DatabaseActivity
{
  private DataEntry m_editedEntry = null;
  private CountDownLatch m_latch = new CountDownLatch(1);

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_entry);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    Bundle extras = getIntent().getExtras();
    if (extras != null)
    {
      final long timeStamp = extras.getLong("time_stamp", 0);
      if (timeStamp != 0)
      {
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {
            m_editedEntry = m_database.dataEntriesDao().getEntry(timeStamp);
            m_latch.countDown();
          }
        });
      }
    }

    modifyAddNewButton();
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    try
    {
      m_latch.await();
      NewEntryFragment fragment = (NewEntryFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_edit_entry);
      if (fragment != null)
      {
        fragment.setValues(m_editedEntry, this);
      }
    }
    catch (InterruptedException exception)
    {
      exception.printStackTrace();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      checkEditCancel();
      return true;
    }
    return false;
  }

  @Override
  public void onBackPressed()
  {
    checkEditCancel();
  }

  private void checkEditCancel()
  {
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(R.string.title_cancel_entry_edit)
        .setMessage(R.string.message_cancel_entry_edit)
        .setNegativeButton(R.string.continue_edit, null)
        .setPositiveButton(R.string.cancel_edit, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            finish();
          }
        })
        .create();
    dialog.show();
  }

  private void modifyAddNewButton()
  {
    Button button = findViewById(R.id.button_add_new_entry);
    button.setText(R.string.save_changes_text);
    button.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        final NewEntryFragment newEntryFragment = (NewEntryFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_edit_entry);
        if (newEntryFragment != null)
        {
          final DataEntry entry = newEntryFragment.createEntry(EditEntryActivity.this);
          AsyncTask.execute(new Runnable()
          {
            @Override
            public void run()
            {
              newEntryFragment.checkDateMismatch(EditEntryActivity.this, entry, m_editedEntry);
            }
          });
        }

        /*NewEntryFragment fragment = (NewEntryFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_edit_entry);
        if (fragment != null)
        {
          final DataEntry entry = fragment.createEntry(EditEntryActivity.this);
          if (Math.abs(entry.actualTimestamp - m_entry.actualTimestamp) > 30000L ||
              !entry.event.endsWith(m_entry.event) ||
              !entry.insulinName.equals(m_entry.insulinName) ||
              !entry.insulinDose.equals(m_entry.insulinDose) ||
              entry.bloodGlucoseLevel != m_entry.bloodGlucoseLevel ||
              !entry.foodEaten.equals(m_entry.foodEaten) ||
              !entry.additionalNotes.equals(m_entry.additionalNotes))
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                boolean changesSaved = false;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(entry.actualTimestamp);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                DataEntry previousEntry = m_database.dataEntriesDao().findFirstBefore(calendar.getTimeInMillis() - 1, entry.event, m_entry.actualTimestamp);
                if (previousEntry == null)
                {
                  m_database.dataEntriesDao().delete(m_entry);
                  m_database.dataEntriesDao().insert(entry);
                  changesSaved = true;
                  setResult(EntryListFragment.RESULT_LIST_UPDATE_NEEDED);
                }
                else
                {
                  calendar.setTimeInMillis(entry.actualTimestamp);
                  Calendar previousCalendar = Calendar.getInstance();
                  previousCalendar.setTimeInMillis(previousEntry.actualTimestamp);
                  if (calendar.get(Calendar.YEAR) != previousCalendar.get(Calendar.YEAR) ||
                      calendar.get(Calendar.MONTH) != previousCalendar.get(Calendar.MONTH) ||
                      calendar.get(Calendar.DAY_OF_MONTH) != previousCalendar.get(Calendar.DAY_OF_MONTH))
                  {
                    m_database.dataEntriesDao().delete(m_entry);
                    m_database.dataEntriesDao().insert(entry);
                    changesSaved = true;
                    setResult(EntryListFragment.RESULT_LIST_UPDATE_NEEDED);
                  }
                }

                final boolean finalChangesSaved = changesSaved;
                runOnUiThread(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    if (finalChangesSaved)
                    {
                      Toast.makeText(EditEntryActivity.this, R.string.changes_saved_message, Toast.LENGTH_LONG).show();
                      finish();
                    }
                    else
                    {
                      AlertDialog dialog = new AlertDialog.Builder(EditEntryActivity.this)
                          .setTitle(R.string.title_event_collision)
                          .setMessage(R.string.message_event_collision)
                          .setPositiveButton(R.string.ok, null)
                          .create();
                      dialog.show();
                    }
                  }
                });
              }
            });
          }
          else
          {
            Toast.makeText(EditEntryActivity.this, R.string.changes_not_saved_message, Toast.LENGTH_LONG).show();
            finish();
          }
        }*/
      }
    });
  }
}
