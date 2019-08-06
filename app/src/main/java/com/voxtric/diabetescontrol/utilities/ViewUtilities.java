package com.voxtric.diabetescontrol.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.StringRes;

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
}
