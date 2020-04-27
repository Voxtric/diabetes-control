package com.voxtric.diabetescontrol.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.voxtric.diabetescontrol.R;

public class ViewUtilities
{
  public static float getVisibilityValue(View view)
  {
    Rect dataViewRect = new Rect();
    boolean isVisible = view.getGlobalVisibleRect(dataViewRect);
    if (isVisible)
    {
      float visible = dataViewRect.width() * dataViewRect.height();
      float total = view.getWidth() * view.getHeight();
      return Math.min(1.0f, visible / total);
    }
    else
    {
      return 0.0f;
    }
  }

  public static AlertDialog launchMessageDialog(Activity activity, String messageTitle, String messageText, DialogInterface.OnClickListener onOkClicked)
  {
    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setTitle(messageTitle)
        .setMessage(messageText)
        .setPositiveButton(R.string.ok_dialog_option, onOkClicked)
        .create();
    dialog.show();
    return dialog;
  }

  public static void setAlphaForChildren(@NonNull ViewGroup viewGroup, float alpha, @IdRes int[] excluding, float excludedAlpha)
  {
    for (int childIndex = 0; childIndex < viewGroup.getChildCount(); childIndex++)
    {
      View view = viewGroup.getChildAt(childIndex);
      boolean viewExcluded = false;
      if (excluding != null)
      {
        for (int excludingIndex = 0; (excludingIndex < excluding.length) && !viewExcluded; excludingIndex++)
        {
          viewExcluded = view.getId() == excluding[excludingIndex];
        }
      }

      if (viewExcluded)
      {
        view.setAlpha(excludedAlpha);
      }
      else
      {
        if (view instanceof ViewGroup)
        {
          setAlphaForChildren((ViewGroup)view, alpha, excluding, excludedAlpha);
        }
        else
        {
          view.setAlpha(alpha);
        }
      }
    }
  }
}
