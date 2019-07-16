package voxtric.com.diabetescontrol.utilities;

import android.view.View;

import java.util.ArrayList;

public class CompositeOnFocusChangeListener implements View.OnFocusChangeListener
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
    for (View.OnFocusChangeListener listener: m_registeredListeners)
    {
      listener.onFocusChange(view, hasFocus);
    }
  }

  public static void applyListenerToView(View view, View.OnFocusChangeListener listener)
  {
    View.OnFocusChangeListener onFocusChangeListener = view.getOnFocusChangeListener();
    if (onFocusChangeListener == null)
    {
      view.setOnFocusChangeListener(listener);
    }
    else if (onFocusChangeListener instanceof CompositeOnFocusChangeListener)
    {
      ((CompositeOnFocusChangeListener)onFocusChangeListener).registerListener(listener);
    }
    else
    {
      CompositeOnFocusChangeListener newOnFocusChangeListener = new CompositeOnFocusChangeListener(onFocusChangeListener);
      newOnFocusChangeListener.registerListener(listener);
      view.setOnFocusChangeListener(newOnFocusChangeListener);
    }
  }
}