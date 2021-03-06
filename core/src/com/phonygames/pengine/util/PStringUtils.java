package com.phonygames.pengine.util;

import com.phonygames.pengine.math.PNumberUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PStringUtils {
  private static final PStringMap<PStringMap<String>> staticConcatMap = new PStringMap<PStringMap<String>>() {
    @Override public PStringMap<String> newUnpooled(String key) {
      return new PStringMap<String>();
    }
  };

  public static String concat(String a, String b) {
    PStringMap<String> m = staticConcatMap.genUnpooled(a);
    if (m.has(b)) { return m.get(b); }
    String ret = a + b;
    m.put(b, ret);
    return ret;
  }

  public static String appendSpacesToLength(String s, int length) {
    if (s == null) {
      return null;
    }
    if (s.length() >= length) {
      return s;
    }
    StringBuilder builder = new StringBuilder();
    builder.append(s);
    for (int a = 0; a < length - s.length(); a++) {
      builder.append(' ');
    }
    return builder.toString();
  }

  public static String[] extractStringArray(String s, String delimLeft, String delimRight, String separator,
                                            boolean trim) {
    String extractedPart = extract(s, delimLeft, delimRight);
    if (extractedPart == null) {
      return null;
    }
    String[] ret = extractedPart.split(separator);
    if (trim) {
      for (int a = 0; a < ret.length; a++) {
        ret[a] = ret[a].trim();
      }
    }
    return ret;
  }

  public static String extract(String s, String delimL, String delimR) {
    return extract(s, delimL, delimR, false);
  }

  public static String extract(String s, String delimL, String delimR, boolean delimRSoftRequirement) {
    int indexAfterDelimL = indexAfter(s, delimL);
    if (indexAfterDelimL == -1) {
      return null;
    }
    int indexOfDelimR = s.indexOf(delimR, indexAfterDelimL);
    if (indexOfDelimR == -1) {
      if (delimRSoftRequirement) {
        return s.substring(indexAfterDelimL);
      }
      return null;
    }
    return s.substring(indexAfterDelimL, indexOfDelimR);
  }

  /**
   * Returns the index of the next character after a substring, or -1.
   * @param in the larger string to search in
   * @param of the substring
   * @return the index, or -1
   */
  public static int indexAfter(String in, String of) {
    int i = in.indexOf(of);
    if (i == -1) {
      return -1;
    }
    i = i + of.length();
    return i;
  }

  public static String getLineSpacePrefix(String line) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int a = 0; a < line.length(); a++) {
      char c = line.charAt(a);
      if (c == ' ') {
        stringBuilder.append(' ');
      } else if (c == '\t') {
        stringBuilder.append('\t');
      } else {
        break;
      }
    }
    return stringBuilder.toString();
  }

  public static String partAfter(String s, String delim) {
    int indexAfterDelim = indexAfter(s, delim);
    if (indexAfterDelim == -1) {
      return null;
    }
    return s.substring(indexAfterDelim);
  }

  public static String prependSpacesToLength(String s, int length) {
    if (s == null) {
      return null;
    }
    if (s.length() >= length) {
      return s;
    }
    StringBuilder builder = new StringBuilder();
    for (int a = 0; a < length - s.length(); a++) {
      builder.append(' ');
    }
    builder.append(s);
    return builder.toString();
  }

  public static String roundNearestHundredth(float n) {
    double nd = Math.round(100.0 * n);
    String s = ("" + (nd * .01));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 3));
    }
    return s;
  }

  public static String roundNearestTenth(float n) {
    double nd = Math.round(10.0 * n);
    String s = ("" + (nd * .1));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 2));
    }
    return s;
  }

  public static String roundNearestThousandth(float n) {
    double nd = Math.round(1000.0 * n);
    String s = ("" + (nd * .001));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 4));
    }
    return s;
  }

  public static String roundNum(float n, int digits) {
    String s = ("" + n);
    if (s.contains("E")) {
      String[] split = s.split("E");
      int exDigits = split[1].length();
      int numDigits = PNumberUtils.clamp(digits - 1 - exDigits, 0, split[0].length());
      if (numDigits == 0) {
        return "";
      }
      if (split[0].charAt(numDigits - 1) == '.') {
        return split[0].substring(0, numDigits - 1) + "E" + split[1];
      }
      return split[0].substring(0, numDigits) + "E" + split[1];
    }
    s = s.substring(0, Math.min(s.length(), digits));
    if (s.charAt(s.length() - 1) == '.') {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  public static String[] splitByLine(String s) {
    return s.split("\\r?\\n");
  }
}
