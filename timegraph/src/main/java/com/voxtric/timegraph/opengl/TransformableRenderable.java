package com.voxtric.timegraph.opengl;

import android.graphics.Color;
import android.opengl.GLES20;

import androidx.annotation.ColorInt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

abstract class TransformableRenderable extends Renderable
{
  private static final String VERTEX_SHADER_CODE =
      "uniform float xOffset;" +
      "uniform float xScale;" +
      "uniform float xScalePosition;" +
      "uniform float yScale;" +

      "attribute vec2 vertexPosition;" +

      "void main() {" +
      "  float scaledDifference = (xScalePosition - vertexPosition.x) * (xScale - 1.0);" +
      "  gl_Position = vec4(vertexPosition.x - scaledDifference + xOffset, ((vertexPosition.y + 1.0) * yScale) - 1.0, 0, 1);" +
      "}";
  private static final String FRAGMENT_SHADER_CODE =
      "precision mediump float;" +
      "uniform vec4 color;" +

      "void main() {" +
      "  gl_FragColor = color;" +
      "}";
  private static int s_shaderHandle = -1;

  float m_xOffset = 0.0f;
  float m_xScale = 1.0f;
  float m_xScalePosition = 0.0f;
  float m_yScale = 1.0f;
  final FloatBuffer m_colorBuffer;

  TransformableRenderable(int drawOrder, float[] coords, @ColorInt int color)
  {
    super(drawOrder, coords);

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(COLORS_PER_VERTEX * (Float.SIZE / Byte.SIZE));
    byteBuffer.order(ByteOrder.nativeOrder());

    float[] colors = new float[] {
        Color.red(color) / 255.0f,
        Color.green(color) / 255.0f,
        Color.blue(color) / 255.0f,
        Color.alpha(color) / 255.0f
    };
    m_colorBuffer = byteBuffer.asFloatBuffer();
    m_colorBuffer.put(colors);
    m_colorBuffer.position(0);
  }

  public void setXOffset(float xOffset)
  {
    m_xOffset = xOffset;
  }

  public void setXScale(float xScale, float xScalePosition)
  {
    m_xScale = xScale;
    m_xScalePosition = xScalePosition;
  }

  public void setYScale(float yScale)
  {
    m_yScale = yScale;
  }

  public void setColor(@ColorInt int color)
  {
    float[] colors = new float[] {
        Color.red(color) / 255.0f,
        Color.green(color) / 255.0f,
        Color.blue(color) / 255.0f,
        Color.alpha(color) / 255.0f
    };
    m_colorBuffer.clear();
    m_colorBuffer.put(colors);
    m_colorBuffer.position(0);
  }

  static int getShaderHandle()
  {
    if (s_shaderHandle == -1)
    {
      int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
      int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);
      s_shaderHandle = GLES20.glCreateProgram();
      GLES20.glAttachShader(s_shaderHandle, vertexShaderHandle);
      GLES20.glAttachShader(s_shaderHandle, fragmentShaderHandle);
      GLES20.glLinkProgram(s_shaderHandle);
    }
    GLES20.glUseProgram(s_shaderHandle);
    return s_shaderHandle;
  }

  static void releaseShader()
  {
    if (s_shaderHandle != -1)
    {
      GLES20.glDeleteShader(s_shaderHandle);
    }
    s_shaderHandle = -1;
  }
}
