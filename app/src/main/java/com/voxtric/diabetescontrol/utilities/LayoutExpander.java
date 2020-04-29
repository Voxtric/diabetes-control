package com.voxtric.diabetescontrol.utilities;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

public class LayoutExpander
{
  public static void toggleExpansion(Activity activity, View view, ExpansionState state, float expandCollapseDuration)
  {
    if (state.activeAnimator != null)
    {
      state.activeAnimator.cancel();
    }

    ValueAnimator valueAnimator;
    if (state.expanding)
    {
      valueAnimator = collapse(activity, state.view, expandCollapseDuration);
      ((LinearLayout)view).getChildAt(1).animate().rotation(90.0f).start();
    }
    else
    {
      valueAnimator = expand(activity, state.view, expandCollapseDuration, state.fullHeight);
      ((LinearLayout)view).getChildAt(1).animate().rotation(0.0f).start();
    }
    state.expanding = !state.expanding;
    state.activeAnimator = valueAnimator;
  }

  private static ValueAnimator expand(Activity activity, final View view, float duration, final int targetHeight)
  {
    Display display = activity.getWindowManager().getDefaultDisplay();
    final Point screenSize = new Point();
    display.getSize(screenSize);

    int previousHeight = view.getHeight();
    view.setVisibility(View.VISIBLE);
    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, targetHeight);
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(ValueAnimator animation)
      {
        int height = (int)animation.getAnimatedValue();
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (location[1] + height > screenSize.y)
        {
          height = targetHeight;
          animation.cancel();
        }

        view.getLayoutParams().height = height;
        view.requestLayout();
      }
    });
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    valueAnimator.setDuration((long)(targetHeight / duration));
    valueAnimator.start();

    return valueAnimator;
  }

  private static ValueAnimator collapse(Activity activity, final View view, float duration)
  {
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    Display display = activity.getWindowManager().getDefaultDisplay();
    final Point screenSize = new Point();
    display.getSize(screenSize);
    int previousHeight = screenSize.y - location[1];

    ValueAnimator valueAnimator = ValueAnimator.ofInt(previousHeight, 0);
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(ValueAnimator animation)
      {
        view.getLayoutParams().height = (int)animation.getAnimatedValue();
        view.requestLayout();
      }
    });
    valueAnimator.setInterpolator(new DecelerateInterpolator());
    if (duration == 0)
    {
      valueAnimator.setDuration(0);
    }
    else
    {
      valueAnimator.setDuration((long)(previousHeight / duration));
    }
    valueAnimator.start();

    return valueAnimator;
  }

  public static class ExpansionState
  {
    public final View view;
    final int fullHeight;
    boolean expanding;
    ValueAnimator activeAnimator;

    public ExpansionState(View view)
    {
      this.view = view;
      this.fullHeight = view.getHeight();
      this.expanding = true;
      this.activeAnimator = null;
    }
  }
}
