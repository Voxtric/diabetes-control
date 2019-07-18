package voxtric.com.diabetescontrol;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.DatabaseActivity;
import voxtric.com.diabetescontrol.database.Food;

public class EditEntryActivity extends DatabaseActivity
{
  private DataEntry m_editedEntry = null;
  private List<Food> m_editedFoods = null;

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
      final long timestamp = extras.getLong("timestamp", 0);
      if (timestamp != 0)
      {
        AsyncTask.execute(new Runnable()
        {
          @Override
          public void run()
          {
            m_editedEntry = m_database.dataEntriesDao().getEntry(timestamp);
            m_editedFoods = m_database.foodsDao().getFoods(timestamp);
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
        fragment.setValues(this, m_editedEntry, m_editedFoods);
      }
    }
    catch (InterruptedException exception)
    {
      exception.printStackTrace();
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
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
          final List<Food> foodList = newEntryFragment.createFoodList(EditEntryActivity.this, entry);
          AsyncTask.execute(new Runnable()
          {
            @Override
            public void run()
            {
              newEntryFragment.checkDateMismatch(EditEntryActivity.this, entry, m_editedEntry, foodList);
            }
          });
        }
      }
    });
  }
}
