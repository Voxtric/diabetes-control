package voxtric.com.diabetescontrol.utilities;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.ArrayList;

public class ViewUtilities
{
  public static void addHintHide(final EditText viewWithHint, final int targetGravity, final Activity activity)
  {
    final View.OnFocusChangeListener HINT_HIDE_LISTENER = new View.OnFocusChangeListener()
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
    };

    View.OnFocusChangeListener focusChangeListener = viewWithHint.getOnFocusChangeListener();
    if (focusChangeListener == null)
    {
      viewWithHint.setOnFocusChangeListener(HINT_HIDE_LISTENER);
    }
    else if (focusChangeListener instanceof CompositeOnFocusChangeListener)
    {
      ((CompositeOnFocusChangeListener)focusChangeListener).registerListener(HINT_HIDE_LISTENER);
    }
    else
    {
      viewWithHint.setOnFocusChangeListener(new CompositeOnFocusChangeListener(HINT_HIDE_LISTENER));
    }
  }

  static class CompositeOnFocusChangeListener implements View.OnFocusChangeListener
  {
    private ArrayList<View.OnFocusChangeListener> m_registeredListeners = new ArrayList<>();

    CompositeOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener)
    {
      m_registeredListeners.add(onFocusChangeListener);
    }

    void registerListener(View.OnFocusChangeListener listener)
    {
      m_registeredListeners.add(listener);
    }

    public void unregisterListener(View.OnFocusChangeListener listener)
    {
      m_registeredListeners.remove(listener);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
      for(View.OnFocusChangeListener listener: m_registeredListeners)
      {
        listener.onFocusChange(view, hasFocus);
      }
    }
  }
}
