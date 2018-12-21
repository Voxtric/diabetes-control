package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import voxtric.com.diabetescontrol.database.AppDatabase;
import voxtric.com.diabetescontrol.database.DataEntry;

public class EntryListFragment extends Fragment
{
    public static final int REQUEST_EDIT_ENTRY = 107;
    public static final int RESULT_LIST_UPDATE_NEEDED = 108;

    AppDatabase m_database = null;
    EntryListRecyclerViewAdapter m_adapter = null;

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
            recyclerView.setLayoutManager(new GridLayoutManager(activity, 1));

            m_database = ((MainActivity) activity).getDatabase();
            refreshEntryList();
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

    public void refreshEntryList()
    {
        final Activity activity = getActivity();
        if (activity != null)
        {
            final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_entry_list);
            AsyncTask.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    final List<DataEntry> entries = m_database.dataEntriesDao().getPreviousEntries(100);
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
                                m_adapter = new EntryListRecyclerViewAdapter(entries);
                                recyclerView.setAdapter(m_adapter);
                            }
                        }
                    });
                }
            });
        }
    }

    public void viewFull(View dataView, Activity activity)
    {
        DataEntry entry = m_adapter.getEntry(dataView);
        View view = MainActivity.getFullView(activity, entry);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(R.string.done, null)
                .create();
        dialog.show();
    }

    public void launchEdit(final View dataView, Activity activity)
    {
        DataEntry entry = m_adapter.getEntry(dataView);
        Intent intent = new Intent(activity, EditEntryActivity.class);
        intent.putExtra("time_stamp", entry.timeStamp);
        startActivityForResult(intent, REQUEST_EDIT_ENTRY);
    }

    public void deleteEntry(final View dataView, Activity activity)
    {
        final DataEntry entry = m_adapter.getEntry(dataView);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_delete_entry)
                .setMessage(R.string.message_delete_entry)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        AsyncTask.execute(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                m_database.dataEntriesDao().delete(entry);
                            }
                        });
                        m_adapter.deleteEntry(dataView);
                        if (m_adapter.getItemCount() == 0)
                        {
                            refreshEntryList();
                        }
                    }
                })
                .create();
        dialog.show();
    }
}
