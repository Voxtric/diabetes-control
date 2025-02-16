package com.voxtric.timegraph.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;

import com.voxtric.timegraph.ClickableDataPoint;
import com.voxtric.timegraph.TimeGraph;

import java.util.ArrayList;

public class GraphSurface extends GLSurfaceView
{
  private TimeGraph m_timeGraph = null;
  private GraphRenderer m_renderer = null;

  private boolean m_transformed = false;
  private long m_clickBeginTimestamp = 0L;
  private ArrayList<ClickableDataPoint> m_clickableDataPoints = null;
  private float m_clickDistanceSquared = -Float.MAX_VALUE;
  private TimeGraph.OnDataPointClickedListener m_onDataPointClickedListener = null;

  private float m_startPixelX = 0.0f;
  private float m_startPixelY = 0.0f;

  private float m_previousPixelX = 0.0f;
  private float m_previousPixelDistance = 0.0f;
  private boolean m_scaling = false;
  private boolean m_ignoreScroll = false;

  private ViewGroup[] m_disallowTouchViews = null;

  public GraphSurface(Context context)
  {
    super(context);
  }

  public GraphSurface(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public void initialise(TimeGraph timeGraph, @ColorInt int backgroundColor)
  {
    m_timeGraph = timeGraph;

    setEGLContextClientVersion(2);

    m_renderer = new GraphRenderer(backgroundColor);
    setRenderer(m_renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Renderable.releaseShader();
    TransformableRenderable.releaseShader();
    MeshRenderable.releaseShader();
  }

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent)
  {
    boolean handled;

    int pointerIndex = motionEvent.getActionIndex();
    int pointerId = motionEvent.getPointerId(pointerIndex);

    switch (motionEvent.getActionMasked())
    {
    case MotionEvent.ACTION_DOWN:
      m_clickBeginTimestamp = System.currentTimeMillis();
      m_startPixelX = motionEvent.getX();
      m_startPixelY = motionEvent.getY();
      m_previousPixelX = m_startPixelX;
      m_ignoreScroll = false;
      handled = true;

      if (m_disallowTouchViews != null)
      {
        for (ViewGroup view : m_disallowTouchViews)
        {
          view.requestDisallowInterceptTouchEvent(true);
        }
      }

      break;

    case MotionEvent.ACTION_UP:
      float xDifference = m_startPixelX - motionEvent.getX();
      float yDifference = m_startPixelY - motionEvent.getY();
      float distanceSquared = (xDifference * xDifference) + (yDifference * yDifference);

      if ((m_onDataPointClickedListener != null) &&
          (System.currentTimeMillis() - m_clickBeginTimestamp < ViewConfiguration.getLongPressTimeout()) &&
          (distanceSquared < (m_clickDistanceSquared * 1.1f)))
      {
        clickDataPoint(motionEvent.getX(), motionEvent.getY());
      }
      if (m_transformed)
      {
        m_timeGraph.refresh(false);
      }

      if (m_disallowTouchViews != null)
      {
        for (ViewGroup view : m_disallowTouchViews)
        {
          view.requestDisallowInterceptTouchEvent(false);
        }
      }

      m_transformed = false;
      handled = true;
      break;

    case MotionEvent.ACTION_POINTER_DOWN:
      m_scaling = true;
      float startingXDifference = motionEvent.getX(0) - motionEvent.getX(1);
      float startingYDifference = motionEvent.getY(0) - motionEvent.getY(1);
      m_previousPixelDistance = (float)Math.sqrt((startingXDifference * startingXDifference) + (startingYDifference * startingYDifference));
      handled = true;
      break;

    case MotionEvent.ACTION_POINTER_UP:
      m_scaling = false;
      handled = true;
      break;

    case MotionEvent.ACTION_MOVE:
      m_transformed = true;
      if (!m_scaling)
      {
        if (!m_ignoreScroll)
        {
          float pixelX = motionEvent.getX();
          float pixelXDelta = pixelX - m_previousPixelX;
          float normalisedXDelta = pixelXDelta / getWidth();
          m_timeGraph.scrollData(normalisedXDelta);
          m_previousPixelX = pixelX;
        }
      }
      else if (motionEvent.getPointerCount() >= 2)
      {
        float pixelXDifference = motionEvent.getX(0) - motionEvent.getX(1);
        float pixelYDifference = motionEvent.getY(0) - motionEvent.getY(1);
        float pixelDistance = (float)Math.sqrt((pixelXDifference * pixelXDifference) + (pixelYDifference * pixelYDifference));
        float normalisedDistanceDelta = (pixelDistance - m_previousPixelDistance) / getWidth();

        float pixelXCentre = motionEvent.getX(0) - (pixelXDifference * 0.5f);
        float normalisedXCentre = pixelXCentre / getWidth();

        m_timeGraph.scaleData(normalisedDistanceDelta, normalisedXCentre);
        m_ignoreScroll = true;
        m_previousPixelDistance = pixelDistance;
      }
      handled = true;
      break;

    default:
      handled = false;
    }

    return handled;
  }

  public void setClickablePoints(ArrayList<ClickableDataPoint> clickableDataPoints)
  {
    m_clickableDataPoints = clickableDataPoints.isEmpty() ? null : clickableDataPoints;
    if (m_clickableDataPoints == null)
    {
      m_clickDistanceSquared = -Float.MAX_VALUE;
    }
    else
    {
      float clickDistance = (float)Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 0.025f;
      m_clickDistanceSquared = clickDistance * clickDistance;
    }
  }

  public void setOnDataPointClickedListener(TimeGraph.OnDataPointClickedListener listener)
  {
    m_onDataPointClickedListener = listener;
  }

  private void clickDataPoint(float x, float y)
  {
    ClickableDataPoint closestDataPoint = null;
    float closestDistanceSquared = Float.MAX_VALUE;
    for (ClickableDataPoint dataPoint : m_clickableDataPoints)
    {
      float xDifference = x - (dataPoint.normalisedX * getWidth());
      float yDifference = y - (dataPoint.normalisedY * getHeight());
      float distanceSquared = (xDifference * xDifference) + (yDifference * yDifference);
      if ((distanceSquared < m_clickDistanceSquared) && (distanceSquared < closestDistanceSquared))
      {
        closestDistanceSquared = distanceSquared;
        closestDataPoint = dataPoint;
      }
    }
    if (closestDataPoint != null)
    {
      m_onDataPointClickedListener.onDataPointClicked(m_timeGraph, closestDataPoint.timestamp, closestDataPoint.value);
    }
  }

  public void setDisallowHorizontalScrollViews(ViewGroup[] views)
  {
    m_disallowTouchViews = views;
  }

  public void setBackgroundColor(final @ColorInt int color)
  {
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.setClearColor(color);
        requestRender();
      }
    });
  }

  public @ColorInt int getBackgroundColor()
  {
    return m_renderer.getClearColor();
  }

  public LineStripRenderable addLineStrip(int drawOrder, float[] coords, @ColorInt int color)
  {
    final LineStripRenderable lineStrip = new LineStripRenderable(drawOrder, coords, color);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(lineStrip);
        requestRender();
      }
    });
    return lineStrip;
  }

  public LineRenderable addLine(int drawOrder, float[] coords, @ColorInt int color)
  {
    final LineRenderable line = new LineRenderable(drawOrder, coords, color);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(line);
        requestRender();
      }
    });
    return line;
  }

  public MeshRenderable addMesh(int drawOrder, float[] coords, short[] indices, float[] colors)
  {
    final MeshRenderable mesh = new MeshRenderable(drawOrder, coords, indices, colors);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(mesh);
        requestRender();
      }
    });
    return mesh;
  }

  public void removeRenderable(final Renderable renderable)
  {
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.removeRenderable(renderable);
        requestRender();
      }
    });
  }
}
