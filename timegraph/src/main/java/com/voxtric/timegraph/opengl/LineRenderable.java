package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

import androidx.annotation.ColorInt;

public class LineRenderable extends TransformableRenderable
{
  LineRenderable(int drawOrder, float[] coords, @ColorInt int color)
  {
    super(drawOrder, coords, color);
  }

  @Override
  public void draw()
  {
    int shaderHandle = TransformableRenderable.getShaderHandle();

    int vertexPositionHandle = GLES20.glGetAttribLocation(shaderHandle, "vertexPosition");
    GLES20.glEnableVertexAttribArray(vertexPositionHandle);
    GLES20.glVertexAttribPointer(vertexPositionHandle, COORDS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 COORDS_PER_VERTEX * (Float.SIZE / Byte.SIZE) , getVertexBuffer());

    int xOffsetHandle = GLES20.glGetUniformLocation(shaderHandle, "xOffset");
    GLES20.glUniform1f(xOffsetHandle, m_xOffset);

    int xScaleHandle = GLES20.glGetUniformLocation(shaderHandle, "xScale");
    GLES20.glUniform1f(xScaleHandle, m_xScale);

    int xScalePositionHandle = GLES20.glGetUniformLocation(shaderHandle, "xScalePosition");
    GLES20.glUniform1f(xScalePositionHandle, m_xScalePosition);

    int yScaleHandle = GLES20.glGetUniformLocation(shaderHandle, "yScale");
    GLES20.glUniform1f(yScaleHandle, m_yScale);

    int colorHandle = GLES20.glGetUniformLocation(shaderHandle, "color");
    GLES20.glUniform4fv(colorHandle, 1, m_colorBuffer);

    GLES20.glDrawArrays(GLES20.GL_LINES, 0, getVertexCount());

    GLES20.glDisableVertexAttribArray(vertexPositionHandle);
  }
}
