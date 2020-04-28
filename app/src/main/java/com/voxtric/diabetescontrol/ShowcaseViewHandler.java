package com.voxtric.diabetescontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.voxtric.diabetescontrol.settings.EditEventsActivity;
import com.voxtric.diabetescontrol.settings.EditEventsRecyclerViewAdapter;
import com.voxtric.diabetescontrol.settings.SettingsActivity;
import com.voxtric.diabetescontrol.utilities.ViewUtilities;

import java.util.Objects;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.shape.RectangleShape;
import uk.co.deanwild.materialshowcaseview.shape.Shape;

public class ShowcaseViewHandler
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
  private static MaterialShowcaseView s_activeShowcaseView = null;

  static void handleMainActivityShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        preferences.getBoolean("show_showcases", false))
    {
      Toolbar toolbar = activity.findViewById(R.id.toolbar);

      final int activityShowcaseProgress = preferences.getInt("main_activity_showcase_progress", 0);

      View showcaseTargetView;
      Shape showcaseShape = null;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;

      switch (activityShowcaseProgress)
      {
      case MAIN_ACTIVITY_SHOWCASE_APP:
        showcaseTargetView = toolbar.getChildAt(0);
        showcaseTitle = R.string.main_activity_showcase_app_title;
        showcaseText = R.string.main_activity_showcase_app_text;
        break;
      case MAIN_ACTIVITY_SHOWCASE_SETTINGS:
        showcaseTargetView = toolbar.getChildAt(1);
        showcaseTitle = R.string.main_activity_showcase_settings_title;
        showcaseText = R.string.main_activity_showcase_settings_text;
        break;
      case MAIN_ACTIVITY_SHOWCASE_EXPORTING:
        showcaseTargetView = toolbar.getChildAt(1);
        showcaseTitle = R.string.main_activity_showcase_exporting_title;
        showcaseText = R.string.main_activity_showcase_exporting_text;
        break;
      case MAIN_ACTIVITY_SHOWCASE_NAVIGATION:
        showcaseTargetView = activity.findViewById(R.id.navigation);
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
          showcaseShape = new RectangleShape(new Rect(), false);
        }
        showcaseTitle = R.string.main_activity_showcase_navigation_title;
        showcaseText = R.string.main_activity_showcase_navigation_text;
        break;
      default:
        showcaseTargetView = null;
        showcaseTitle = 0;
        showcaseText = 0;
      }

      if (showcaseTargetView != null)
      {
        activity.findViewById(R.id.fragment_container).setAlpha(UNDER_SHOWCASE_ALPHA);
        MaterialShowcaseView.Builder showcaseViewBuilder = new MaterialShowcaseView.Builder(activity).setTarget(
            showcaseTargetView).setTitleText(showcaseTitle).setContentText(showcaseText).setTargetTouchable(false).setTitleTextColor(
            Color.WHITE).setContentTextColor(Color.WHITE).setDismissText(R.string.next_dialog_option);
        if (showcaseShape != null)
        {
          showcaseViewBuilder.setShape(showcaseShape);
        }
        s_activeShowcaseView = showcaseViewBuilder.show();

        s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            s_activeShowcaseView.hide();
            s_activeShowcaseView = null;
            preferences.edit().putInt("main_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            handleMainActivityShowcaseViews(activity);
          }
        });
      }
      else
      {
        activity.findViewById(R.id.fragment_container).setAlpha(1.0f);
        NewEntryFragment newEntryFragment = activity.getFragment(NewEntryFragment.class);
        EntryListFragment entryListFragment = activity.getFragment(EntryListFragment.class);
        EntryGraphFragment entryGraphFragment = activity.getFragment(EntryGraphFragment.class);
        if (newEntryFragment.isVisible() && newEntryFragment.getUserVisibleHint())
        {
          handleAddNewEntryFragmentShowcaseViews(activity);
        }
        else if (entryListFragment.isVisible() && entryListFragment.getUserVisibleHint())
        {
          handleEntryListFragmentShowcaseViews(activity);
        }
        else if (entryGraphFragment.isVisible() && entryGraphFragment.getUserVisibleHint())
        {
          handleEntryGraphFragmentShowcaseViews(activity);
        }
      }
    }
  }

  static void handleAddNewEntryFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    final ViewGroup contentView = activity.findViewById(R.id.new_entry_content);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        (preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) &&
        preferences.getBoolean("show_showcases", false) &&
        (contentView != null))
    {
      final int fragmentShowcaseProgress = preferences.getInt("add_new_entry_fragment_showcase_progress", 0);

      View showcaseTargetView;
      View scrollTargetView = null;
      Shape showcaseShape = null;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;
      @StringRes final int showcaseDismissText;

      switch (fragmentShowcaseProgress)
      {
      case ADD_NEW_FRAGMENT_SHOWCASE_ADD_NEW_ENTRY:
        showcaseTargetView = activity.findViewById(R.id.add_new_entry_button);
        scrollTargetView = showcaseTargetView;
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
          showcaseShape = new RectangleShape(new Rect(), false);
        }
        showcaseTitle = R.string.new_entry_fragment_showcase_add_new_entry_title;
        showcaseText = R.string.new_entry_fragment_showcase_add_new_entry_text;
        showcaseDismissText = R.string.next_dialog_option;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.add_new_entry_button }, 1.0f);
        activity.findViewById(R.id.event_spinner).setAlpha(UNDER_SHOWCASE_ALPHA);
        break;
      case ADD_NEW_FRAGMENT_SHOWCASE_AUTO_POPULATED:
        showcaseTargetView = (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ? activity.findViewById(R.id.dummy_showcase_target) : activity.findViewById(R.id.time_button);
        scrollTargetView = activity.findViewById(R.id.date_button);
        showcaseTitle = R.string.new_entry_fragment_showcase_auto_populated_title;
        showcaseText = R.string.new_entry_fragment_showcase_auto_populated_text;
        showcaseDismissText = R.string.next_dialog_option;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.date_button, R.id.time_button, R.id.event_spinner }, 1.0f);
        ((Spinner)activity.findViewById(R.id.event_spinner)).getSelectedView().setAlpha(1.0f);
        break;
      case ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS:
        showcaseTargetView = activity.findViewById(R.id.see_previous_button);
        scrollTargetView = showcaseTargetView;
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
          showcaseShape = new RectangleShape(new Rect(), false);
        }
        showcaseTitle = R.string.new_entry_fragment_showcase_see_previous_title;
        showcaseText = R.string.new_entry_fragment_showcase_see_previous_text;
        showcaseDismissText = R.string.done_dialog_option;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.see_previous_button }, 1.0f);
        activity.findViewById(R.id.event_spinner).setAlpha(UNDER_SHOWCASE_ALPHA);
        break;
      default:
        showcaseTargetView = null;
        showcaseTitle = 0;
        showcaseText = 0;
        showcaseDismissText = 0;
      }

      if (showcaseTargetView != null)
      {
        if (scrollTargetView != null)
        {
          scrollTargetView.getParent().requestChildFocus(scrollTargetView, scrollTargetView);
          scrollTargetView.postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              activity.forceKeyboardClosed();
            }
          }, 10);
        }

        MaterialShowcaseView.Builder showcaseViewBuilder = new MaterialShowcaseView.Builder(activity)
            .setTarget(showcaseTargetView)
            .setTitleText(showcaseTitle)
            .setContentText(showcaseText)
            .setTargetTouchable(false)
            .setTitleTextColor(Color.WHITE)
            .setContentTextColor(Color.WHITE)
            .setDismissText(showcaseDismissText);
        if (showcaseShape != null)
        {
          showcaseViewBuilder.setShape(showcaseShape);
        }
        s_activeShowcaseView = showcaseViewBuilder.show();

        s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            s_activeShowcaseView.hide();
            s_activeShowcaseView = null;
            preferences.edit().putInt("add_new_entry_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
            handleMainActivityShowcaseViews(activity);

            if (showcaseDismissText == R.string.done_dialog_option)
            {
              ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
              activity.findViewById(R.id.event_spinner).setAlpha(1.0f);
            }
          }
        });
      }
    }
  }

  static void handleEntryListFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    final ViewGroup contentView = activity.findViewById(R.id.entry_list_content);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        (preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) &&
        preferences.getBoolean("show_showcases", false) &&
        (contentView != null))
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
                View targetView = ((EntryListRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
                ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { targetView.getId() }, 1.0f);

                s_activeShowcaseView = new MaterialShowcaseView.Builder(activity)
                    .setTarget(targetView)
                    .setTitleText(R.string.entry_list_fragment_showcase_more_options_title)
                    .setContentText(R.string.entry_list_fragment_showcase_more_options_text)
                    .setTargetTouchable(false)
                    .setTitleTextColor(Color.WHITE)
                    .setContentTextColor(Color.WHITE)
                    .setDismissText(R.string.done_dialog_option)
                    .setShape(new RectangleShape(new Rect(), true))
                    .show();

                s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
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

                    ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
                  }
                });
              }
            }, 10);
          }
          else
          {
            recyclerView.postDelayed(new Runnable()
            {
              @Override
              public void run()
              {
                View targetView = ((EntryListRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
                ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { targetView.getId() }, 1.0f);

                for (int itemIndex = 0; itemIndex < entryList.getItemCount(); itemIndex++)
                {
                  EntryListRecyclerViewAdapter.ViewHolder viewHolder = (EntryListRecyclerViewAdapter.ViewHolder)recyclerView.findViewHolderForAdapterPosition(itemIndex);
                  if ((viewHolder != null) && (viewHolder.itemView != targetView))
                  {
                    viewHolder.itemView.setAlpha(UNDER_SHOWCASE_ALPHA);
                  }
                }

                s_activeShowcaseView = new MaterialShowcaseView.Builder(activity)
                    .setTarget(targetView)
                    .setTitleText(R.string.entry_list_fragment_showcase_more_options_title)
                    .setContentText(R.string.entry_list_fragment_showcase_more_options_text)
                    .setTargetTouchable(false)
                    .setTitleTextColor(Color.WHITE)
                    .setContentTextColor(Color.WHITE)
                    .setShape(new RectangleShape(new Rect(), true))
                    .setDismissText(R.string.done_dialog_option)
                    .show();

                s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
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

                    ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
                  }
                });
              }
            }, 1000);
          }
        }
      }
    }
  }

  static void handleEntryGraphFragmentShowcaseViews(final MainActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    final ViewGroup contentView = activity.findViewById(R.id.entry_graph_content);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        (preferences.getInt("main_activity_showcase_progress", 0) > MAIN_ACTIVITY_SHOWCASE_NAVIGATION) &&
        preferences.getBoolean("show_showcases", false) &&
        (contentView != null))
    {
      final int fragmentShowcaseProgress = preferences.getInt("entry_graph_fragment_showcase_progress", 0);

      final View targetView;
      @StringRes final int showcaseTitle;
      @StringRes final int showcaseText;
      @StringRes final int showcaseDismissText;

      switch (fragmentShowcaseProgress)
      {
      case ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_CONTROL:
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, null, 1.0f);
        targetView = activity.findViewById(R.id.dummy_showcase_target);
        showcaseTitle = R.string.entry_graph_fragment_showcase_graph_control_title;
        showcaseText = R.string.entry_graph_fragment_showcase_graph_control_text;
        showcaseDismissText = (activity.findViewById(R.id.statistics_layout) == null) ? R.string.done_dialog_option : R.string.next_dialog_option;
        break;
      case ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_STATISTICS:
        targetView = activity.findViewById(R.id.statistics_layout_label);
        showcaseTitle = R.string.entry_graph_fragment_showcase_graph_statistics_title;
        showcaseText = R.string.entry_graph_fragment_showcase_graph_statistics_text;
        showcaseDismissText = R.string.done_dialog_option;
        break;
      default:
        targetView = null;
        showcaseTitle = 0;
        showcaseText = 0;
        showcaseDismissText = 0;
      }

      if (targetView != null)
      {
        targetView.postDelayed(new Runnable()
        {
          @Override
          public void run()
          {
            s_activeShowcaseView = new MaterialShowcaseView.Builder(activity)
                .setTarget(targetView)
                .setTitleText(showcaseTitle)
                .setShape(new RectangleShape(new Rect(), false))
                .setContentText(showcaseText)
                .setTargetTouchable(false)
                .setTitleTextColor(Color.WHITE)
                .setContentTextColor(Color.WHITE)
                .setDismissText(showcaseDismissText)
                .show();

            s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
            {
              @SuppressLint("ApplySharedPref")
              @Override
              public void onClick(View view)
              {
                s_activeShowcaseView.hide();
                s_activeShowcaseView = null;
                preferences.edit().putInt("entry_graph_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
                handleEntryGraphFragmentShowcaseViews(activity);

                if (showcaseDismissText == R.string.done_dialog_option)
                {
                  ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
                }
              }
            });
            ((TextView)s_activeShowcaseView.findViewById(R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
          }
        }, 500);
      }
    }
  }

  public static void handleSettingsActivityShowcaseViews(final SettingsActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        preferences.getBoolean("show_showcases", false))
    {
      final int activityShowcaseProgress = preferences.getInt("settings_activity_showcase_progress", 0);

      @IdRes final int showcaseTargetViewId;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;

      if (activityShowcaseProgress == SETTINGS_ACTIVITY_SHOWCASE_HELP_BUTTON)
      {
        showcaseTargetViewId = R.id.backup_more_info_button;
        showcaseTitle = R.string.settings_activity_showcase_help_button_title;
        showcaseText = R.string.settings_activity_showcase_help_button_text;
      }
      else
      {
        showcaseTargetViewId = 0;
        showcaseTitle = 0;
        showcaseText = 0;
      }

      if (showcaseTargetViewId != 0)
      {
        ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.root), UNDER_SHOWCASE_ALPHA, new int[] { showcaseTargetViewId }, 1.0f);
        s_activeShowcaseView = new MaterialShowcaseView.Builder(activity)
            .setTarget(activity.findViewById(showcaseTargetViewId))
            .setTitleText(showcaseTitle)
            .setContentText(showcaseText)
            .setTargetTouchable(false)
            .setTitleTextColor(Color.WHITE)
            .setContentTextColor(Color.WHITE)
            .setDismissText(R.string.done_dialog_option)
            .show();

        s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            s_activeShowcaseView.hide();
            s_activeShowcaseView = null;
            preferences.edit().putInt("settings_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.root), 1.0f, null, 1.0f);
          }
        });
      }
    }
  }

  public static void handleEditEventsActivityShowcaseViews(final EditEventsActivity activity)
  {
    final SharedPreferences preferences = activity.getSharedPreferences("preferences", Context.MODE_PRIVATE);
    if ((activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) &&
        preferences.getBoolean("show_showcases", false))
    {
      final int activityShowcaseProgress = preferences.getInt("edit_events_activity_showcase_progress", 0);

      final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_event_list);

      final View showcaseTargetView;
      Shape showcaseShape = null;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;
      final @StringRes int showcaseDismissText;

      switch (activityShowcaseProgress)
      {
      case EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS:
        showcaseTargetView = ((EditEventsRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
        showcaseTitle = R.string.edit_events_activity_showcase_more_options_title;
        showcaseText = R.string.edit_events_activity_showcase_more_options_text;
        showcaseShape = new RectangleShape(new Rect(), true);
        showcaseDismissText = R.string.next_dialog_option;
        break;
      case EDIT_EVENTS_ACTIVITY_SHOWCASE_RESET_EVENTS:
        showcaseTargetView = activity.findViewById(R.id.button_reset_events);
        showcaseTitle = R.string.edit_events_activity_showcase_reset_events_title;
        showcaseText = R.string.edit_events_activity_showcase_reset_events_text;
        showcaseDismissText = R.string.next_dialog_option;
        break;
      case EDIT_EVENTS_ACTIVITY_SHOWCASE_ADD_NEW_EVENT:
        showcaseTargetView = activity.findViewById(R.id.button_add_new_event);
        showcaseTitle = R.string.edit_events_activity_showcase_add_new_event_title;
        showcaseText = R.string.edit_events_activity_showcase_add_new_event_text;
        showcaseDismissText = R.string.done_dialog_option;
        break;
      default:
        showcaseTargetView = null;
        showcaseTitle = 0;
        showcaseText = 0;
        showcaseDismissText = 0;
      }

      if (showcaseTargetView != null)
      {
        ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.edit_events_content), UNDER_SHOWCASE_ALPHA, new int[] { R.id.toolbar, showcaseTargetView.getId() }, 1.0f);
        final EditEventsRecyclerViewAdapter eventsList = (EditEventsRecyclerViewAdapter)recyclerView.getAdapter();
        if (eventsList != null)
        {
          for (int itemIndex = 0; itemIndex < eventsList.getItemCount(); itemIndex++)
          {
            EditEventsRecyclerViewAdapter.ViewHolder viewHolder = (EditEventsRecyclerViewAdapter.ViewHolder)recyclerView.findViewHolderForAdapterPosition(itemIndex);
            if ((viewHolder != null) && (viewHolder.itemView != showcaseTargetView))
            {
              viewHolder.itemView.setAlpha(UNDER_SHOWCASE_ALPHA);
            }
          }
        }

        MaterialShowcaseView.Builder showcaseViewBuilder = new MaterialShowcaseView.Builder(activity)
            .setTarget(showcaseTargetView)
            .setTitleText(showcaseTitle)
            .setContentText(showcaseText)
            .setTargetTouchable(false)
            .setTitleTextColor(Color.WHITE)
            .setContentTextColor(Color.WHITE)
            .setDismissText(showcaseDismissText);
        if (showcaseShape != null)
        {
          showcaseViewBuilder.setShape(showcaseShape);
        }
        s_activeShowcaseView = showcaseViewBuilder.show();

        s_activeShowcaseView.findViewById(R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            s_activeShowcaseView.hide();
            s_activeShowcaseView = null;
            preferences.edit().putInt("edit_events_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            handleEditEventsActivityShowcaseViews(activity);

            if (showcaseDismissText == R.string.done_dialog_option)
            {
              ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.edit_events_content), 1.0f, null, 1.0f);
              if (eventsList != null)
              {
                for (int itemIndex = 0; itemIndex < eventsList.getItemCount(); itemIndex++)
                {
                  EditEventsRecyclerViewAdapter.ViewHolder viewHolder = (EditEventsRecyclerViewAdapter.ViewHolder)recyclerView.findViewHolderForAdapterPosition(itemIndex);
                  if (viewHolder != null)
                  {
                    viewHolder.itemView.setAlpha(1.0f);
                  }
                }
              }
            }
          }
        });
      }
    }
  }
}
