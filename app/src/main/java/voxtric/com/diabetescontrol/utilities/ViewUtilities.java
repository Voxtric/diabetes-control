package voxtric.com.diabetescontrol.utilities;

import android.graphics.Rect;
import android.view.View;

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
}
