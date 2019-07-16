package voxtric.com.diabetescontrol.utilities;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecimalDigitsInputFilter implements InputFilter
{
  private final Pattern m_pattern;

  public DecimalDigitsInputFilter(int digitsBeforeDecimalPlace, int digitsAfterDecimalPlace)
  {
    m_pattern = Pattern.compile("^\\d{0," + digitsBeforeDecimalPlace + "}((\\.{0," + digitsAfterDecimalPlace + "})|(\\.\\d{0," + digitsAfterDecimalPlace + "}))$");
  }

  @Override
  public CharSequence filter(CharSequence source, int sourceStart, int sourceEnd, Spanned destination, int destinationStart, int destinationEnd)
  {
    StringBuilder proposedString;
    if (destinationStart != destinationEnd)
    {
      CharSequence destinationSwap = destination.subSequence(destinationStart, destinationEnd);
      CharSequence sourceSwap = source.subSequence(sourceStart, sourceEnd);
      proposedString = new StringBuilder(destination.toString().replace(destinationSwap, sourceSwap));
    }
    else
    {
      proposedString = new StringBuilder(destination);
      proposedString.insert(destinationStart, source.subSequence(sourceStart, sourceEnd));
    }

    Matcher matcher = m_pattern.matcher(proposedString);
    if (!matcher.matches())
    {
      return "";
    }
    return null;
  }
}
