package com.google.common.base;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(NANOSECONDS)
@State(Scope.Benchmark)
@Fork(5)
public class StringUtilJmhBenchmark {
  @Param({"100", "1000"})
  private int length;

  @Param({"ASCII", "CJK"})
  private String type;

  private String input;

  @Setup
  public void setUp() {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      if ("ASCII".equals(type)) {
        sb.append((char) ('a' + (i % 26)));
      } else {
        sb.append((char) (0x4e00 + (i % 1000))); // Chinese ideographs
      }
    }
    input = sb.toString();
  }

  @Benchmark
  public String escapeUnicodeCurrent() {
    return StringUtil.javaScriptEscape(input);
  }

  @Benchmark
  public String escapeUnicodeOriginal() {
    return originalJavaScriptEscape(input, false);
  }

  @Benchmark
  public String escapeAsciiCurrent() {
    return StringUtil.javaScriptEscapeToAscii(input);
  }

  @Benchmark
  public String escapeAsciiOriginal() {
    return originalJavaScriptEscape(input, true);
  }

  private static String originalJavaScriptEscape(CharSequence s, boolean escapeToAscii) {
    StringBuilder sb = new StringBuilder(s.length() * 9 / 8);
    try {
      originalEscapeStringBody(s, escapeToAscii, StringUtil.JsEscapingMode.EMBEDDABLE_JS, sb);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return sb.toString();
  }

  private static void originalEscapeStringBody(
      CharSequence plainText,
      boolean escapeToAscii,
      StringUtil.JsEscapingMode jsEscapingMode,
      Appendable out)
      throws IOException {
    int pos = 0;
    int len = plainText.length();
    for (int codePoint, charCount, i = 0; i < len; i += charCount) {
      codePoint = Character.codePointAt(plainText, i);
      charCount = Character.charCount(codePoint);

      if (!originalShouldEscapeChar(codePoint, escapeToAscii, jsEscapingMode)) {
        continue;
      }

      out.append(plainText, pos, i);
      pos = i + charCount;
      switch (codePoint) {
        case '\b':
          out.append("\\b");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\f':
          out.append("\\f");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '"':
        case '\'':
          if (jsEscapingMode == StringUtil.JsEscapingMode.JSON && codePoint == '\'') {
            out.append((char) codePoint);
            break;
          } else if (jsEscapingMode != StringUtil.JsEscapingMode.EMBEDDABLE_JS) {
            out.append('\\').append((char) codePoint);
            break;
          }
        // fall through
        default:
          if (codePoint >= 0x100 || jsEscapingMode == StringUtil.JsEscapingMode.JSON) {
            appendUnicode(codePoint, out);
          } else {
            appendHex((char) codePoint, out);
          }
          break;
      }
    }
    out.append(plainText, pos, len);
  }

  private static boolean originalShouldEscapeChar(
      int codePoint, boolean escapeToAscii, StringUtil.JsEscapingMode jsEscapingMode) {
    if (escapeToAscii && (codePoint < 0x20 || codePoint > 0x7e)) {
      return true;
    }
    return ORIGINAL_JS_ESCAPE_CHARS.contains(codePoint);
  }

  private static final char[] hexChars = "0123456789abcdef".toCharArray();

  private static void appendHex(char ch, Appendable out) throws IOException {
    out.append("\\x").append(hexChars[(ch >>> 4) & 0xf]).append(hexChars[ch & 0xf]);
  }

  private static void appendUnicode(int codePoint, Appendable out) throws IOException {
    if (Character.isSupplementaryCodePoint(codePoint)) {
      char[] surrogates = Character.toChars(codePoint);
      appendUnicode(surrogates[0], out);
      appendUnicode(surrogates[1], out);
      return;
    }
    out.append("\\u")
        .append(hexChars[(codePoint >>> 12) & 0xf])
        .append(hexChars[(codePoint >>> 8) & 0xf])
        .append(hexChars[(codePoint >>> 4) & 0xf])
        .append(hexChars[codePoint & 0xf]);
  }

  private static final class BoxedCodePointSet {
    final boolean[] fastArray;
    final Set<Integer> elements;

    BoxedCodePointSet(Set<Integer> codePoints) {
      this.elements = codePoints;
      fastArray = new boolean[0x100];
      for (int i = 0; i < fastArray.length; i++) {
        fastArray[i] = elements.contains(i);
      }
    }

    boolean contains(int codePoint) {
      if (codePoint < fastArray.length) {
        return fastArray[codePoint];
      }
      return elements.contains(codePoint);
    }
  }

  private static final BoxedCodePointSet ORIGINAL_JS_ESCAPE_CHARS;

  static {
    Set<Integer> set = new HashSet<>();
    set.add(0xAD);
    for (int i = 0x600; i <= 0x603; i++) set.add(i);
    set.add(0x6DD);
    set.add(0x070F);
    for (int i = 0x17B4; i <= 0x17B5; i++) set.add(i);
    for (int i = 0x200B; i <= 0x200F; i++) set.add(i);
    for (int i = 0x202A; i <= 0x202E; i++) set.add(i);
    for (int i = 0x2028; i <= 0x2029; i++) set.add(i);
    for (int i = 0x2060; i <= 0x2064; i++) set.add(i);
    for (int i = 0x206A; i <= 0x206F; i++) set.add(i);
    set.add(0xFEFF);
    for (int i = 0xFFF9; i <= 0xFFFB; i++) set.add(i);
    for (int i = 0x1D173; i <= 0x1D17A; i++) set.add(i);
    set.add(0xE0001);
    for (int i = 0xE0020; i <= 0xE007F; i++) set.add(i);
    set.add(0x0000);
    set.add(0x000A);
    set.add(0x000D);
    set.add(0x0085);
    set.add((int) '\'');
    set.add((int) '\"');
    set.add((int) '&');
    set.add((int) '<');
    set.add((int) '>');
    set.add((int) '=');
    set.add((int) '\\');
    ORIGINAL_JS_ESCAPE_CHARS = new BoxedCodePointSet(set);
  }
}
