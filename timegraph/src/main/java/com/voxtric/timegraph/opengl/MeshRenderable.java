package com.voxtric.timegraph.opengl;

import android.graphics.Color;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class MeshRenderable extends TransformableRenderable
{
  private static final String VERTEX_SHADER_CODE =
      "uniform float xOffset;" +
      "uniform float xScale;" +
      "uniform float xScalePosition;" +
      "uniform float yScale;" +

      "attribute vec2 vertexPosition;" +
      "attribute vec4 vertexColor;" +

      "varying vec4 fragmentColor;" +

      "void main() {" +
      "  float scaledDifference = (xScalePosition - vertexPosition.x) * (xScale - 1.0);" +
      "  gl_Position = vec4(vertexPosition.x - scaledDifference + xOffset, ((vertexPosition.y + 1.0) * yScale) - 1.0, 0, 1);" +
      "  fragmentColor = vertexColor;" +
      "}";
  private static final String FRAGMENT_SHADER_CODE =
      "precision mediump float;" +

      "varying vec4 fragmentColor;" +

      "void main() {" +
      "  gl_FragColor = fragmentColor;" +
      "}";
  private static int s_shaderHandle = -1;

  private final ShortBuffer m_indexBuffer;
  private final int m_indexCount;
  private final FloatBuffer m_colorBuffer;

  MeshRenderable(int drawOrder, float[] coords, short[] indices, float[] colors)
  {
    super(drawOrder, coords, Color.TRANSPARENT);

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(indices.length * (Short.SIZE / Byte.SIZE));
    byteBuffer.order(ByteOrder.nativeOrder());
    m_indexBuffer = byteBuffer.asShortBuffer();
    m_indexBuffer.put(indices);
    m_indexBuffer.position(0);
    m_indexCount = indices.length;

    byteBuffer = ByteBuffer.allocateDirect(colors.length * (Float.SIZE / Byte.SIZE));
    byteBuffer.order(ByteOrder.nativeOrder());
    m_colorBuffer = byteBuffer.asFloatBuffer();
    m_colorBuffer.put(colors);
    m_colorBuffer.position(0);
  }

  @Override
  void draw()
  {
    int shaderHandle = MeshRenderable.getShaderHandle();

    int vertexPositionHandle = GLES20.glGetAttribLocation(shaderHandle, "vertexPosition");
    GLES20.glEnableVertexAttribArray(vertexPositionHandle);
    GLES20.glVertexAttribPointer(vertexPositionHandle, COORDS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 COORDS_PER_VERTEX * (Float.SIZE / Byte.SIZE), getVertexBuffer());

    int vertexColorHandle = GLES20.glGetAttribLocation(shaderHandle, "vertexColor");
    GLES20.glEnableVertexAttribArray(vertexColorHandle);
    GLES20.glVertexAttribPointer(vertexColorHandle, COLORS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 COLORS_PER_VERTEX * (Float.SIZE / Byte.SIZE), m_colorBuffer);

    int xOffsetHandle = GLES20.glGetUniformLocation(shaderHandle, "xOffset");
    GLES20.glUniform1f(xOffsetHandle, m_xOffset);

    int xScaleHandle = GLES20.glGetUniformLocation(shaderHandle, "xScale");
    GLES20.glUniform1f(xScaleHandle, m_xScale);

    int xScalePositionHandle = GLES20.glGetUniformLocation(shaderHandle, "xScalePosition");
    GLES20.glUniform1f(xScalePositionHandle, m_xScalePosition);

    int yScaleHandle = GLES20.glGetUniformLocation(shaderHandle, "yScale");
    GLES20.glUniform1f(yScaleHandle, m_yScale);

    GLES20.glDrawElements(GLES20.GL_TRIANGLES, m_indexCount, GLES20.GL_UNSIGNED_SHORT, m_indexBuffer);

    GLES20.glDisableVertexAttribArray(vertexPositionHandle);
    GLES20.glDisableVertexAttribArray(vertexColorHandle);
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
