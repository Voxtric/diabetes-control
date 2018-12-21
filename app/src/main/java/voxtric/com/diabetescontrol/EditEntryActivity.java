package voxtric.com.diabetescontrol;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;

public class EditEntryActivity extends DatabaseActivity
{
    private DataEntry m_entry = null;
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
                        m_entry = m_database.dataEntriesDao().getEntry(timeStamp).get(0);
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
                fragment.setValues(m_entry, this);
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
        switch (item.getItemId())
        {
        case android.R.id.home:
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
                NewEntryFragment fragment = (NewEntryFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_edit_entry);
                if (fragment != null)
                {
                    final DataEntry entry = fragment.createEntry(EditEntryActivity.this);
                    AsyncTask.execute(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            @StringRes final int messageID;
                            if (Math.abs(entry.timeStamp - m_entry.timeStamp) > 30000L ||
                                !entry.event.endsWith(m_entry.event) ||
                                !entry.insulinName.equals(m_entry.insulinName) ||
                                !entry.insulinDose.equals(m_entry.insulinDose) ||
                                entry.bloodGlucoseLevel != m_entry.bloodGlucoseLevel ||
                                !entry.foodEaten.equals(m_entry.foodEaten) ||
                                !entry.additionalNotes.equals(m_entry.additionalNotes))
                            {
                                m_database.dataEntriesDao().delete(m_entry);
                                m_database.dataEntriesDao().insert(entry);
                                messageID = R.string.changes_saved_message;
                                setResult(EntryListFragment.RESULT_LIST_UPDATE_NEEDED);
                            }
                            else
                            {
                                messageID = R.string.changes_not_saved_message;
                            }
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Toast.makeText(EditEntryActivity.this, messageID, Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            });
                        }
                    });
                }
            }
        });
    }
}
