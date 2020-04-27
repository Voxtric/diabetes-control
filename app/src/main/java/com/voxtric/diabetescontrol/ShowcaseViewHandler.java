package com.voxtric.diabetescontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.voxtric.diabetescontrol.utilities.ViewUtilities;

import java.util.Objects;

class ShowcaseViewHandler
{
  private static final float UNDER_SHOWCASE_ALPHA = 0.3f;

  private static final int MAIN_ACTIVITY_SHOWCASE_APP = 0;
  private static final int MAIN_ACTIVITY_SHOWCASE_SETTINGS = 1;
  private static final int MAIN_ACTIVITY_SHOWCASE_EXPORTING = 2;
  private static final int MAIN_ACTIVITY_SHOWCASE_NAVIGATION = 3;

  private static final int ADD_NEW_FRAGMENT_SHOWCASE_ADD_NEW_ENTRY = 0;
  private static final int ADD_NEW_FRAGMENT_SHOWCASE_AUTO_POPULATED = 1;
  private static final int ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS = 2;

  private static final int ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS = 0;

  private static final int ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_CONTROL = 0;
  private static final int ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_STATISTICS = 1;

  private static final int SETTINGS_ACTIVITY_SHOWCASE_HELP_BUTTON = 0;

  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS = 0;
  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_RESET_EVENTS = 1;
  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_ADD_NEW_EVENT = 2;

  @SuppressLint("StaticFieldLeak")
  private static ShowcaseView s_activeShowcaseView = null;

  static void handleMainActivityShowcaseViews(final MainActivity activity)
  {
    Toolbar toolbar = activity.findViewById(R.id.toolbar);

    final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    final int mainActivityShowcaseProgress = preferences.getInt("main_activity_showcase_progress", 0);

    Target target;
    @StringRes int showcaseTitle;
    @StringRes int showcaseText;

    switch (mainActivityShowcaseProgress)
    {
    case MAIN_ACTIVITY_SHOWCASE_APP:
      target = new ViewTarget(toolbar.getChildAt(0));
      showcaseTitle = R.string.main_activity_showcase_app_title;
      showcaseText = R.string.main_activity_showcase_app_text;
      break;
    case MAIN_ACTIVITY_SHOWCASE_SETTINGS:
      target = new ViewTarget(toolbar.getChildAt(1));
      showcaseTitle = R.string.main_activity_showcase_settings_title;
      showcaseText = R.string.main_activity_showcase_settings_text;
      break;
    case MAIN_ACTIVITY_SHOWCASE_EXPORTING:
      target = new ViewTarget(toolbar.getChildAt(1));
      showcaseTitle = R.string.main_activity_showcase_exporting_title;
      showcaseText = R.string.main_activity_showcase_exporting_text;
      break;
    case MAIN_ACTIVITY_SHOWCASE_NAVIGATION:
      target = new ViewTarget(R.id.navigation, activity);
      showcaseTitle = R.string.main_activity_showcase_navigation_title;
      showcaseText = R.string.main_activity_showcase_navigation_text;
      break;
    default:
      target = null;
      showcaseTitle = 0;
      showcaseText = 0;
    }

    if (target != null)
    {
      activity.findViewById(R.id.fragment_container).setAlpha(UNDER_SHOWCASE_ALPHA);
      s_activeShowcaseView = new ShowcaseView.Builder(activity)
          .setTarget(target)
          .setContentTitle(showcaseTitle)
          .setContentText(showcaseText)
          .blockAllTouches()
          .hideOnTouchOutside()
          .withHoloShowcase()
          .setStyle(R.style.CustomShowcaseTheme)
          .setOnClickListener(new View.OnClickListener()
          {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onClick(View view)
            {
              s_activeShowcaseView.hide();
              s_activeShowcaseView = null;
              preferences.edit().putInt("main_activity_showcase_progress", mainActivityShowcaseProgress + 1).commit();
              handleMainActivityShowcaseViews(activity);
            }
          })
          .build();
    }
    else
    {
      activity.findViewById(R.id.fragment_container).setAlpha(1.0f);
      handleAddNewEntryFragmentShowcaseViews(activity);
    }
  }

