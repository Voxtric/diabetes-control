package com.voxtric.diabetescontrol.utilities;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class HintHideOnFocusChangeListener implements View.OnFocusChangeListener
{
  private String m_originalHint;
  private int m_originalGravity;
  private int m_targetGravity;

  public HintHideOnFocusChangeListener(EditText viewWithHint, int targetGravity)
  {
    m_originalHint = viewWithHint.getHint().toString();
    m_originalGravity = viewWithHint.getGravity();
    m_targetGravity = targetGravity;
  }

  @Override
  public void onFocusChange(View view, boolean hasFocus)
  {
    EditText editText = (EditText)view;
    if (hasFocus)
    {
      editText.setGravity(m_targetGravity);
      editText.setHint("");

      // They keyboard may fail to be raised normally on some versions of Android, so do it manually.
      InputMethodManager inputMethodManager = (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (inputMethodManager != null)
      {
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
      }
    }
    else if (editText.getText().length() == 0)
    {
      editText.setGravity(m_originalGravity);
      editText.setHint(m_originalHint);
    }
  }

  public void changeOriginalHint(String newHint)
  {
    m_originalHint = newHint;
  }
}
