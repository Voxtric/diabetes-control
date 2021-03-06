package com.voxtric.diabetescontrol;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

import com.voxtric.diabetescontrol.database.DataEntry;

class ViewPreviousPagerAdapter extends PagerAdapter
{
  private final Activity m_activity;
  private final List<DataEntry> m_entries;

  ViewPreviousPagerAdapter(Activity activity, List<DataEntry> entries)
  {
    m_activity = activity;
    m_entries = entries;
  }

  @NonNull
  @Override
  public Object instantiateItem(@NonNull ViewGroup collection, int position)
  {
    View layout = MainActivity.getFullView(m_activity, m_entries.get(position));
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
