package voxtric.com.diabetescontrol;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import voxtric.com.diabetescontrol.database.DataEntry;

public class ViewPreviousPagerAdapter extends PagerAdapter
{
  private Context m_context;
  private List<DataEntry> m_entries;

  ViewPreviousPagerAdapter(Context context, List<DataEntry> entries)
  {
    m_context = context;
    m_entries = entries;
  }

  @NonNull
  @Override
  public Object instantiateItem(@NonNull ViewGroup collection, int position)
  {
    View layout = MainActivity.getFullView(m_context, m_entries.get(position));
    collection.addView(layout);
    return layout;
  }

  @Override
  public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view)
  {
    collection.removeView((View) view);
  }

  @Override
  public int getCount()
  {
    return m_entries.size();
  }

  @Override
  public boolean isViewFromObject(@NonNull View view, @NonNull Object object)
  {
    return view == object;
  }

  @Override
  public CharSequence getPageTitle(int position)
  {
    return String.valueOf(m_entries.get(position).actualTimestamp);
  }
}
