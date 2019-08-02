package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;
import voxtric.com.diabetescontrol.database.Preference;
import voxtric.com.diabetescontrol.settings.fragments.BGLHighlightingSettingsFragment;

public class EntryListFragment extends Fragment
{
  private static final int REQUEST_EDIT_ENTRY = 107;
  static final int RESULT_LIST_UPDATE_NEEDED = 108;

  static final int LOAD_COUNT = 100;
  private static final int LOAD_BOUNDARY = 10;

  private EntryListRecyclerViewAdapter m_adapter = null;

  public EntryListFragment()
  {
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.fragment_entry_list, container, false);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);

    final Activity activity = getActivity();
    if (activity != null)
    {
      final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_entry_list);
      final GridLayoutManager layoutManager = new GridLayoutManager(activity, 1);
      recyclerView.setLayoutManager(layoutManager);
      refreshEntryList();

      final FloatingActionButton backToTopButton = activity.findViewById(R.id.back_to_top_button);
      backToTopButton.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View view)
        {
          recyclerView.scrollToPosition(0);
        }
      });

      recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
      {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy)
        {
          super.onScrolled(recyclerView, dx, dy);
          if (recyclerView.canScrollVertically(-1))
          {
            backToTopButton.show();
          }
          else
          {
            backToTopButton.hide();
          }
        }
      });
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (requestCode == REQUEST_EDIT_ENTRY && resultCode == RESULT_LIST_UPDATE_NEEDED)
    {
      refreshEntryList();
    }
  }

  void refreshEntryList()
  {
    final MainActivity activity = (MainActivity)getActivity();
    if (activity != null && !RecoveryForegroundService.isDownloading())
    {
      final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_entry_list);
      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          final List<DataEntry> entries = AppDatabase.getInstance().dataEntriesDao().getPreviousEntries(Long.MAX_VALUE, LOAD_COUNT);
          activity.runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              if (entries.isEmpty())
              {
                recyclerView.setVisibility(View.GONE);
                activity.findViewById(R.id.text_view_no_data).setVisibility(View.VISIBLE);
              }
              else
              {
                recyclerView.setVisibility(View.VISIBLE);
                activity.findViewById(R.id.text_view_no_data).setVisibility(View.GONE);

                Preference.get(activity,
                    new String[] {
                        BGLHighlightingSettingsFragment.HIGHLIGHTING_ENABLED_PREFERENCE,
                        BGLHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE,
                        BGLHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE,
                        BGLHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE
                    },
                    new String[] {
                        String.valueOf(true),
                        String.valueOf(BGLHighlightingSettingsFragment.DEFAULT_VALUES.get(BGLHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE)),
                        String.valueOf(BGLHighlightingSettingsFragment.DEFAULT_VALUES.get(BGLHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE)),
                        String.valueOf(BGLHighlightingSettingsFragment.DEFAULT_VALUES.get(BGLHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE))
                    },
                    new Preference.ResultRunnable()
                    {
                      @Override
                      public void run()
                      {
                        float[] bglHighlightingValues = null;
                        HashMap<String, String> results = getResults();
                        if (Boolean.valueOf(results.get(BGLHighlightingSettingsFragment.HIGHLIGHTING_ENABLED_PREFERENCE)))
                        {
                          bglHighlightingValues = new float[3];
                          bglHighlightingValues[0] = Float.valueOf(results.get(BGLHighlightingSettingsFragment.IDEAL_MINIMUM_PREFERENCE));
                          bglHighlightingValues[1] = Float.valueOf(results.get(BGLHighlightingSettingsFragment.HIGH_MINIMUM_PREFERENCE));
                          bglHighlightingValues[2] = Float.valueOf(results.get(BGLHighlightingSettingsFragment.ACTION_REQUIRED_MINIMUM_PREFERENCE));
                        }

                        m_adapter = new EntryListRecyclerViewAdapter(activity, entries, bglHighlightingValues);
                        recyclerView.setAdapter(m_adapter);

                        final LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                        if (layoutManager != null)
                        {
                          recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener()
                          {
                            @Override
                            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy)
                            {
                              int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                              if (m_adapter.getItemCount() >= LOAD_BOUNDARY && lastVisiblePosition >= m_adapter.getItemCount() - LOAD_BOUNDARY)
                              {
                                m_adapter.loadMore(activity);
                              }
                            }
                          });
                        }
                      }
                    });
              }
            }
          });
        }
      });
    }
  }

  void viewFull(Activity activity, View dataView)
  {
    DataEntry entry = m_adapter.getEntry(dataView);
    View view = MainActivity.getFullView(activity, entry);

    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setView(view)
        .setPositiveButton(R.string.done_dialog_option, null)
        .create();
    dialog.show();
  }

  void launchEdit(Activity activity, final View dataView)
  {
    DataEntry entry = m_adapter.getEntry(dataView);
    Intent intent = new Intent(activity, EditEntryActivity.class);
    intent.putExtra("timestamp", entry.actualTimestamp);
    startActivityForResult(intent, REQUEST_EDIT_ENTRY);
  }

  void deleteEntry(final Activity activity, final View dataView)
  {
    final DataEntry entry = m_adapter.getEntry(dataView);
    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setTitle(R.string.title_delete_entry)
        .setMessage(R.string.message_delete_entry)
        .setNegativeButton(R.string.cancel_dialog_option, null)
        .setPositiveButton(R.string.delete_dialog_option, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            AsyncTask.execute(new Runnable()
            {
              @Override
              public void run()
              {
                AppDatabase.getInstance().dataEntriesDao().delete(entry);
                activity.runOnUiThread(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    m_adapter.deleteEntry(dataView);
                    if (m_adapter.getItemCount() == 0)
                    {
                      refreshEntryList();
                    }
                  }
                });
              }
            });
          }
        })
        .create();
    dialog.show();
  }
}
