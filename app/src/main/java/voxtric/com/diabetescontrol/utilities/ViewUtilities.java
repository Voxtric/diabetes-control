package voxtric.com.diabetescontrol.utilities;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class ViewUtilities
{
  public static void addHintHide(final EditText viewWithHint, final int targetGravity, final Activity activity)
  {
    viewWithHint.setOnFocusChangeListener(new View.OnFocusChangeListener()
    {
      private String originalHint = null;

      @Override
      public void onFocusChange(View view, boolean hasFocus)
      {
        if (originalHint == null)
        {
          originalHint = viewWithHint.getHint().toString();
        }

        if (hasFocus)
        {
          viewWithHint.setGravity(targetGravity);
          viewWithHint.setHint("");

          // They keyboard may fail to be raised normally on some versions of Android, so do it manually.
          InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
          if (inputMethodManager != null)
          {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
          }
        }
        else if (viewWithHint.getText().length() == 0)
        {
          viewWithHint.setGravity(Gravity.CENTER);
          viewWithHint.setHint(originalHint);
        }
      }
    });
  }
}
