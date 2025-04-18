package com.voxtric.diabetescontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
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
  private static final float UNDER_SHOWCASE_ALPHA = 0.4f;

  private static final int MAIN_ACTIVITY_SHOWCASE_APP = 0;
  private static final int MAIN_ACTIVITY_SHOWCASE_SETTINGS = 1;
  private static final int MAIN_ACTIVITY_SHOWCASE_EXPORTING = 2;
  private static final int MAIN_ACTIVITY_SHOWCASE_NAVIGATION = 3;

  private static final int ADD_NEW_FRAGMENT_SHOWCASE_ADD_NEW_ENTRY = 0;
  private static final int ADD_NEW_FRAGMENT_SHOWCASE_AUTO_POPULATED = 1;
  private static final int ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS_BUTTON = 2;
  private static final int ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS_EXAMPLE = 3;

  private static final int ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS_MENU = 0;
  private static final int ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS_ACTIONS = 1;

  private static final int ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_CONTROL = 0;
  private static final int ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_STATISTICS = 1;

  private static final int SETTINGS_ACTIVITY_SHOWCASE_HELP_BUTTON = 0;

  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS_MENU = 0;
  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS_ACTIONS = 1;
  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_RESET_EVENTS = 2;
  private static final int EDIT_EVENTS_ACTIVITY_SHOWCASE_ADD_NEW_EVENT = 3;

  @SuppressLint("StaticFieldLeak")
  private static MaterialShowcaseView s_activeShowcaseView = null;
  private static void closeCurrentActiveShowcaseView()
  {
    if (s_activeShowcaseView != null)
    {
      try
      {
        s_activeShowcaseView.hide();
        s_activeShowcaseView = null;
      }
      catch (Exception exception)
      {
        Log.e("ShowcaseViewHandler", exception.getMessage(), exception);
      }
    }
  }

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
      int showcaseShapePadding = Integer.MIN_VALUE;
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
        showcaseShapePadding = -10;
        showcaseTitle = R.string.main_activity_showcase_navigation_title;
        showcaseText = R.string.main_activity_showcase_navigation_text;
        toolbar.setAlpha(UNDER_SHOWCASE_ALPHA);
        break;
      default:
        showcaseTargetView = null;
        showcaseTitle = 0;
        showcaseText = 0;
      }

      if (showcaseTargetView != null)
      {
        closeCurrentActiveShowcaseView();

        activity.findViewById(R.id.fragment_container).setAlpha(UNDER_SHOWCASE_ALPHA);
        MaterialShowcaseView.Builder showcaseViewBuilder = new MaterialShowcaseView.Builder(activity)
            .setTarget(showcaseTargetView)
            .setTitleText(showcaseTitle)
            .setContentText(showcaseText)
            .setTargetTouchable(false).setTitleTextColor(Color.WHITE)
            .setContentTextColor(Color.WHITE)
            .setDismissText(R.string.next_dialog_option);
        if (showcaseShape != null)
        {
          showcaseViewBuilder.setShape(showcaseShape);
        }
        if (showcaseShapePadding != Integer.MIN_VALUE)
        {
          showcaseViewBuilder.setShapePadding(showcaseShapePadding);
        }
        s_activeShowcaseView = showcaseViewBuilder.show();
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

        s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            closeCurrentActiveShowcaseView();
            preferences.edit().putInt("main_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            handleMainActivityShowcaseViews(activity);
          }
        });
      }
      else
      {
        activity.findViewById(R.id.fragment_container).setAlpha(1.0f);
        toolbar.setAlpha(1.0f);
        if (activity.fragmentActive(NewEntryFragment.class))
        {
          handleAddNewEntryFragmentShowcaseViews(activity);
        }
        else if (activity.fragmentActive(EntryListFragment.class))
        {
          handleEntryListFragmentShowcaseViews(activity);
        }
        else if (activity.fragmentActive(EntryGraphFragment.class))
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
      case ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS_BUTTON:
        showcaseTargetView = activity.findViewById(R.id.see_previous_button);
        scrollTargetView = showcaseTargetView;
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
          showcaseShape = new RectangleShape(new Rect(), false);
        }
        showcaseTitle = R.string.new_entry_fragment_showcase_see_previous_title;
        showcaseText = R.string.new_entry_fragment_showcase_see_previous_button_text;
        showcaseDismissText = R.string.next_dialog_option;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.see_previous_button }, 1.0f);
        activity.findViewById(R.id.event_spinner).setAlpha(UNDER_SHOWCASE_ALPHA);
        break;
      case ADD_NEW_FRAGMENT_SHOWCASE_SEE_PREVIOUS_EXAMPLE:
        showcaseTargetView = activity.findViewById(R.id.food_eaten_label);
        scrollTargetView = activity.findViewById(R.id.date_label);
        showcaseTitle = R.string.new_entry_fragment_showcase_see_previous_title;
        showcaseText = R.string.new_entry_fragment_showcase_see_previous_example_text;
        showcaseDismissText = R.string.done_dialog_option;
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.food_eaten_label }, 1.0f);
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
        closeCurrentActiveShowcaseView();

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

        activity.findViewById(R.id.toolbar).setAlpha(UNDER_SHOWCASE_ALPHA);
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
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

        s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            closeCurrentActiveShowcaseView();
            preferences.edit().putInt("add_new_entry_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
            handleMainActivityShowcaseViews(activity);

            if (showcaseDismissText == R.string.done_dialog_option)
            {
              ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
              activity.findViewById(R.id.toolbar).setAlpha(1.0f);
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
      int fragmentShowcaseProgress = preferences.getInt("entry_list_fragment_showcase_progress", 0);

      final @StringRes int showcaseTitle;
      final @StringRes int showcaseText;
      final @StringRes int showcaseDismiss;

      switch (fragmentShowcaseProgress)
      {
      case ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS_MENU:
        if (activity.getResources().getDisplayMetrics().density >= 1.0f)
        {
          showcaseTitle = R.string.entry_list_fragment_showcase_more_options_menu_title;
          showcaseText = R.string.entry_list_fragment_showcase_more_options_menu_text;
          showcaseDismiss = R.string.next_dialog_option;
          break;
        }
        else
        {
          fragmentShowcaseProgress++;
          // Fall through on purpose as screen is too bad to display tutorial.
        }
      case ENTRY_LIST_FRAGMENT_SHOWCASE_MORE_OPTIONS_ACTIONS:
        showcaseTitle = R.string.entry_list_fragment_showcase_more_options_actions_title;
        showcaseText = R.string.entry_list_fragment_showcase_more_options_actions_text;
        showcaseDismiss = R.string.done_dialog_option;
        break;
      default:
        showcaseTitle = 0;
        showcaseText = 0;
        showcaseDismiss = 0;
      }

      if (showcaseTitle != 0)
      {
        final RecyclerView recyclerView = activity.findViewById(R.id.recycler_view_entry_list);
        final EntryListRecyclerViewAdapter entryList = (EntryListRecyclerViewAdapter)recyclerView.getAdapter();
        if (entryList != null)
        {
          if (entryList.getItemCount() == 0)
          {
            activity.getFragment(EntryListFragment.class).setDisplayingDummyEntry(true, activity);
          }
          else
          {
            closeCurrentActiveShowcaseView();

            View targetView = ((EntryListRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
            ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { targetView.getId() }, 1.0f);
            activity.findViewById(R.id.toolbar).setAlpha(UNDER_SHOWCASE_ALPHA);

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
                .setTitleText(showcaseTitle)
                .setContentText(showcaseText)
                .setTargetTouchable(false)
                .setTitleTextColor(Color.WHITE)
                .setContentTextColor(Color.WHITE)
                .setShape(new RectangleShape(new Rect(), true))
                .setShapePadding(0)
                .setDismissText(showcaseDismiss)
                .show();
            ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
            ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
            ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

            final int finalFragmentShowcaseProgress = fragmentShowcaseProgress;
            s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
            {
              @SuppressLint("ApplySharedPref")
              @Override
              public void onClick(View view)
              {
                closeCurrentActiveShowcaseView();
                preferences.edit().putInt("entry_list_fragment_showcase_progress", finalFragmentShowcaseProgress + 1).commit();
                handleEntryListFragmentShowcaseViews(activity);

                if (showcaseDismiss == R.string.done_dialog_option)
                {
                  activity.getFragment(EntryListFragment.class).setDisplayingDummyEntry(false, activity);
                  ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
                  activity.findViewById(R.id.toolbar).setAlpha(1.0f);
                }
              }
            });
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
        showcaseDismissText = R.string.next_dialog_option;
        break;
      case ENTRY_GRAPH_FRAGMENT_SHOWCASE_GRAPH_STATISTICS:
        ViewUtilities.setAlphaForChildren(contentView, UNDER_SHOWCASE_ALPHA, new int[] { R.id.statistics_title }, 1.0f);
        targetView = activity.findViewById(R.id.statistics_title);
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
        closeCurrentActiveShowcaseView();

        activity.findViewById(R.id.toolbar).setAlpha(UNDER_SHOWCASE_ALPHA);
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
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

        s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            closeCurrentActiveShowcaseView();
            preferences.edit().putInt("entry_graph_fragment_showcase_progress", fragmentShowcaseProgress + 1).commit();
            handleEntryGraphFragmentShowcaseViews(activity);

            if (showcaseDismissText == R.string.done_dialog_option)
            {
              ViewUtilities.setAlphaForChildren(contentView, 1.0f, null, 1.0f);
              activity.findViewById(R.id.toolbar).setAlpha(1.0f);
            }
          }
        });
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
        closeCurrentActiveShowcaseView();

        ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.root), UNDER_SHOWCASE_ALPHA, new int[] { showcaseTargetViewId }, 1.0f);
        activity.findViewById(R.id.toolbar).setAlpha(UNDER_SHOWCASE_ALPHA);
        s_activeShowcaseView = new MaterialShowcaseView.Builder(activity)
            .setTarget(activity.findViewById(showcaseTargetViewId))
            .setTitleText(showcaseTitle)
            .setContentText(showcaseText)
            .setTargetTouchable(false)
            .setTitleTextColor(Color.WHITE)
            .setContentTextColor(Color.WHITE)
            .setDismissText(R.string.done_dialog_option)
            .show();
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

        s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            closeCurrentActiveShowcaseView();
            preferences.edit().putInt("settings_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.root), 1.0f, null, 1.0f);
            activity.findViewById(R.id.toolbar).setAlpha(1.0f);
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
      int showcaseShapePadding = Integer.MIN_VALUE;
      @StringRes int showcaseTitle;
      @StringRes int showcaseText;
      final @StringRes int showcaseDismissText;

      switch (activityShowcaseProgress)
      {
      case EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS_MENU:
        showcaseTargetView = ((EditEventsRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
        showcaseTitle = R.string.edit_events_activity_showcase_more_options_menu_title;
        showcaseText = R.string.edit_events_activity_showcase_more_options_menu_text;
        showcaseShape = new RectangleShape(new Rect(), true);
        showcaseShapePadding = 0;
        showcaseDismissText = R.string.next_dialog_option;
        break;
      case EDIT_EVENTS_ACTIVITY_SHOWCASE_MORE_OPTIONS_ACTIONS:
        showcaseTargetView = ((EditEventsRecyclerViewAdapter.ViewHolder)Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0))).itemView;
        showcaseTitle = R.string.edit_events_activity_showcase_more_options_actions_title;
        showcaseText = R.string.edit_events_activity_showcase_more_options_actions_text;
        showcaseShape = new RectangleShape(new Rect(), true);
        showcaseShapePadding = 0;
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
        closeCurrentActiveShowcaseView();

        ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.edit_events_content), UNDER_SHOWCASE_ALPHA, new int[] { R.id.toolbar, showcaseTargetView.getId() }, 1.0f);
        activity.findViewById(R.id.toolbar).setAlpha(UNDER_SHOWCASE_ALPHA);
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
        if (showcaseShapePadding != Integer.MIN_VALUE)
        {
          showcaseViewBuilder.setShapePadding(showcaseShapePadding);
        }
        s_activeShowcaseView = showcaseViewBuilder.show();
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_content)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));
        ((TextView)s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss)).setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.getResources().getDimension(R.dimen.large_text_size));

        s_activeShowcaseView.findViewById(uk.co.deanwild.materialshowcaseview.R.id.tv_dismiss).setOnClickListener(new View.OnClickListener()
        {
          @SuppressLint("ApplySharedPref")
          @Override
          public void onClick(View view)
          {
            closeCurrentActiveShowcaseView();
            preferences.edit().putInt("edit_events_activity_showcase_progress", activityShowcaseProgress + 1).commit();
            handleEditEventsActivityShowcaseViews(activity);

            if (showcaseDismissText == R.string.done_dialog_option)
            {
              ViewUtilities.setAlphaForChildren((ViewGroup)activity.findViewById(R.id.edit_events_content), 1.0f, null, 1.0f);
              activity.findViewById(R.id.toolbar).setAlpha(1.0f);
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