  static void handleAddNewEntryFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    ViewGroup contentView = activity.findViewById(R.id.new_entry_content);
    if ((preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) && (contentView != null))
    {
      final int fragmentShowcaseProgress = preferences.getInt("add_new_entry_fragment_showcase_progress", 0);

      Target target;
      View targetView;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;

      switch (fragmentShowcaseProgress)
      {
      case ADD_NEW_FRAGMENT_SHOWCASE_ADD_NEW_ENTRY:
        targetView = activity.findViewById(R.id.add_new_entry_button);
        targetView.getParent().requestChildFocus(targetView, targetView);
        target = new ViewTarget(R.id.add_new_entry_button, activity);
        showcaseTitle = R.string.new_entry_fragment_showcase_add_new_entry_title;
        showcaseText = R.string.new_entry_fragment_showcase_add_new_entry_text;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.add_new_entry_button }, 1.0f);
        break;
      case ADD_NEW_FRAGMENT_SHOWCASE_AUTO_POPULATED:
        targetView = activity.findViewById(R.id.date_button);
        targetView.getParent().requestChildFocus(targetView, targetView);
        target = new ViewTarget(R.id.time_button, activity);
        showcaseTitle = R.string.new_entry_fragment_showcase_auto_populated_title;
        showcaseText = R.string.new_entry_fragment_showcase_auto_populated_text;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.date_button, R.id.time_button, R.id.event_spinner }, 1.0f);
        ((Spinner)activity.findViewById(R.id.event_spinner)).getSelectedView().setAlpha(1.0f);
        break;
      case ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS:
        targetView = activity.findViewById(R.id.see_previous_button);
        targetView.getParent().requestChildFocus(targetView, targetView);
        target = new ViewTarget(R.id.see_previous_button, activity);
        showcaseTitle = R.string.new_entry_fragment_showcase_see_previous_title;
        showcaseText = R.string.new_entry_fragment_showcase_see_previous_text;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.see_previous_button }, 1.0f);
        break;
      default:
        target = null;
        showcaseTitle = 0;
        showcaseText = 0;
      }

      if (target != null)
      {
        s_activeShowcaseView = new ShowcaseView.Builder(activity)
            .setTarget(target)
           .setContentTitle(showcaseTitle)
           .setContentText(showcaseText)
           .blockAllTouches()
           .hideOnTouchOutside()
           .withHoloShowcase()
           .setStyle(R.style.CustomShowcaseTheme)
           .setOnClickListener(new View.OnClickListener()
           {
             @SuppressLint("ApplySharedPref")
             @Override
             public void onClick(View view)
             {
               s_activeShowcaseView.hide();
               s_activeShowcaseView = null;
               preferences.edit().putInt("add_new_entry_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
               handleAddNewEntryFragmentShowcaseViews(activity);
             }
           })
           .build();
      }
      else
      {
        ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
      }
    }
  }

  static void handleEntryListFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    final ViewGroup contentView = activity.findViewById(R.id.entry_list_content);
    if ((preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) && (contentView != null))
    {
      final int fragmentShowcaseProgress = preferences.getInt("entry_list_fragment_showcase_progress", 0);
      if (fragmentShowcaseProgress == ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS)
      {
        final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_entry_list);
        final EntryListRecyclerViewAdapter entryList = (EntryListRecyclerViewAdapter)recyclerView.getAdapter();
        if (entryList != null)
        {
          if (entryList.getItemCount() == 0)
          {
            activity.getFragment(EntryListFragment.class).setDisplayingDummyEntry(true, activity);
            recyclerView.postDelayed(new Runnable()
            {
              @Override
              public void run()
              {
                View targetView = ((EntryListRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).getMoreOptionsView();
                ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { targetView.getId() }, 1.0f);

                s_activeShowcaseView = new ShowcaseView.Builder(activity)
                    .setTarget(new ViewTarget(targetView))
                    .setContentTitle(R.string.entry_list_fragment_showcase_more_options_title)
                    .setContentText(R.string.entry_list_fragment_showcase_more_options_text)
                    .blockAllTouches()
                    .hideOnTouchOutside()
                    .withHoloShowcase()
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setOnClickListener(new View.OnClickListener()
                    {
                      @SuppressLint("ApplySharedPref")
                      @Override
                      public void onClick(View view)
                      {
                        s_activeShowcaseView.hide();
                        s_activeShowcaseView = null;
                        preferences.edit().putInt("entry_list_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
                        activity.getFragment(EntryListFragment.class).setDisplayingDummyEntry(false, activity);
                        handleEntryListFragmentShowcaseViews(activity);
                      }
                    })
                    .build();
              }
            }, 10);
          }
          else
          {
            // TODO: Test this case properly.
            recyclerView.postDelayed(new Runnable()
            {
              @Override
              public void run()
              {
                View targetView = ((EntryListRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).getMoreOptionsView();
                ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { targetView.getId() }, 1.0f);

                s_activeShowcaseView = new ShowcaseView.Builder(activity)
                    .setTarget(new ViewTarget(targetView))
                    .setContentTitle(R.string.entry_list_fragment_showcase_more_options_title)
                    .setContentText(R.string.entry_list_fragment_showcase_more_options_text)
                    .blockAllTouches()
                    .hideOnTouchOutside()
                    .withHoloShowcase()
                    .setStyle(R.style.CustomShowcaseTheme)
                    .setOnClickListener(new View.OnClickListener()
                    {
                      @SuppressLint("ApplySharedPref")
                      @Override
                      public void onClick(View view)
                      {
                        s_activeShowcaseView.hide();
                        s_activeShowcaseView = null;
                        preferences.edit().putInt("entry_list_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
                        activity.getFragment(EntryListFragment.class).setDisplayingDummyEntry(false, activity);
                        handleEntryListFragmentShowcaseViews(activity);
                      }
                    })
                    .build();
              }
            }, 1000);
          }
        }
      }
      else
      {
        ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
      }
    }
  }

  static void handleEntryGraphFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
    ViewGroup contentView = activity.findViewById(R.id.entry_graph_content);
    if ((preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) && (contentView != null))
    {
      final int fragmentShowcaseProgress = preferences.getInt("entry_graph_fragment_showcase_progress", 0);

      @IdRes int targetViewId;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;

      switch (fragmentShowcaseProgress)
      {
      case ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_CONTROL:
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.graph }, 1.0f);
        targetViewId = R.id.graph;
        showcaseTitle = R.string.entry_graph_fragment_showcase_graph_control_title;
        showcaseText = R.string.entry_graph_fragment_showcase_graph_control_text;
        break;
      case ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_STATISTICS:
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.statistics_layout_button, R.id.graph }, 1.0f);
        targetViewId = R.id.statistics_layout_button;
        showcaseTitle = R.string.entry_graph_fragment_showcase_graph_statistics_title;
        showcaseText = R.string.entry_graph_fragment_showcase_graph_statistics_text;
        break;
      default:
        targetViewId = 0;
        showcaseTitle = 0;
        showcaseText = 0;
      }

      if ((targetViewId != 0) && (activity.findViewById(targetViewId) != null))
      {
        s_activeShowcaseView = new ShowcaseView.Builder(activity)
            .setTarget(new ViewTarget(targetViewId, activity))
            .setContentTitle(showcaseTitle)
            .setContentText(showcaseText)
            .blockAllTouches()
            .hideOnTouchOutside()
            .withHoloShowcase()
            .setStyle(R.style.CustomShowcaseTheme)
            .setOnClickListener(new View.OnClickListener()
            {
              @SuppressLint("ApplySharedPref")
              @Override
              public void onClick(View view)
              {
                s_activeShowcaseView.hide();
                s_activeShowcaseView = null;
                preferences.edit().putInt("entry_graph_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
                handleEntryGraphFragmentShowcaseViews(activity);
              }
            })
            .build();
      }
      else
      {
        ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
      }
    }
  }
}
