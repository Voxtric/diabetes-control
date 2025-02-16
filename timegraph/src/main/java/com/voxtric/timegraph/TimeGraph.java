package com.voxtric.timegraph;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.drawable.DrawableCompat;

import com.voxtric.timegraph.opengl.GraphSurface;
import com.voxtric.timegraph.opengl.LineRenderable;
import com.voxtric.timegraph.opengl.LineStripRenderable;
import com.voxtric.timegraph.opengl.MeshRenderable;
import com.voxtric.timegraph.opengl.Renderable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class TimeGraph extends ConstraintLayout
{
  private static final boolean DEFAULT_SHOW_VALUE_AXIS = true;
  private static final float DEFAULT_VALUE_AXIS_TEXT_SIZE_SP = 14.0f;
  private static final @ColorInt int DEFAULT_VALUE_AXIS_TEXT_COLOR = Color.GRAY;
  private static final float DEFAULT_VALUE_AXIS_MIN = 0.0f;
  private static final float DEFAULT_VALUE_AXIS_MAX = 100.0f;

  private static final boolean DEFAULT_SHOW_TIME_AXIS = true;
  private static final float DEFAULT_TIME_AXIS_TEXT_SIZE_SP = 14.0f;
  private static final @ColorInt int DEFAULT_TIME_AXIS_TEXT_COLOR = Color.GRAY;
  private static final @ColorInt int DEFAULT_TIME_AXIS_MARKER_COLOR = Color.BLACK;

  private static final boolean DEFAULT_SHOW_NO_DATA_TEXT = true;
  private static final String DEFAULT_NO_DATA_TEXT = "No Data To Display";
  private static final float DEFAULT_NO_DATA_TEXT_SIZE_SP = 18.0f;
  private static final @ColorInt int DEFAULT_NO_DATA_TEXT_COLOR = Color.BLACK;

  private static final boolean DEFAULT_SHOW_REFRESH_PROGRESS = true;
  private static final @ColorInt int DEFAULT_GRAPH_BACKGROUND_COLOR = Color.parseColor("#E6E6E6");
  private static final @ColorInt int DEFAULT_DATA_LINE_COLOR = Color.BLACK;

  private static final boolean DEFAULT_ALLOW_SCROLL = true;
  private static final boolean DEFAULT_ALLOW_SCALE = true;

  private static final float VALUE_AXIS_MARGIN_DP = 4.0f;
  private static final long NEW_DATA_ANIMATION_DURATION = 600L;
  private static final float HALF_FADE_MULTIPLIER = 0.05f;
  private static final float TIME_AXIS_LABEL_WIDTH_MODIFIER = 1.25f;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
      DISPLAY_MODE_BACKGROUND,
      DISPLAY_MODE_BACKGROUND_WITH_FADE,
      DISPLAY_MODE_UNDERLINE,
      DISPLAY_MODE_UNDERLINE_WITH_FADE
  })
  private @interface DisplayMode {}

  private static final int DISPLAY_MODE_BACKGROUND = 0;
  private static final int DISPLAY_MODE_BACKGROUND_WITH_FADE = 1;
  private static final int DISPLAY_MODE_UNDERLINE = 2;
  public static final int DISPLAY_MODE_UNDERLINE_WITH_FADE = 3;

  private boolean m_showValueAxis = DEFAULT_SHOW_VALUE_AXIS;
  private float m_valueAxisTextSizeSp = DEFAULT_VALUE_AXIS_TEXT_SIZE_SP;
  private @ColorInt int m_valueAxisTextColor = DEFAULT_VALUE_AXIS_TEXT_COLOR;
  private float m_valueAxisMin = DEFAULT_VALUE_AXIS_MIN;
  private float m_valueAxisMax = DEFAULT_VALUE_AXIS_MAX;

  private boolean m_showTimeAxis = DEFAULT_SHOW_TIME_AXIS;
  private float m_timeAxisTextSizeSp = DEFAULT_TIME_AXIS_TEXT_SIZE_SP;
  private @ColorInt int m_timeAxisTextColor = DEFAULT_TIME_AXIS_TEXT_COLOR;
  private @ColorInt int m_timeAxisMarkerColor = DEFAULT_TIME_AXIS_MARKER_COLOR;

  private boolean m_showNoDataText = DEFAULT_SHOW_NO_DATA_TEXT;
  private CharSequence m_noDataText = DEFAULT_NO_DATA_TEXT;
  private float m_noDataTextSizeSp = DEFAULT_NO_DATA_TEXT_SIZE_SP;
  private @ColorInt int m_noDataTextColor = DEFAULT_NO_DATA_TEXT_COLOR;

  private boolean m_showRefreshProgress = DEFAULT_SHOW_REFRESH_PROGRESS;
  private @ColorInt int m_graphBackgroundColor = DEFAULT_GRAPH_BACKGROUND_COLOR;
  private @ColorInt int m_dataLineColor = DEFAULT_DATA_LINE_COLOR;

  private boolean m_allowScroll = DEFAULT_ALLOW_SCROLL;
  private boolean m_allowScale = DEFAULT_ALLOW_SCALE;

  private long m_startTimestamp = 0L;
  private long m_endTimestamp = 0L;
  private long m_beforeScalingStartTimestamp = Long.MIN_VALUE;
  private long m_beforeScalingEndTimestamp = Long.MAX_VALUE;
  private GraphDataProvider m_dataProvider = null;

  private boolean m_refreshing = false;
  private boolean m_newRefreshRequested = false;
  private ProgressBar m_refreshProgressView = null;
  private TextView m_noDataView = null;
  private Boolean m_hasEnoughData = false;

  private GraphSurface m_graphSurfaceView = null;
  private float m_xOffset = 0.0f;
  private float m_xScale = 1.0f;
  private float m_normalisedForcedXCentre = -1.0f;

  private LineStripRenderable m_dataLineStrip = null;
  private MeshRenderable m_rangeHighlightMesh = null;
  private LineRenderable m_labelMarkersLine = null;
  private ValueAnimator m_newDataAnimator = null;

  private GraphData m_firstGraphDataEntry = null;
  private GraphData m_lastGraphDataEntry = null;

  private TextView m_valueAxisMinView = null;
  private TextView m_valueAxisMaxView = null;
  private final ArrayList<TextView> m_valueAxisMidViews = new ArrayList<>();

  private RelativeLayout m_timeAxisLabelsLayoutView = null;
  private final LongSparseArray<TimeAxisLabel> m_timeAxisLabels = new LongSparseArray<>();
  private Drawable m_timeAxisLabelsBackground = null;
  private long m_timeAxisLabelsAnchorTimestamp = Long.MIN_VALUE;

  private float[] m_rangeHighlightingValues = null;
  private int[] m_rangeHighlightingColors = null;
  private @DisplayMode
  int m_rangeHighlightingDisplayMode = DISPLAY_MODE_BACKGROUND;

  private OnPeriodChangeListener m_onPeriodChangeListener = null;
  private OnRefreshListener m_onRefreshListener = null;

  public TimeGraph(Context context)
  {
    super(context);
    initialise(context);
  }

  public TimeGraph(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    applyAttributes(context, attrs);
    initialise(context);
  }

  public TimeGraph(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    applyAttributes(context, attrs);
    initialise(context);
  }

  @Override
  protected Parcelable onSaveInstanceState()
  {
    Parcelable superState = super.onSaveInstanceState();

    Bundle state = new Bundle();
    state.putParcelable("superState", superState);

    state.putBoolean("m_showValueAxis", m_showValueAxis);
    state.putFloat("m_valueAxisTextSizeSp", m_valueAxisTextSizeSp);
    state.putInt("m_valueAxisTextColor", m_valueAxisTextColor);
    state.putFloat("m_valueAxisMin", m_valueAxisMin);
    state.putFloat("m_valueAxisMax", m_valueAxisMax);

    state.putBoolean("m_showTimeAxis", m_showTimeAxis);
    state.putFloat("m_timeAxisTextSizeSp", m_timeAxisTextSizeSp);
    state.putInt("m_timeAxisTextColor", m_timeAxisTextColor);
    state.putInt("m_timeAxisMarkerColor", m_timeAxisMarkerColor);

    state.putBoolean("m_showNoDataText", m_showNoDataText);
    state.putCharSequence("m_noDataText", m_noDataText);
    state.putFloat("m_noDataTextSizeSp", m_noDataTextSizeSp);
    state.putInt("m_noDataTextColor", m_noDataTextColor);

    state.putBoolean("m_showRefreshProgress", m_showRefreshProgress);
    state.putInt("m_graphBackgroundColor", m_graphBackgroundColor);
    state.putInt("m_dataLineColor", m_dataLineColor);

    state.putBoolean("m_allowScroll", m_allowScroll);
    state.putBoolean("m_allowScale", m_allowScale);

    state.putFloatArray("m_rangeHighlightingValues", m_rangeHighlightingValues);
    state.putIntArray("m_rangeHighlightingColors", m_rangeHighlightingColors);
    state.putInt("m_rangeHighlightingDisplayMode", m_rangeHighlightingDisplayMode);

    float[] valueAxisMidValues = new float[m_valueAxisMidViews.size()];
    for (int i = 0; i < valueAxisMidValues.length; i++)
    {
      valueAxisMidValues[i] = Float.parseFloat(m_valueAxisMidViews.get(i).getText().toString());
    }
    state.putFloatArray("valueAxisMidValues", valueAxisMidValues);

    state.putLong("m_startTimestamp", m_startTimestamp);
    state.putLong("m_endTimestamp", m_endTimestamp);

    return state;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state)
  {
    if (state instanceof Bundle)
    {
      Bundle bundle = (Bundle)state;
      state = bundle.getParcelable("superState");

      setShowValueAxis(bundle.getBoolean("m_showValueAxis"));
      setValueAxisTextSizeSp(bundle.getFloat("m_valueAxisTextSizeSp"));
      setValueAxisTextColor(bundle.getInt("m_valueAxisTextColor"));
      setValueAxisMin(bundle.getFloat("m_valueAxisMin"), false);
      setValueAxisMax(bundle.getFloat("m_valueAxisMax"), false);

      setShowTimeAxis(bundle.getBoolean("m_showTimeAxis"));
      setTimeAxisTextSizeSp(bundle.getFloat("m_timeAxisTextSizeSp"));
      setTimeAxisTextColor(bundle.getInt("m_timeAxisTextColor"));
      setTimeAxisMarkerColor(bundle.getInt("m_timeAxisMarkerColor"));

      setShowNoDataText(bundle.getBoolean("m_showNoDataText"));
      setNoDataText(bundle.getCharSequence("m_noDataText"));
      setNoDataTextSizeSp(bundle.getFloat("m_noDataTextSizeSp"));
      setNoDataTextColor(bundle.getInt("m_noDataTextColor"));

      setShowRefreshProgress(bundle.getBoolean("m_showRefreshProgress"));
      setGraphBackgroundColor(bundle.getInt("m_graphBackgroundColor"));
      setDataLineColor(bundle.getInt("m_dataLineColor"));

      setAllowScroll(bundle.getBoolean("m_allowScroll"));
      setAllowScale(bundle.getBoolean("m_allowScale"));

      float[] rangeHighlightingValues = bundle.getFloatArray("m_rangeHighlightingValues");
      @ColorInt int[] rangeHighlightingColors = bundle.getIntArray("m_rangeHighlightingColors");
      if (rangeHighlightingValues != null && rangeHighlightingColors != null)
      {
        setRangeHighlights(rangeHighlightingValues,
                           rangeHighlightingColors,
                           bundle.getInt("m_rangeHighlightingDisplayMode"),
                           false);
      }

      setValueAxisMidLabels(bundle.getFloatArray("valueAxisMidValues"));

      m_startTimestamp = bundle.getLong("m_startTimestamp");
      m_endTimestamp = bundle.getLong("m_endTimestamp");
    }
    super.onRestoreInstanceState(state);
  }

  @Override
  protected void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight)
  {
    super.onSizeChanged(width, height, oldHeight, oldHeight);

    post(new Runnable()
    {
      @Override
      public void run()
      {
        if (width != oldWidth)
        {
          int timeAxisLabelCount = m_timeAxisLabels.size();
          for (int i = 0; i < timeAxisLabelCount; i++)
          {
            repositionTimeAxisLabel(m_timeAxisLabels.valueAt(i));
          }
          post(new Runnable()
          {
            @Override
            public void run()
            {
              if (height != oldHeight)
              {
                for (TextView textView : m_valueAxisMidViews)
                {
                  repositionValueAxisLabel(textView);
                }
              }
            }
          });
        }
        else if (height != oldHeight)
        {
          for (TextView textView : m_valueAxisMidViews)
          {
            repositionValueAxisLabel(textView);
          }
        }
      }
    });
  }

  public boolean hasEnoughData()
  {
    return m_hasEnoughData;
  }

  public void setDisallowHorizontalScrollViews(ViewGroup[] views)
  {
    m_graphSurfaceView.setDisallowHorizontalScrollViews(views);
  }

  public void setShowValueAxis(boolean value)
  {
    m_showValueAxis = value;
    int visibility = value ? View.VISIBLE : View.GONE;

    m_valueAxisMinView.setVisibility(visibility);
    m_valueAxisMaxView.setVisibility(visibility);
    for (TextView view : m_valueAxisMidViews)
    {
      view.setVisibility(visibility);
    }

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);
    constraintSet.clear(m_graphSurfaceView.getId(), ConstraintSet.START);
    if (value)
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT,
                            dpToPx(getContext(), VALUE_AXIS_MARGIN_DP));
    }
    else
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);

    post(new Runnable()
    {
      @Override
      public void run()
      {
        int timeAxisLabelCount = m_timeAxisLabels.size();
        for (int i = 0; i < timeAxisLabelCount; i++)
        {
          repositionTimeAxisLabel(m_timeAxisLabels.valueAt(i));
        }
      }
    });
  }

  public boolean getShowValueAxis()
  {
    return m_showValueAxis;
  }

  public void setValueAxisTextSizeSp(float textSizeSp)
  {
    m_valueAxisTextSizeSp = textSizeSp;

    m_valueAxisMinView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    m_valueAxisMaxView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    for (TextView textView : m_valueAxisMidViews)
    {
      textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        resizeViewForValueAxisLabels();

        float offset = m_valueAxisMaxView.getHeight() * 0.5f;
        m_valueAxisMinView.animate().translationY(0.0f).setDuration(0L).start();
        m_valueAxisMinView.animate().translationYBy(offset).setDuration(0L).start();
        m_valueAxisMaxView.animate().translationY(0.0f).setDuration(0L).start();
        m_valueAxisMaxView.animate().translationYBy(-offset).setDuration(0L).start();

        post(new Runnable()
        {
          @Override
          public void run()
          {
            for (TextView textView : m_valueAxisMidViews)
            {
              repositionValueAxisLabel(textView);
            }

            int timeAxisLabelCount = m_timeAxisLabels.size();
            for (int i = 0; i < timeAxisLabelCount; i++)
            {
              repositionTimeAxisLabel(m_timeAxisLabels.valueAt(i));
            }
          }
        });
      }
    });
  }

  public float getValueAxisTextSizeSp()
  {
    return m_valueAxisTextSizeSp;
  }

  private void setValueAxisTextColor(@ColorInt int color)
  {
    m_valueAxisTextColor = color;
    m_valueAxisMinView.setTextColor(color);
    m_valueAxisMaxView.setTextColor(color);
    for (TextView view : m_valueAxisMidViews)
    {
      view.setTextColor(color);
    }
  }

  public @ColorInt int getValueAxisTextColor()
  {
    return m_valueAxisTextColor;
  }

  public void setValueAxisMinMax(float min, float max, boolean animate)
  {
    if (min <= max)
    {
      throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
    }

    m_valueAxisMin = min;
    m_valueAxisMinView.setText(String.valueOf(min));
    setValueAxisMax(max, animate);
  }

  public void setValueAxisMin(float value, boolean animate)
  {
    if (value >= m_valueAxisMax)
    {
      throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
    }

    m_valueAxisMin = value;
    m_valueAxisMinView.setText(String.valueOf(value));

    refresh(animate);
  }

  public float getValueAxisMin()
  {
    return m_valueAxisMin;
  }

  public void setValueAxisMax(float value, boolean animate)
  {
    if (value <= m_valueAxisMin)
    {
      throw new IllegalArgumentException("Maximum value must be greater than minimum value.");
    }

    m_valueAxisMax = value;
    m_valueAxisMaxView.setText(String.valueOf(value));

    refresh(animate);
  }

  public float getValueAxisMax()
  {
    return m_valueAxisMax;
  }

  public void setShowTimeAxis(boolean value)
  {
    m_showTimeAxis = value;
    m_timeAxisLabelsLayoutView.setVisibility(value ? View.VISIBLE : View.GONE);

    post(new Runnable()
    {
      @Override
      public void run()
      {
        for (TextView textView : m_valueAxisMidViews)
        {
          repositionValueAxisLabel(textView);
        }
      }
    });
  }

  public boolean getShowTimeAxis()
  {
    return m_showTimeAxis;
  }

  public void setTimeAxisTextSizeSp(float textSizeSp)
  {
    m_timeAxisTextSizeSp = textSizeSp;
    int timeAxisLabelCount = m_timeAxisLabels.size();
    for (int i = 0; i < timeAxisLabelCount; i++)
    {
      m_timeAxisLabels.valueAt(i).view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        for (TextView textView : m_valueAxisMidViews)
        {
          repositionValueAxisLabel(textView);
        }
      }
    });
  }

  public float getTimeAxisTextSizeSp()
  {
    return m_timeAxisTextSizeSp;
  }

  public void setTimeAxisTextColor(@ColorInt int color)
  {
    m_timeAxisTextColor = color;
    int timeAxisLabelCount = m_timeAxisLabels.size();
    for (int i = 0; i < timeAxisLabelCount; i++)
    {
      m_timeAxisLabels.valueAt(i).view.setTextColor(color);
    }
  }

  public @ColorInt int getTimeAxisTextColor()
  {
    return m_timeAxisTextColor;
  }

  public void setTimeAxisMarkerColor(@ColorInt int color)
  {
    m_timeAxisMarkerColor = color;

    if (m_labelMarkersLine != null)
    {
      m_labelMarkersLine.setColor(color);
      m_graphSurfaceView.requestRender();
    }

    DrawableCompat.setTint(m_timeAxisLabelsBackground, color);
    int timeAxisLabelCount = m_timeAxisLabels.size();
    for (int i = 0; i < timeAxisLabelCount; i++)
    {
      m_timeAxisLabels.valueAt(i).view.invalidate();
    }
  }

  public @ColorInt int getTimeAxisMarkerColor()
  {
    return m_timeAxisMarkerColor;
  }

  public void setShowNoDataText(boolean value)
  {
    m_showNoDataText = value;
    if (!value)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }
    else if (m_endTimestamp - m_startTimestamp == 0L)
    {
      m_noDataView.setVisibility(View.VISIBLE);
    }
  }

  public boolean getShowNoDataText()
  {
    return m_showNoDataText;
  }

  public void setNoDataText(CharSequence text)
  {
    m_noDataText = text;
    m_noDataView.setText(text);
  }

  public CharSequence getNoDataText()
  {
    return m_noDataText;
  }

  public void setNoDataTextSizeSp(float textSizeSp)
  {
    m_noDataTextSizeSp = textSizeSp;
    m_noDataView.setTextSize(textSizeSp);
  }

  public float getNoDataTextSizeSp()
  {
    return m_timeAxisTextSizeSp;
  }

  public void setNoDataTextColor(int color)
  {
    m_noDataTextColor = color;
    m_noDataView.setTextColor(color);
  }

  public int getNoDataTextColor(int color)
  {
    return m_noDataTextColor;
  }

  public void setShowRefreshProgress(boolean value)
  {
    m_showRefreshProgress = value;
    m_refreshProgressView.setVisibility(value && m_refreshing ? View.VISIBLE : View.INVISIBLE);
  }

  public boolean getShowRefreshProgress()
  {
    return m_showRefreshProgress;
  }

  public void setGraphBackgroundColor(@ColorInt int color)
  {
    m_graphBackgroundColor = color;
    m_graphSurfaceView.setBackgroundColor(color);
  }

  public @ColorInt int getGraphBackgroundColor()
  {
    return m_graphSurfaceView.getBackgroundColor();
  }

  public void setDataLineColor(@ColorInt int color)
  {
    m_dataLineColor = color;
    if (m_dataLineStrip != null)
    {
      m_dataLineStrip.setColor(m_dataLineColor);
      m_graphSurfaceView.requestRender();
    }
  }

  public @ColorInt int getDataLineColor()
  {
    return m_dataLineColor;
  }

  public void setAllowScroll(boolean allow)
  {
    m_allowScroll = allow;
  }

  public boolean getAllowScroll()
  {
    return m_allowScroll;
  }

  public void setAllowScale(boolean allow)
  {
    m_allowScale = allow;
  }

  public boolean getAllowScale()
  {
    return m_allowScale;
  }

  public void setValueAxisMidLabels(final float[] midValues)
  {
    if (midValues != null)
    {
      post(new Runnable()
      {
        @Override
        public void run()
        {
          int index = 0;
          for (; index < midValues.length; index++)
          {
            if (midValues[index] < m_valueAxisMin)
            {
              throw new IllegalArgumentException("'midValues' array must not contain values lower than the minimum.");
            }
            if (midValues[index] > m_valueAxisMax)
            {
              throw new IllegalArgumentException("'midValues' array must not contain values greater than the maximum.");
            }

            TextView textView;
            if (index >= m_valueAxisMidViews.size())
            {
              textView = createTextView(getContext(), m_valueAxisTextSizeSp, m_valueAxisTextColor);
              addView(textView);
              m_valueAxisMidViews.add(textView);
            }
            else
            {
              textView = m_valueAxisMidViews.get(index);
              if (textView == null)
              {
                textView = createTextView(getContext(), m_valueAxisTextSizeSp, m_valueAxisTextColor);
                addView(textView);
                m_valueAxisMidViews.set(index, textView);
              }
            }

            textView.setText(String.valueOf(midValues[index]));
            repositionValueAxisLabel(textView);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(TimeGraph.this);
            constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, m_valueAxisMaxView.getId(), ConstraintSet.RIGHT);
            constraintSet.setHorizontalBias(textView.getId(), 1.0f);
            constraintSet.applyTo(TimeGraph.this);
          }

          for (int i = m_valueAxisMidViews.size() - 1; i >= index; i--)
          {
            removeView(m_valueAxisMidViews.get(i));
            m_valueAxisMidViews.remove(i);
          }
        }
      });
    }
  }

  private void repositionValueAxisLabel(TextView textView)
  {
    float value = Float.parseFloat(textView.getText().toString());
    float height = ((m_valueAxisMax - value) / (m_valueAxisMax - m_valueAxisMin)) * m_graphSurfaceView.getHeight();
    textView.animate().translationY(0.0f).setDuration(0).start();
    textView.animate().translationYBy(height).setDuration(0).start();
  }

  private void resizeViewForValueAxisLabels()
  {
    int topMargin = (int)Math.ceil(m_valueAxisMaxView.getHeight() * 0.5f);
    int bottomMargin = Math.max(topMargin - m_timeAxisLabelsLayoutView.getHeight(), 0);

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(TimeGraph.this);
    constraintSet.connect(m_graphSurfaceView.getId(),
                          ConstraintSet.TOP,
                          ConstraintSet.PARENT_ID,
                          ConstraintSet.TOP,
                          topMargin);
    constraintSet.connect(m_timeAxisLabelsLayoutView.getId(),
                          ConstraintSet.BOTTOM,
                          ConstraintSet.PARENT_ID,
                          ConstraintSet.BOTTOM,
                          bottomMargin);
    constraintSet.applyTo(TimeGraph.this);
  }

  private void setTimeAxisLabels(final TimeAxisLabelData[] timeAxisLabelData)
  {
    if (timeAxisLabelData != null && timeAxisLabelData.length > 0)
    {
      post(new Runnable()
      {
        @Override
        public void run()
        {
          final int initialTimeAxisHeight = m_timeAxisLabelsLayoutView.getHeight();

          // Add the new labels and create graph markers.
          int anchorIndex = -1;
          float[] labelMarkerCoords = new float[timeAxisLabelData.length * 4];
          for (int i = 0; i < timeAxisLabelData.length; i++)
          {
            TimeAxisLabel label = m_timeAxisLabels.get(timeAxisLabelData[i].timestamp);
            if (label == null)
            {
              label = new TimeAxisLabel(timeAxisLabelData[i].timestamp, createTextView(getContext(), m_timeAxisTextSizeSp, m_timeAxisTextColor));
              label.view.setBackground(m_timeAxisLabelsBackground);
              label.view.setPadding(dpToPx(getContext(), 3), 0, 0, 0);
              m_timeAxisLabelsLayoutView.addView(label.view);
              m_timeAxisLabels.append(label.timestamp, label);
            }
            else if (label.view.getVisibility() == View.VISIBLE && anchorIndex == -1)
            {
              anchorIndex = i;
            }
            repositionTimeAxisLabel(label);
            label.view.setText(timeAxisLabelData[i].label);

            int coordsIndex = i * 4;
            float markerX = (((label.offset / m_graphSurfaceView.getWidth()) * 2.0f) - 1.0f) + 0.001f;
            labelMarkerCoords[coordsIndex] = markerX;
            labelMarkerCoords[coordsIndex + 1] = -1.0f;
            labelMarkerCoords[coordsIndex + 2] = markerX;
            labelMarkerCoords[coordsIndex + 3] = -0.9f;
          }

          LineRenderable oldLabelMarkersLine = m_labelMarkersLine;
          m_labelMarkersLine = m_graphSurfaceView.addLine(1, labelMarkerCoords, m_timeAxisMarkerColor);
          if (oldLabelMarkersLine != null)
          {
            m_graphSurfaceView.removeRenderable(oldLabelMarkersLine);
          }

          // Remove the unused labels.
          if (anchorIndex != -1)
          {
            // Same time scale.
            int removeAfterIndex = m_timeAxisLabels.indexOfKey(timeAxisLabelData[timeAxisLabelData.length - 1].timestamp);
            for (int i = m_timeAxisLabels.size() - 1; i > removeAfterIndex; i--)
            {
              m_timeAxisLabelsLayoutView.removeView(m_timeAxisLabels.valueAt(i).view);
              m_timeAxisLabels.removeAt(i);
            }
            int removeBeforeIndex = m_timeAxisLabels.indexOfKey(timeAxisLabelData[0].timestamp);
            for (int i = removeBeforeIndex - 1; i >= 0; i--)
            {
              m_timeAxisLabelsLayoutView.removeView(m_timeAxisLabels.valueAt(i).view);
              m_timeAxisLabels.removeAt(i);
            }
          }
          else
          {
            // Different time scale.
            int dataIndex = 0;
            int incorrectTimeAxisLabelCount = m_timeAxisLabels.size();
            for (int i = 0; i < incorrectTimeAxisLabelCount; i++)
            {
              // Race condition: bail out here to ensure the app doesn't crash.
              if (dataIndex >= timeAxisLabelData.length)
              {
                break;
              }

              TimeAxisLabel label = m_timeAxisLabels.valueAt(i);
              if (label.timestamp != timeAxisLabelData[dataIndex].timestamp)
              {
                m_timeAxisLabelsLayoutView.removeView(label.view);
                m_timeAxisLabels.removeAt(i);
                incorrectTimeAxisLabelCount--;
                i--;
              }
              else
              {
                dataIndex++;
              }
            }
          }

          final int finalAnchorIndex = anchorIndex;
          post(new Runnable()
          {
            @Override
            public void run()
            {
              setTimeAxisLabelsVisibility(finalAnchorIndex);

              if (initialTimeAxisHeight != m_timeAxisLabelsLayoutView.getHeight())
              {
                resizeViewForValueAxisLabels();

                post(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    for (TextView textView : m_valueAxisMidViews)
                    {
                      repositionValueAxisLabel(textView);
                    }
                  }
                });
              }
            }
          });
        }
      });
    }
  }

  private void repositionTimeAxisLabel(TimeAxisLabel label)
  {
    double difference = (double)(m_endTimestamp - m_startTimestamp);
    float widthMultiplier = 1.0f - (float)((double)(m_endTimestamp - label.timestamp) / difference);
    float offset = widthMultiplier * m_graphSurfaceView.getWidth();
    label.view.animate().translationX(offset).setDuration(0).start();
    label.offset = offset;
  }

  private void setTimeAxisLabelsVisibility(int anchorIndex)
  {
    int timeAxisLabelCount = m_timeAxisLabels.size();
    if (anchorIndex < 0)
    {
      anchorIndex = 0;
    }
    else if (anchorIndex >= timeAxisLabelCount)
    {
      anchorIndex = timeAxisLabelCount - 1;
    }

    float lastOffset = -Float.MAX_VALUE;
    for (int i = anchorIndex; i < timeAxisLabelCount; i++)
    {
      TimeAxisLabel label = m_timeAxisLabels.valueAt(i);
      float width = label.view.getWidth() * TIME_AXIS_LABEL_WIDTH_MODIFIER;
      if (label.offset - width < lastOffset)
      {
        label.view.setVisibility(View.INVISIBLE);
      }
      else
      {
        label.view.setVisibility(View.VISIBLE);
        lastOffset = label.offset;
      }
    }
    lastOffset = Float.MAX_VALUE;
    for (int i = anchorIndex; i >= 0; i--)
    {
      TimeAxisLabel label = m_timeAxisLabels.valueAt(i);
      float width = label.view.getWidth() * TIME_AXIS_LABEL_WIDTH_MODIFIER;
      if (label.offset + width > lastOffset)
      {
        label.view.setVisibility(View.INVISIBLE);
      }
      else
      {
        label.view.setVisibility(View.VISIBLE);
        lastOffset = label.offset;
      }
    }
  }

  public void setRangeHighlights(@NonNull float[] values, @NonNull int[] colors, @DisplayMode int displayMode, boolean animate)
  {
    if (values.length <= 1)
    {
      throw new IllegalArgumentException("Insufficient range highlight values have been provided.");
    }

    if (values.length != colors.length + 1)
    {
      throw new IllegalArgumentException("The number of range highlight colors must be one fewer than the number of range highlight values.");
    }

    for (int i = 1; i < values.length; i++)
    {
      if (values[i] < values[i - 1])
      {
        throw new IllegalArgumentException("Range highlight values must be provided in ascending order.");
      }
    }

    m_rangeHighlightingValues = values;
    m_rangeHighlightingColors = colors;
    m_rangeHighlightingDisplayMode = displayMode;

    refresh(animate);
  }

  public void clearRangeHighlights()
  {
    m_rangeHighlightingValues = null;
    m_rangeHighlightingColors = null;

    if (m_rangeHighlightMesh != null)
    {
      m_graphSurfaceView.removeRenderable(m_rangeHighlightMesh);
      m_rangeHighlightMesh = null;
    }
  }

  public void setVisibleDataPeriod(long startTimestamp, long endTimestamp, @NonNull final GraphDataProvider dataProvider, boolean animate)
  {
    m_startTimestamp = startTimestamp;
    m_endTimestamp = endTimestamp;
    m_dataProvider = dataProvider;

    refresh(animate);
  }

  public long getVisibleStartTimestamp()
  {
    return m_startTimestamp;
  }

  public long getVisibleEndTimestamp()
  {
    return m_endTimestamp;
  }

  public void clearData()
  {
    m_startTimestamp = 0L;
    m_endTimestamp = 0L;
    m_dataProvider = null;

    refresh(false);
  }

  public void refresh(GraphDataProvider dataProvider, boolean animateNew)
  {
    m_dataProvider = dataProvider;
    refresh(animateNew);
  }

  public void refresh(boolean animateNew)
  {
    m_normalisedForcedXCentre = -1.0f;
    final long timeDifference = m_endTimestamp - m_startTimestamp;
    if (timeDifference > 0L)
    {
      createDataRepresentations(timeDifference, animateNew);
    }
    else
    {
      clearDataRepresentations();
    }
  }

  private void createDataRepresentations(final long timeDifference, final boolean animate)
  {
    if (m_refreshing)
    {
      m_newRefreshRequested = true;
    }
    else
    {
      m_refreshing = true;
      m_newRefreshRequested = true;
      post(new Runnable()
      {
        @Override
        public void run()
        {
          if (m_showRefreshProgress)
          {
            m_refreshProgressView.setVisibility(View.VISIBLE);
          }
          m_noDataView.setVisibility(View.INVISIBLE);
        }
      });

      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          float valueDifference = m_valueAxisMax - m_valueAxisMin;

          GraphData[] data = null;
          if (m_dataProvider != null)
          {
            while (m_newRefreshRequested)
            {
              m_newRefreshRequested = false;
              data = m_dataProvider.getData(TimeGraph.this,
                                            m_startTimestamp - timeDifference,
                                            m_endTimestamp + timeDifference,
                                            m_startTimestamp,
                                            m_endTimestamp);
            }

            m_hasEnoughData = data != null && data.length > 1;
            if (m_hasEnoughData)
            {
              m_firstGraphDataEntry = data[0];
              m_lastGraphDataEntry = data[data.length - 1];
              float floatTimeDifference = (float)timeDifference;
              if (m_firstGraphDataEntry.timestamp > m_startTimestamp)
              {
                m_startTimestamp = m_firstGraphDataEntry.timestamp;
                floatTimeDifference = (float)(m_endTimestamp - m_startTimestamp);
              }
              if (m_lastGraphDataEntry.timestamp < m_endTimestamp)
              {
                m_endTimestamp = m_lastGraphDataEntry.timestamp;
                floatTimeDifference = (float)(m_endTimestamp - m_startTimestamp);
              }
              if (m_onPeriodChangeListener != null)
              {
                m_onPeriodChangeListener.onPeriodChanged(TimeGraph.this, m_startTimestamp, m_endTimestamp);
              }
              setTimeAxisLabels(m_dataProvider.getLabelsForData(data));

              float startingYScale = animate ? 0.0f : 1.0f;
              createDataLineStrip(data, floatTimeDifference, valueDifference, startingYScale);
              if (m_rangeHighlightingValues != null && m_rangeHighlightingColors != null)
              {
                createHighlightMesh(data, floatTimeDifference, valueDifference, startingYScale);
              }

              m_xOffset = 0.0f;
              m_xScale = 1.0f;
              m_beforeScalingStartTimestamp = Long.MIN_VALUE;
              m_beforeScalingEndTimestamp = Long.MAX_VALUE;
            }
            else
            {
              if (m_dataLineStrip != null)
              {
                m_graphSurfaceView.removeRenderable(m_dataLineStrip);
              }
              if (m_labelMarkersLine != null)
              {
                m_graphSurfaceView.removeRenderable(m_labelMarkersLine);
              }
              if (m_rangeHighlightMesh != null)
              {
                m_graphSurfaceView.removeRenderable(m_rangeHighlightMesh);
              }
            }
          }

          if (m_onRefreshListener != null)
          {
            m_onRefreshListener.onRefresh(TimeGraph.this, m_startTimestamp, m_endTimestamp, data);
          }

          m_refreshing = false;
          final boolean dataApplied = data != null && data.length > 1;
          post(new Runnable()
          {
            @Override
            public void run()
            {
              m_refreshProgressView.setVisibility(View.INVISIBLE);
              m_noDataView.setVisibility(!dataApplied && m_showNoDataText ? View.VISIBLE : View.INVISIBLE);
            }
          });

          if (animate)
          {
            post(new Runnable()
            {
              @Override
              public void run()
              {
                m_newDataAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                m_newDataAnimator.setDuration(NEW_DATA_ANIMATION_DURATION);
                m_newDataAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
                {
                  @Override
                  public void onAnimationUpdate(ValueAnimator valueAnimator)
                  {
                    float animatedValue = (float)valueAnimator.getAnimatedValue();
                    if (m_dataLineStrip != null)
                    {
                      m_dataLineStrip.setYScale(animatedValue);
                    }
                    if (m_rangeHighlightMesh != null)
                    {
                      m_rangeHighlightMesh.setYScale(animatedValue);
                    }
                    m_graphSurfaceView.requestRender();
                  }
                });
                m_newDataAnimator.start();
              }
            });
          }
        }
      });
    }
  }

  private void clearDataRepresentations()
  {
    m_startTimestamp = 0L;
    m_endTimestamp = 0L;

    m_hasEnoughData = false;
    if (m_dataLineStrip != null)
    {
      m_graphSurfaceView.removeRenderable(m_dataLineStrip);
      m_dataLineStrip = null;
    }
    if (m_rangeHighlightMesh != null)
    {
      m_graphSurfaceView.removeRenderable(m_rangeHighlightMesh);
      m_rangeHighlightMesh = null;
    }
    if (m_labelMarkersLine != null)
    {
      m_graphSurfaceView.removeRenderable(m_labelMarkersLine);
      m_labelMarkersLine = null;
    }
    setTimeAxisLabels(new TimeAxisLabelData[0]);
    post(new Runnable()
    {
      @Override
      public void run()
      {
        m_noDataView.setVisibility(m_showNoDataText ? View.VISIBLE : View.INVISIBLE);
      }
    });

    if (m_onRefreshListener != null)
    {
      m_onRefreshListener.onRefresh(this, 0L, 0L, null);
    }
  }

  private void createDataLineStrip(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    final ArrayList<ClickableDataPoint> clickableDataPoints = new ArrayList<>();
    float[] coords = new float[data.length * Renderable.COORDS_PER_VERTEX];
    int coordsIndex = 0;
    for (GraphData datum : data)
    {
      float xCoord = (datum.timestamp - m_startTimestamp) / timeDifference;
      float yCoord = (datum.value - m_valueAxisMin) / valueDifference;
      coords[coordsIndex] = (xCoord * 2.0f) - 1.0f;
      coords[coordsIndex + 1] = (yCoord * 2.0f) - 1.0f;
      coordsIndex += 2;

      if (datum.timestamp >= m_startTimestamp && datum.timestamp <= m_endTimestamp)
      {
        clickableDataPoints.add(new ClickableDataPoint(xCoord, 1.0f - yCoord, datum));
      }
    }
    LineStripRenderable oldDataLine = m_dataLineStrip;
    m_dataLineStrip = m_graphSurfaceView.addLineStrip(1, coords, Color.BLACK);
    m_dataLineStrip.setColor(m_dataLineColor);
    m_dataLineStrip.setYScale(startingYScale);
    if (oldDataLine != null)
    {
      m_graphSurfaceView.removeRenderable(oldDataLine);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        m_graphSurfaceView.setClickablePoints(clickableDataPoints);
      }
    });
  }

  private void createHighlightMesh(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    switch (m_rangeHighlightingDisplayMode)
    {
    case DISPLAY_MODE_BACKGROUND:
      createRangeHighlightMeshBackground(data, timeDifference, valueDifference, startingYScale);
      break;
    case DISPLAY_MODE_BACKGROUND_WITH_FADE:
      createRangeHighlightMeshBackgroundWithFade(data, timeDifference, valueDifference, startingYScale);
      break;
    case DISPLAY_MODE_UNDERLINE:
      createRangeHighlightMeshUnderline(data, timeDifference, valueDifference, startingYScale);
      break;
    case DISPLAY_MODE_UNDERLINE_WITH_FADE:
      createRangeHighlightMeshUnderlineWithFade(data, timeDifference, valueDifference, startingYScale);
      break;
    default:
      throw new IllegalStateException("Unknown range highlighting display mode value.");
    }
  }

  private void createRangeHighlightMeshBackground(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    float[] coordArray = new float[m_rangeHighlightingColors.length * 4 * Renderable.COORDS_PER_VERTEX];
    short[] indexArray = new short[m_rangeHighlightingColors.length * 6];
    float[] colorArray = new float[m_rangeHighlightingColors.length * 4 * Renderable.COLORS_PER_VERTEX];

    int coordStartIndex = 0;
    int indexStartIndex = 0;
    int colorStartIndex = 0;
    short indexStart = 0;
    for (int i = 0; i < m_rangeHighlightingColors.length; i++)
    {
      float normalisedRangeStart = (m_rangeHighlightingValues[i] - m_valueAxisMin) / valueDifference;
      float normalisedRangeEnd = (m_rangeHighlightingValues[i + 1] - m_valueAxisMin) / valueDifference;
      coordArray[coordStartIndex] = -1.0f;
      coordArray[coordStartIndex + 1] = (normalisedRangeStart * 2.0f) - 1.0f;
      coordArray[coordStartIndex + 2] = -1.0f;
      coordArray[coordStartIndex + 3] = (normalisedRangeEnd * 2.0f) - 1.0f;
      coordArray[coordStartIndex + 4] = 1.0f;
      coordArray[coordStartIndex + 5] = (normalisedRangeEnd * 2.0f) - 1.0f;
      coordArray[coordStartIndex + 6] = 1.0f;
      coordArray[coordStartIndex + 7] = (normalisedRangeStart * 2.0f) - 1.0f;
      coordStartIndex += 8;

      indexArray[indexStartIndex] = indexStart;
      indexArray[indexStartIndex + 1] = (short)(indexStart + 1);
      indexArray[indexStartIndex + 2] = (short)(indexStart + 2);
      indexArray[indexStartIndex + 3] = indexStart;
      indexArray[indexStartIndex + 4] = (short)(indexStart + 2);
      indexArray[indexStartIndex + 5] = (short)(indexStart + 3);
      indexStartIndex += 6;

      for (int j = 0; j < 4; j++)
      {
        colorArray[colorStartIndex] = Color.red(m_rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 1] = Color.green(m_rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 2] = Color.blue(m_rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 3] = 1.0f;
        colorStartIndex += 4;
      }

      indexStart += 4;
    }

    replaceMesh(startingYScale, coordArray, indexArray, colorArray);
  }

  private void createRangeHighlightMeshBackgroundWithFade(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    float[] rangeHighlightingValues = modifyRangeHighlightingValuesForFade(valueDifference);
    int[] rangeHighlightingColors = modifyRangeHighlightingColorsForFade(rangeHighlightingValues.length);

    float[] coordArray = new float[rangeHighlightingValues.length * 2 * Renderable.COORDS_PER_VERTEX];
    short[] indexArray = new short[(rangeHighlightingValues.length - 1) * 6];
    float[] colorArray = new float[rangeHighlightingValues.length * 2 * Renderable.COLORS_PER_VERTEX];

    float firstNormalisedRangeValue = (rangeHighlightingValues[0] - m_valueAxisMin) / valueDifference;
    coordArray[0] = -1.0f;
    coordArray[1] = (firstNormalisedRangeValue * 2.0f) - 1.0f;
    coordArray[2] = 1.0f;
    coordArray[3] = (firstNormalisedRangeValue * 2.0f) - 1.0f;
    for (int i = 0; i < 2; i++)
    {
      colorArray[(i * 4)] = Color.red(rangeHighlightingColors[0]) / 255.0f;
      colorArray[(i * 4) + 1] = Color.green(rangeHighlightingColors[0]) / 255.0f;
      colorArray[(i * 4) + 2] = Color.blue(rangeHighlightingColors[0]) / 255.0f;
      colorArray[(i * 4) + 3] = 1.0f;
    }
    int coordStartIndex = 4;
    int indexStartIndex = 0;
    int colorStartIndex = 8;

    short indexStart = 0;
    for (int i = 1; i < rangeHighlightingColors.length; i++)
    {
      float normalisedRangeEnd = (rangeHighlightingValues[i] - m_valueAxisMin) / valueDifference;
      coordArray[coordStartIndex] = -1.0f;
      coordArray[coordStartIndex + 1] = (normalisedRangeEnd * 2.0f) - 1.0f;
      coordArray[coordStartIndex + 2] = 1.0f;
      coordArray[coordStartIndex + 3] = (normalisedRangeEnd * 2.0f) - 1.0f;
      coordStartIndex += 4;

      indexArray[indexStartIndex] = indexStart;
      indexArray[indexStartIndex + 1] = (short)(indexStart + 1);
      indexArray[indexStartIndex + 2] = (short)(indexStart + 3);
      indexArray[indexStartIndex + 3] = indexStart;
      indexArray[indexStartIndex + 4] = (short)(indexStart + 3);
      indexArray[indexStartIndex + 5] = (short)(indexStart + 2);
      indexStartIndex += 6;

      for (int j = 0; j < 2; j++)
      {
        colorArray[colorStartIndex] = Color.red(rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 1] = Color.green(rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 2] = Color.blue(rangeHighlightingColors[i]) / 255.0f;
        colorArray[colorStartIndex + 3] = 1.0f;
        colorStartIndex += 4;
      }

      indexStart += 2;
    }

    replaceMesh(startingYScale, coordArray, indexArray, colorArray);
  }

  private void createRangeHighlightMeshUnderline(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    ArrayList<Float> coords = new ArrayList<>();
    ArrayList<Short> indices = new ArrayList<>();
    ArrayList<Float> colors = new ArrayList<>();
    short indexStart = 0;

    for (int dataIndex = 0; dataIndex < data.length - 1; dataIndex++)
    {
      GraphData start = data[dataIndex];
      float startXCoord = (start.timestamp - m_startTimestamp) / timeDifference;
      float startYCoord = (start.value - m_valueAxisMin) / valueDifference;
      GraphData end = data[dataIndex + 1];
      float endXCoord = (end.timestamp - m_startTimestamp) / timeDifference;
      float endYCoord = (end.value - m_valueAxisMin) / valueDifference;
      if (endYCoord < startYCoord)
      {
        float tempXCoord = startXCoord;
        float tempYCoord = startYCoord;
        startXCoord = endXCoord;
        startYCoord = endYCoord;
        endXCoord = tempXCoord;
        endYCoord = tempYCoord;
      }

      int indexReached = 1;
      boolean finish = false;
      for (; indexReached < m_rangeHighlightingValues.length && !finish; indexReached++)
      {
        float normalisedRangeStart = (m_rangeHighlightingValues[indexReached - 1] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (m_rangeHighlightingValues[indexReached] - m_valueAxisMin) / valueDifference;
        if (normalisedRangeEnd > startYCoord)
        {
          normalisedRangeEnd = startYCoord;
          finish = true;
        }

        // Under quad
        coords.add(startXCoord);
        coords.add(normalisedRangeStart);
        coords.add(endXCoord);
        coords.add(normalisedRangeStart);
        coords.add(endXCoord);
        coords.add(normalisedRangeEnd);
        coords.add(startXCoord);
        coords.add(normalisedRangeEnd);

        indices.add(indexStart);
        indices.add((short)(indexStart + 1));
        indices.add((short)(indexStart + 2));
        indices.add(indexStart);
        indices.add((short)(indexStart + 3));
        indices.add((short)(indexStart + 2));
        indexStart += 4;

        float r = Color.red(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
        float g = Color.green(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
        float b = Color.blue(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
        for (int i = 0; i < 4; i++)
        {
          colors.add(r);
          colors.add(g);
          colors.add(b);
          colors.add(1.0f);
        }
      }

      indexReached--;
      float lastX = startXCoord;
      float lastY = startYCoord;
      for (; indexReached < m_rangeHighlightingValues.length; indexReached++)
      {
        float normalisedRangeStart = (m_rangeHighlightingValues[indexReached - 1] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (m_rangeHighlightingValues[indexReached] - m_valueAxisMin) / valueDifference;
        PointF intersection = new PointF();
        if (getRangeIntersection(startXCoord, startYCoord, endXCoord, endYCoord, normalisedRangeEnd, intersection))
        {
          // Intersect quad.
          coords.add(lastX);
          coords.add(lastY);
          coords.add(endXCoord);
          coords.add(lastY);
          coords.add(endXCoord);
          coords.add(intersection.y);
          coords.add(intersection.x);
          coords.add(intersection.y);

          indices.add(indexStart);
          indices.add((short)(indexStart + 1));
          indices.add((short)(indexStart + 2));
          indices.add(indexStart);
          indices.add((short)(indexStart + 3));
          indices.add((short)(indexStart + 2));
          indexStart += 4;

          float topR = Color.red(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
          float topG = Color.green(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
          float topB = Color.blue(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
          for (int i = 0; i < 4; i++)
          {
            colors.add(topR);
            colors.add(topG);
            colors.add(topB);
            colors.add(1.0f);
          }

          lastX = intersection.x;
          lastY = intersection.y;
        }
        else
        {
          break;
        }
      }

      // Peak tri.
      coords.add(lastX);
      coords.add(lastY);
      coords.add(endXCoord);
      coords.add(lastY);
      coords.add(endXCoord);
      coords.add(endYCoord);

      indices.add(indexStart);
      indices.add((short)(indexStart + 1));
      indices.add((short)(indexStart + 2));
      indexStart += 3;

      float r = Color.red(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
      float g = Color.green(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
      float b = Color.blue(m_rangeHighlightingColors[indexReached - 1]) / 255.0f;
      for (int i = 0; i < 3; i++)
      {
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
      }
    }

    createVariableSizedMesh(startingYScale, coords, indices, colors);
  }

  private void createRangeHighlightMeshUnderlineWithFade(GraphData[] data, float timeDifference, float valueDifference, float startingYScale)
  {
    float[] rangeHighlightingValues = modifyRangeHighlightingValuesForFade(valueDifference);
    int[] rangeHighlightingColors = modifyRangeHighlightingColorsForFade(rangeHighlightingValues.length);
    ArrayList<Float> coords = new ArrayList<>();
    ArrayList<Short> indices = new ArrayList<>();
    ArrayList<Float> colors = new ArrayList<>();
    short indexStart = 0;

    for (int dataIndex = 0; dataIndex < data.length - 1; dataIndex++)
    {
      GraphData start = data[dataIndex];
      float startXCoord = (start.timestamp - m_startTimestamp) / timeDifference;
      float startYCoord = (start.value - m_valueAxisMin) / valueDifference;
      GraphData end = data[dataIndex + 1];
      float endXCoord = (end.timestamp - m_startTimestamp) / timeDifference;
      float endYCoord = (end.value - m_valueAxisMin) / valueDifference;
      if (endYCoord < startYCoord)
      {
        float tempXCoord = startXCoord;
        float tempYCoord = startYCoord;
        startXCoord = endXCoord;
        startYCoord = endYCoord;
        endXCoord = tempXCoord;
        endYCoord = tempYCoord;
      }

      int indexReached = 1;
      boolean finish = false;

      float firstNormalisedRangeValue = (rangeHighlightingValues[0] - m_valueAxisMin) / valueDifference;
      coords.add(endXCoord);
      coords.add(firstNormalisedRangeValue);
      coords.add(startXCoord);
      coords.add(firstNormalisedRangeValue);
      float firstR = Color.red(rangeHighlightingColors[0]) / 255.0f;
      float firstG = Color.green(rangeHighlightingColors[0]) / 255.0f;
      float firstB = Color.blue(rangeHighlightingColors[0]) / 255.0f;
      for (int j = 0; j < 2; j++)
      {
        colors.add(firstR);
        colors.add(firstB);
        colors.add(firstG);
        colors.add(1.0f);
      }

      for (; indexReached < rangeHighlightingValues.length && !finish; indexReached++)
      {
        float colorInterpolation = 0.0f;
        float normalisedRangeStart = (rangeHighlightingValues[indexReached - 1] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (rangeHighlightingValues[indexReached] - m_valueAxisMin) / valueDifference;
        if (normalisedRangeEnd > startYCoord)
        {
          colorInterpolation = (normalisedRangeEnd - startYCoord) / (normalisedRangeEnd - normalisedRangeStart);
          normalisedRangeEnd = startYCoord;
          finish = true;
        }

        // Under quad
        coords.add(endXCoord);
        coords.add(normalisedRangeEnd);
        coords.add(startXCoord);
        coords.add(normalisedRangeEnd);

        indices.add(indexStart);
        indices.add((short)(indexStart + 1));
        indices.add((short)(indexStart + 3));
        indices.add(indexStart);
        indices.add((short)(indexStart + 2));
        indices.add((short)(indexStart + 3));
        indexStart += 2;

        float bottomR = Color.red(rangeHighlightingColors[indexReached - 1]) / 255.0f;
        float bottomG = Color.green(rangeHighlightingColors[indexReached - 1]) / 255.0f;
        float bottomB = Color.blue(rangeHighlightingColors[indexReached - 1]) / 255.0f;
        float topR = lerp(Color.red(rangeHighlightingColors[indexReached]) / 255.0f, bottomR, colorInterpolation);
        float topG = lerp(Color.green(rangeHighlightingColors[indexReached]) / 255.0f, bottomG, colorInterpolation);
        float topB = lerp(Color.blue(rangeHighlightingColors[indexReached]) / 255.0f, bottomB, colorInterpolation);
        for (int i = 0; i < 2; i++)
        {
          colors.add(topR);
          colors.add(topG);
          colors.add(topB);
          colors.add(1.0f);
        }
      }

      indexReached--;
      for (; indexReached < rangeHighlightingValues.length; indexReached++)
      {
        float normalisedRangeStart = (rangeHighlightingValues[indexReached - 1] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (rangeHighlightingValues[indexReached] - m_valueAxisMin) / valueDifference;
        PointF intersection = new PointF();
        if (getRangeIntersection(startXCoord, startYCoord, endXCoord, endYCoord, normalisedRangeEnd, intersection))
        {
          // Intersect quad.
          coords.add(endXCoord);
          coords.add(intersection.y);
          coords.add(intersection.x);
          coords.add(intersection.y);

          indices.add(indexStart);
          indices.add((short)(indexStart + 1));
          indices.add((short)(indexStart + 3));
          indices.add(indexStart);
          indices.add((short)(indexStart + 2));
          indices.add((short)(indexStart + 3));
          indexStart += 2;

          float topR = Color.red(rangeHighlightingColors[indexReached]) / 255.0f;
          float topG = Color.green(rangeHighlightingColors[indexReached]) / 255.0f;
          float topB = Color.blue(rangeHighlightingColors[indexReached]) / 255.0f;
          for (int i = 0; i < 2; i++)
          {
            colors.add(topR);
            colors.add(topG);
            colors.add(topB);
            colors.add(1.0f);
          }
        }
        else
        {
          break;
        }
      }

      // Peak tri.
      coords.add(endXCoord);
      coords.add(endYCoord);

      indices.add(indexStart);
      indices.add((short)(indexStart + 1));
      indices.add((short)(indexStart + 2));

      float normalisedRangeStart = (rangeHighlightingValues[indexReached - 1] - m_valueAxisMin) / valueDifference;
      float normalisedRangeEnd = (rangeHighlightingValues[indexReached] - m_valueAxisMin) / valueDifference;
      float colorInterpolation = (normalisedRangeEnd - endYCoord) / (normalisedRangeEnd - normalisedRangeStart);
      float bottomR = Color.red(rangeHighlightingColors[indexReached - 1]) / 255.0f;
      float bottomG = Color.green(rangeHighlightingColors[indexReached - 1]) / 255.0f;
      float bottomB = Color.blue(rangeHighlightingColors[indexReached - 1]) / 255.0f;
      float topR = lerp(Color.red(rangeHighlightingColors[indexReached]) / 255.0f, bottomR, colorInterpolation);
      float topG = lerp(Color.green(rangeHighlightingColors[indexReached]) / 255.0f, bottomG, colorInterpolation);
      float topB = lerp(Color.blue(rangeHighlightingColors[indexReached]) / 255.0f, bottomB, colorInterpolation);
      colors.add(topR);
      colors.add(topG);
      colors.add(topB);
      colors.add(1.0f);

      indexStart = (short)(coords.size() / 2);
    }

    createVariableSizedMesh(startingYScale, coords, indices, colors);
  }

  private float[] modifyRangeHighlightingValuesForFade(float valueDifference)
  {
    float[] rangeHighlightingValues = new float[m_rangeHighlightingColors.length * 2];
    rangeHighlightingValues[0] = m_rangeHighlightingValues[0];
    rangeHighlightingValues[rangeHighlightingValues.length - 1] = m_rangeHighlightingValues[m_rangeHighlightingValues.length - 1];

    float baseModifier = valueDifference * HALF_FADE_MULTIPLIER;
    int rangeHighlightValuesIndex = 1;
    for (int i = 1; i < m_rangeHighlightingValues.length - 1; i++)
    {
      float lowerModifier = Math.min(baseModifier, m_rangeHighlightingValues[i] - m_rangeHighlightingValues[i - 1]);
      float upperModifier = Math.min(baseModifier, m_rangeHighlightingValues[i + 1] - m_rangeHighlightingValues[i]);
      rangeHighlightingValues[rangeHighlightValuesIndex] = m_rangeHighlightingValues[i] - lowerModifier;
      rangeHighlightingValues[rangeHighlightValuesIndex + 1] = m_rangeHighlightingValues[i] + upperModifier;
      rangeHighlightValuesIndex += 2;
    }
    return rangeHighlightingValues;
  }

  private int[] modifyRangeHighlightingColorsForFade(int rangeHighlightingValuesLength)
  {
    int[] rangeHighlightingColors = new int[rangeHighlightingValuesLength];
    for (int i = 0; i < rangeHighlightingValuesLength; i++)
    {
      rangeHighlightingColors[i] = m_rangeHighlightingColors[i / 2];
    }
    return rangeHighlightingColors;
  }

  private void createVariableSizedMesh(float startingYScale, ArrayList<Float> coords, ArrayList<Short> indices, ArrayList<Float> colors)
  {
    float[] coordArray = new float[coords.size()];
    for (int i = 0; i < coordArray.length; i++)
    {
      coordArray[i] = (coords.get(i) * 2.0f) - 1.0f;
    }
    short[] indexArray = new short[indices.size()];
    for (int i = 0; i < indexArray.length; i++)
    {
      indexArray[i] = indices.get(i);
    }
    float[] colorArray = new float[colors.size()];
    for (int i = 0; i < colorArray.length; i++)
    {
      colorArray[i] = colors.get(i);
    }

    replaceMesh(startingYScale, coordArray, indexArray, colorArray);
  }

  private void replaceMesh(float startingYScale, float[] coordArray, short[] indexArray, float[] colorArray)
  {
    MeshRenderable oldDataMesh = m_rangeHighlightMesh;
    m_rangeHighlightMesh = m_graphSurfaceView.addMesh(0, coordArray, indexArray, colorArray);
    m_rangeHighlightMesh.setYScale(startingYScale);
    if (oldDataMesh != null)
    {
      m_graphSurfaceView.removeRenderable(oldDataMesh);
    }
  }

  public void scrollData(float normalisedScrollDelta)
  {
    if (m_allowScroll && m_hasEnoughData)
    {
      long timeDifference = m_endTimestamp - m_startTimestamp;

      long startToFirstDifference = m_startTimestamp - m_firstGraphDataEntry.timestamp;
      float normalisedStartToFirstDifference = startToFirstDifference / (float)timeDifference;
      long endToLastDifference = m_endTimestamp - m_lastGraphDataEntry.timestamp;
      float normalisedEndToLastDifference = endToLastDifference / (float)timeDifference;

      if (dataFits())
      {
        if (normalisedScrollDelta > 0.0f)
        {
          normalisedScrollDelta = Math.min(normalisedScrollDelta, normalisedStartToFirstDifference);
        }
        else
        {
          normalisedScrollDelta = Math.max(normalisedScrollDelta, normalisedEndToLastDifference);
        }
      }
      else
      {
        if (normalisedScrollDelta > 0.0f)
        {
          normalisedScrollDelta = Math.min(normalisedScrollDelta, normalisedEndToLastDifference);
        }
        else
        {
          normalisedScrollDelta = Math.max(normalisedScrollDelta, normalisedStartToFirstDifference);
        }
      }

      long timeChange = (long)(timeDifference * normalisedScrollDelta);
      m_startTimestamp -= timeChange;
      m_endTimestamp -= timeChange;

      normalisedScrollDelta = timeChange / (float)timeDifference; // Take into account rounding errors.
      m_xOffset += normalisedScrollDelta;

      float pixelMove = normalisedScrollDelta * m_graphSurfaceView.getWidth();
      int timeAxisLabelCount = m_timeAxisLabels.size();
      for (int i = 0; i < timeAxisLabelCount; i++)
      {
        TimeAxisLabel label = m_timeAxisLabels.valueAt(i);
        label.view.animate().translationXBy(pixelMove).setDuration(0).start();
        label.offset += pixelMove;
      }

      float openGlXOffset = m_xOffset * 2.0f;
      if (m_dataLineStrip != null)
      {
        m_dataLineStrip.setXOffset(openGlXOffset);
      }
      if (m_rangeHighlightMesh != null && m_rangeHighlightingDisplayMode > DISPLAY_MODE_BACKGROUND_WITH_FADE)
      {
        m_rangeHighlightMesh.setXOffset(openGlXOffset);
      }
      if (m_labelMarkersLine != null)
      {
        m_labelMarkersLine.setXOffset(openGlXOffset);
      }
      m_graphSurfaceView.requestRender();

      if (m_onPeriodChangeListener != null)
      {
        m_onPeriodChangeListener.onPeriodChanged(this, m_startTimestamp, m_endTimestamp);
      }
    }
  }

  public void scaleData(float normalisedScaleDelta, float normalisedXCentre)
  {
    if (m_allowScale && m_hasEnoughData && (dataFits() || normalisedScaleDelta > 0.0f))
    {
      if (m_normalisedForcedXCentre != -1.0f)
      {
        normalisedXCentre = m_normalisedForcedXCentre;
      }

      if (m_beforeScalingStartTimestamp == Long.MIN_VALUE && m_beforeScalingEndTimestamp == Long.MAX_VALUE)
      {
        scrollData(-m_xOffset);
        m_beforeScalingStartTimestamp = m_startTimestamp;
        m_beforeScalingEndTimestamp = m_endTimestamp;
      }

      m_xScale *= 1.0f + normalisedScaleDelta;

      float timingScale = 1.0f / m_xScale;
      m_startTimestamp = (long)scaleValue(
          m_beforeScalingStartTimestamp,
          m_beforeScalingEndTimestamp,
          m_beforeScalingStartTimestamp,
          timingScale,
          normalisedXCentre);
      m_endTimestamp = (long)scaleValue(
          m_beforeScalingStartTimestamp,
          m_beforeScalingEndTimestamp,
          m_beforeScalingEndTimestamp,
          timingScale,
          normalisedXCentre);

      long startToFirstDifference = m_firstGraphDataEntry.timestamp - m_startTimestamp;
      long endToLastDifference = m_endTimestamp - m_lastGraphDataEntry.timestamp;
      if (startToFirstDifference > 0 && endToLastDifference <= 0)
      {
        m_startTimestamp += startToFirstDifference;
        m_endTimestamp += startToFirstDifference;
        refresh(false);
        m_normalisedForcedXCentre = 0.0f;
      }
      else if (endToLastDifference > 0 && startToFirstDifference <= 0)
      {
        m_startTimestamp -= endToLastDifference;
        m_endTimestamp -= endToLastDifference;
        refresh(false);
        m_normalisedForcedXCentre = 1.0f;
      }
      else
      {
        int timeAxisLabelCount = m_timeAxisLabels.size();
        float lastOffset = -Float.MAX_VALUE;
        for (int i = 0; i < timeAxisLabelCount; i++)
        {
          TimeAxisLabel label = m_timeAxisLabels.valueAt(i);
          float scaledLabelOffset = (float)scaleValue(0.0,
                                                  m_graphSurfaceView.getWidth(),
                                                  label.offset,
                                                  m_xScale,
                                                  normalisedXCentre);
          label.view.animate().translationX(scaledLabelOffset).setDuration(0).start();

          float width = label.view.getWidth() * TIME_AXIS_LABEL_WIDTH_MODIFIER;
          if (scaledLabelOffset - width < lastOffset)
          {
            label.view.setVisibility(View.INVISIBLE);
          }
          else
          {
            label.view.setVisibility(View.VISIBLE);
            lastOffset = scaledLabelOffset;
          }
        }

        float openGlXScalePosition = (normalisedXCentre * 2.0f) - 1.0f;
        if (m_dataLineStrip != null)
        {
          m_dataLineStrip.setXScale(m_xScale, openGlXScalePosition);
        }
        if (m_rangeHighlightMesh != null && m_rangeHighlightingDisplayMode > DISPLAY_MODE_BACKGROUND_WITH_FADE)
        {
          m_rangeHighlightMesh.setXScale(m_xScale, openGlXScalePosition);
        }
        if (m_labelMarkersLine != null)
        {
          m_labelMarkersLine.setXScale(m_xScale, openGlXScalePosition);
        }
        m_graphSurfaceView.requestRender();

        if (m_onPeriodChangeListener != null)
        {
          m_onPeriodChangeListener.onPeriodChanged(this, m_startTimestamp, m_endTimestamp);
        }
      }
    }
  }

  private boolean dataFits()
  {
    long timeDifference = m_endTimestamp - m_startTimestamp;
    long firstToLastDifference = m_lastGraphDataEntry.timestamp - m_firstGraphDataEntry.timestamp;
    return firstToLastDifference > timeDifference;
  }

  private void applyAttributes(Context context, AttributeSet attrs)
  {
    TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeGraph, 0, 0);
    try
    {
      m_showValueAxis = attributes.getBoolean(R.styleable.TimeGraph_showValueAxis, DEFAULT_SHOW_VALUE_AXIS);
      m_valueAxisTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_valueAxis_textSizeSp, DEFAULT_VALUE_AXIS_TEXT_SIZE_SP);
      m_valueAxisTextColor = attributes.getColor(R.styleable.TimeGraph_valueAxis_textColor, DEFAULT_VALUE_AXIS_TEXT_COLOR);
      m_valueAxisMin = attributes.getFloat(R.styleable.TimeGraph_valueAxis_min, DEFAULT_VALUE_AXIS_MIN);
      m_valueAxisMax = attributes.getFloat(R.styleable.TimeGraph_valueAxis_max, DEFAULT_VALUE_AXIS_MAX);

      m_showTimeAxis = attributes.getBoolean(R.styleable.TimeGraph_showTimeAxis, DEFAULT_SHOW_TIME_AXIS);
      m_timeAxisTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_timeAxis_textSizeSp, DEFAULT_TIME_AXIS_TEXT_SIZE_SP);
      m_timeAxisTextColor = attributes.getColor(R.styleable.TimeGraph_timeAxis_textColor, DEFAULT_TIME_AXIS_TEXT_COLOR);
      m_timeAxisMarkerColor = attributes.getColor(R.styleable.TimeGraph_timeAxis_markerColor, DEFAULT_TIME_AXIS_MARKER_COLOR);

      m_showNoDataText = attributes.getBoolean(R.styleable.TimeGraph_showNoDataText, DEFAULT_SHOW_NO_DATA_TEXT);
      m_noDataText = attributes.getText(R.styleable.TimeGraph_noData_text);
      if (m_noDataText == null)
      {
        m_noDataText = DEFAULT_NO_DATA_TEXT;
      }
      m_noDataTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_noData_textSizeSp, DEFAULT_NO_DATA_TEXT_SIZE_SP);
      m_noDataTextColor = attributes.getColor(R.styleable.TimeGraph_noData_textColor, DEFAULT_NO_DATA_TEXT_COLOR);

      m_showRefreshProgress = attributes.getBoolean(R.styleable.TimeGraph_showRefreshProgress, DEFAULT_SHOW_REFRESH_PROGRESS);
      m_graphBackgroundColor = attributes.getInt(R.styleable.TimeGraph_graphBackgroundColor, DEFAULT_GRAPH_BACKGROUND_COLOR);
      m_dataLineColor = attributes.getInt(R.styleable.TimeGraph_dataLineColor, DEFAULT_DATA_LINE_COLOR);

      m_allowScroll = attributes.getBoolean(R.styleable.TimeGraph_allowScroll, DEFAULT_ALLOW_SCROLL);
      m_allowScale = attributes.getBoolean(R.styleable.TimeGraph_allowScale, DEFAULT_ALLOW_SCALE);

      if (m_valueAxisMin >= m_valueAxisMax)
      {
        throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
      }
    }
    finally
    {
      attributes.recycle();
    }
  }

  private void initialise(Context context)
  {
    m_graphSurfaceView = new GraphSurface(context);
    m_graphSurfaceView.setId(R.id.graph_surface);
    m_graphSurfaceView.initialise(this, m_graphBackgroundColor);
    addView(m_graphSurfaceView);

    m_valueAxisMinView = new TextView(context);
    m_valueAxisMinView.setId(R.id.min_value);
    m_valueAxisMinView.setText(String.valueOf(m_valueAxisMin));
    m_valueAxisMinView.setTextSize(m_valueAxisTextSizeSp);
    m_valueAxisMinView.setTextColor(m_valueAxisTextColor);
    addView(m_valueAxisMinView);

    m_valueAxisMaxView = new TextView(context);
    m_valueAxisMaxView.setId(R.id.max_value);
    m_valueAxisMaxView.setText(String.valueOf(m_valueAxisMax));
    m_valueAxisMaxView.setTextSize(m_valueAxisTextSizeSp);
    m_valueAxisMaxView.setTextColor(m_valueAxisTextColor);
    addView(m_valueAxisMaxView);

    m_timeAxisLabelsLayoutView = new RelativeLayout(context);
    m_timeAxisLabelsLayoutView.setId(R.id.time_labels_layout);
    addView(m_timeAxisLabelsLayoutView);

    m_refreshProgressView = new ProgressBar(context);
    m_refreshProgressView.setId(R.id.refresh_progress);
    addView(m_refreshProgressView);

    m_noDataView = new TextView(context);
    m_noDataView.setId(R.id.no_data);
    m_noDataView.setGravity(Gravity.CENTER);
    m_noDataView.setText(m_noDataText);
    m_noDataView.setTextSize(m_noDataTextSizeSp);
    m_noDataView.setTextColor(m_noDataTextColor);
    addView(m_noDataView);

    Drawable unwrappedTimeAxisLabelBackground = AppCompatResources.getDrawable(getContext(), R.drawable.label_border);
    if (unwrappedTimeAxisLabelBackground != null)
    {
      m_timeAxisLabelsBackground = DrawableCompat.wrap(unwrappedTimeAxisLabelBackground);
      DrawableCompat.setTint(m_timeAxisLabelsBackground, m_timeAxisMarkerColor);
    }

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(context, R.layout.graph_view);
    if (!m_showValueAxis)
    {
      constraintSet.clear(m_graphSurfaceView.getId(), ConstraintSet.START);
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);

    if (!m_showValueAxis)
    {
      m_valueAxisMinView.setVisibility(View.GONE);
      m_valueAxisMaxView.setVisibility(View.GONE);
    }
    if (!m_showTimeAxis)
    {
      m_timeAxisLabelsLayoutView.setVisibility(View.GONE);
    }
    m_refreshProgressView.setVisibility(View.INVISIBLE);
    if (!m_showNoDataText)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        float offset = m_valueAxisMaxView.getHeight() * 0.5f;
        m_valueAxisMaxView.animate().translationYBy(-offset).setDuration(0).start();
        m_valueAxisMinView.animate().translationYBy(offset).setDuration(0).start();

        resizeViewForValueAxisLabels();
      }
    });
  }

  public void setOnDataPointClickedListener(OnDataPointClickedListener listener)
  {
    m_graphSurfaceView.setOnDataPointClickedListener(listener);
  }

  public void setOnPeriodChangedListener(OnPeriodChangeListener listener)
  {
    m_onPeriodChangeListener = listener;
  }

  public void setOnRefreshListener(OnRefreshListener listener)
  {
    m_onRefreshListener = listener;
  }

  private static TextView createTextView(Context context, float textSize, int textColor)
  {
    TextView textView = new TextView(context);
    textView.setId(View.generateViewId());
    textView.setTextSize(textSize);
    textView.setTextColor(textColor);
    return textView;
  }

  private static double scaleValue(double rangeStart, double rangeEnd, double value, double scale, double normalisedScaleFrom)
  {
    double rangeSize = rangeEnd - rangeStart;
    double normalisedValue = (value - rangeStart) / rangeSize;
    double normalisedValueDistance = normalisedScaleFrom - normalisedValue;
    double normalisedNewValueDistance = normalisedValueDistance * scale;
    return rangeStart + (rangeSize * (normalisedScaleFrom - normalisedNewValueDistance));
  }

  private static int dpToPx(Context context, float dp)
  {
    return (int)(dp * context.getResources().getDisplayMetrics().density);
  }

  private static float pxToDp(Context context, float px)
  {
    return px / context.getResources().getDisplayMetrics().density;
  }

  private static int spToPx(Context context, float sp)
  {
    return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
  }

  private static float lerp(float startValue, float endValue, float interpolationFraction)
  {
    return startValue + interpolationFraction * (endValue - startValue);
  }

  private static boolean getRangeIntersection(float startX, float startY, float endX, float endY, float rangeY, PointF point)
  {
    boolean intersected = false;
    if (startY != rangeY && endY != rangeY)
    {
      float a1 = endY - startY;
      float b1 = startX - endX;
      float c1 = a1 * (startX) + b1 * (startY);

      float a2 = 0.0f;
      float b2 = startX - endX;
      float c2 = a2 * (startX) + b2 * (rangeY);

      float determinant = a1 * b2 - a2 * b1;

      if (determinant != 0)
      {
        point.x = (b2 * c1 - b1 * c2) / determinant;
        point.y = (a1 * c2 - a2 * c1) / determinant;
        intersected = (point.x <= Math.max(startX, endX) && point.x >= Math.min(startX, endX) &&
            point.y <= Math.max(startY, endY) && point.y >= Math.min(startY, endY));
      }
    }
    return intersected;
  }

  public interface OnDataPointClickedListener
  {
    void onDataPointClicked(TimeGraph graph, long timestamp, float value);
  }

  public interface OnPeriodChangeListener
  {
    void onPeriodChanged(TimeGraph graph, long startTimestamp, long endTimestamp);
  }

  public interface OnRefreshListener
  {
    void onRefresh(TimeGraph graph, long startTimestamp, long endTimestamp, GraphData[] data);
  }
}
