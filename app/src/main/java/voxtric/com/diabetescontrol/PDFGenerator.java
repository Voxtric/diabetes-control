package voxtric.com.diabetescontrol;

import android.app.Activity;
import android.support.v4.app.FragmentManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class PDFGenerator
{
  protected static class PDFColor
  {
    int r;
    int g;
    int b;

    PDFColor(int red, int green, int blue)
    {
      r = red;
      g = green;
      b = blue;
    }
  }

  public abstract String getFileName();
  public abstract ByteArrayOutputStream createPDF(Activity activity);

  static final float VERTICAL_SPACE = 15.0f;
  static final float BORDER = 30.0f;
  static final float FONT_SIZE_LARGE = 14.0f;
  static final float FONT_SIZE_MEDIUM = 11.0f;
  static final float FONT_SIZE_SMALL = 7.0f;
  static final float LINE_SPACING = 3.0f;
  static final PDFont FONT = PDType1Font.HELVETICA;
  static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
  static final PDFColor BLACK = new PDFColor(0, 0, 0);

  private final PDDocument m_document;
  private PDPageContentStream m_content = null;
  float m_pageWidth = 0.0f;

  PDFGenerator()
  {
    m_document = new PDDocument();
  }

  void addPage() throws IOException
  {
    if (m_content != null)
    {
      m_content.close();
    }

    PDPage page = new PDPage(PDRectangle.A4);
    m_document.addPage(page);
    m_content = new PDPageContentStream(m_document, page);
    m_pageWidth = page.getMediaBox().getWidth() - (BORDER * 2.0f);
  }

  ByteArrayOutputStream getOutputStream() throws IOException
  {
    if (m_content != null)
    {
      m_content.close();
    }
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    m_document.save(outputStream);
    m_document.close();
    return outputStream;
  }

  float drawText(PDFont font, float fontSize, String text, float textStart, float height) throws IOException
  {
    height -= fontSize;
    m_content.beginText();
    m_content.setFont(font, fontSize);
    m_content.transform(Matrix.getTranslateInstance(textStart, height));
    m_content.showText(text);
    m_content.transform(Matrix.getTranslateInstance(-textStart, -height));
    m_content.endText();
    return height;
  }

  float drawTextCenterAligned(PDFont font, float fontSize, String text, float centeredAt, float height) throws IOException
  {
    text = text.replace('\n', ' ');
    height -= fontSize;
    m_content.beginText();
    m_content.setFont(font, fontSize);
    float halfStringLength = font.getStringWidth(text) / 2000.0f * fontSize;
    float position = centeredAt - halfStringLength;
    m_content.transform(Matrix.getTranslateInstance(position, height));
    m_content.showText(text);
    m_content.transform(Matrix.getTranslateInstance(-position, -height));
    m_content.endText();
    return height;
  }

  float drawTextCentered(PDFont font, float fontSize, String text, float rotation, float centeredAt, float height) throws IOException
  {
    text = text.replace('\n', ' ');
    m_content.beginText();
    m_content.setFont(font, fontSize);
    float halfStringLength = font.getStringWidth(text) / 2000.0f * fontSize;
    m_content.transform(Matrix.getTranslateInstance(centeredAt, height));
    m_content.transform(Matrix.getRotateInstance(Math.toRadians(rotation), 0.0f, 0.0f));
    m_content.transform(Matrix.getTranslateInstance(-halfStringLength, -fontSize / 2.0f));
    m_content.showText(text);
    m_content.transform(Matrix.getTranslateInstance(halfStringLength, fontSize / 2.0f));
    m_content.transform(Matrix.getRotateInstance(-Math.toRadians(rotation), 0.0f, 0.0f));
    m_content.transform(Matrix.getTranslateInstance(-centeredAt, -height));
    m_content.endText();
    return height - (halfStringLength * 2.0f);
  }

  float drawTextParagraphed(PDFont font, float fontSize, String text, float startAt, float endAt, float height, float minHeight) throws IOException
  {
    StringBuilder token = new StringBuilder();
    StringBuilder string = new StringBuilder();
    for (int i = 0; i < text.length(); ++i)
    {
      char character = text.charAt(i);
      if (character == ' ' || character == '\n')
      {
        float stringLength = font.getStringWidth(string.toString() + token) / 1000.0f * fontSize;
        if (stringLength > endAt - startAt)
        {
          height = drawText(font, fontSize, string.toString(), startAt, height);
          string = new StringBuilder(token + " ");
          token = new StringBuilder();
          if (height - fontSize <= minHeight + fontSize)
          {
            token.append("...");
            break;
          }
        }
        else
        {
          string.append(token).append(" ");
          token = new StringBuilder();
          if (character == '\n')
          {
            height = drawText(font, fontSize, string.toString(), startAt, height);
            string = new StringBuilder();
            if (height - fontSize <= minHeight + fontSize)
            {
              token = new StringBuilder("...");
              break;
            }
          }
        }
      }
      else
      {
        token.append(character);
      }
    }
    float stringLength = font.getStringWidth(string.toString() + token) / 1000.0f * fontSize;
    if (stringLength > endAt - startAt)
    {
      height = drawText(font, fontSize, string.toString(), startAt, height);
      string = new StringBuilder(token.toString());
    }
    else
    {
      string.append(token);
    }
    height = drawText(font, fontSize, string.toString(), startAt, height);
    return height;
  }

  float drawCenteredTextParagraphed(PDFont font, float fontSize, String text, float rotation, float centeredAt, float height, float width) throws IOException
  {
    StringBuilder token = new StringBuilder();
    StringBuilder string = new StringBuilder();
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < text.length(); ++i)
    {
      char character = text.charAt(i);
      if (character == ' ' || character == '\n')
      {
        float stringLength = font.getStringWidth(string.toString() + token) / 1000.0f * fontSize;
        if (stringLength > width)
        {
          lines.add(string.toString().trim());
          string = new StringBuilder(token + " ");
          token = new StringBuilder();
        }
        else
        {
          string.append(token).append(" ");
          token = new StringBuilder();
          if (character == '\n')
          {
            lines.add(string.toString().trim());
            string = new StringBuilder();
          }
        }
      }
      else
      {
        token.append(character);
      }
    }
    float stringLength = font.getStringWidth(string.toString() + token) / 1000.0f * fontSize;
    if (stringLength > width)
    {
      lines.add(string.toString().trim());
      lines.add(token.toString().trim());
    }
    else
    {
      lines.add((string.toString() + token).trim());
    }

    //float halfHeight = (lines.size() * fontSize) / 2.0f;
    float heightOffset = 0.0f;
    for (int i = 0; i < lines.size(); i++)
    {
      float halfWidth = font.getStringWidth(lines.get(i)) / 2000.0f * fontSize;
      m_content.beginText();
      m_content.setFont(font, fontSize);
      m_content.transform(Matrix.getTranslateInstance(centeredAt, height));
      m_content.transform(Matrix.getRotateInstance(Math.toRadians(rotation), 0.0f, 0.0f));
      //m_content.transform(Matrix.getTranslateInstance(-halfWidth, halfHeight - heightOffset));
      m_content.transform(Matrix.getTranslateInstance(-halfWidth, -heightOffset));
      m_content.showText(lines.get(i));
      //m_content.transform(Matrix.getTranslateInstance(halfWidth, -(halfHeight - heightOffset)));
      m_content.transform(Matrix.getTranslateInstance(halfWidth, heightOffset));
      m_content.transform(Matrix.getRotateInstance(-Math.toRadians(rotation), 0.0f, 0.0f));
      m_content.transform(Matrix.getTranslateInstance(-centeredAt, -height));
      m_content.endText();
      heightOffset += fontSize;
    }

    return height - (width / 2.0f);
  }

  void drawBox(float startX, float startY, float endX, float endY, PDFColor stroke, PDFColor fill) throws IOException
  {
    if (fill != null)
    {
      m_content.addRect(startX, startY, endX - startX, endY - startY);
      m_content.setNonStrokingColor(fill.r, fill.g, fill.b);
      m_content.fill();
      m_content.setNonStrokingColor(0, 0, 0);
    }
    m_content.addRect(startX, startY, endX - startX, endY - startY);
    m_content.setStrokingColor(stroke.r, stroke.g, stroke.b);
    m_content.stroke();
    m_content.setStrokingColor(0, 0, 0);
  }
}
