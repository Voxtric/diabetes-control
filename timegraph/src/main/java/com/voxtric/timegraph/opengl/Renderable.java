package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class Renderable
{
  public static final int COORDS_PER_VERTEX = 2;
  public static final int COLORS_PER_VERTEX = 4;

  private static final String VERTEX_SHADER_CODE =
      "attribute vec2 vertexPosition;" +

      "void main() {" +
      "  gl_Position = vec4(vertexPosition.xy, 0, 1);" +
      "}";
  private static final String FRAGMENT_SHADER_CODE =
      "precision mediump float;" +

      "void main() {" +
      "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
      "}";
  private static int s_shaderHandle = -1;

  private int m_drawOrder;

  private final FloatBuffer m_vertexBuffer;
  private final int m_vertexCount;

  Renderable(int drawOrder, float[] coords)
  {
    m_drawOrder = drawOrder;

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(coords.length * (Float.SIZE / Byte.SIZE));
    byteBuffer.order(ByteOrder.nativeOrder());

    m_vertexBuffer = byteBuffer.asFloatBuffer();
    m_vertexBuffer.put(coords);
    m_vertexBuffer.position(0);

    m_vertexCount = coords.length / COORDS_PER_VERTEX;
  }

  public int getDrawOrder()
  {
    return m_drawOrder;
  }

  FloatBuffer getVertexBuffer()
  {
    return m_vertexBuffer;
  }

  int getVertexCount()
  {
    return m_vertexCount;
  }

  abstract void draw();

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

  static int loadShader(int type, String shaderCode)
  {
    int shaderHandle = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shaderHandle, shaderCode);
    GLES20.glCompileShader(shaderHandle);
    return shaderHandle;
  }
}
