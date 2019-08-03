package voxtric.com.diabetescontrol.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.StringRes;

import voxtric.com.diabetescontrol.R;

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

  public static AlertDialog launchMessageDialog(Activity activity, @StringRes int messageTitleId, @StringRes int messageTextId)
  {
    AlertDialog dialog = new AlertDialog.Builder(activity)
        .setTitle(messageTitleId)
        .setMessage(messageTextId)
        .setPositiveButton(R.string.ok_dialog_option, null)
        .create();
    dialog.show();
    return dialog;
  }
}
