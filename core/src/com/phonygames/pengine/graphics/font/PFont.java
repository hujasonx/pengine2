package com.phonygames.pengine.graphics.font;

import com.badlogic.gdx.files.FileHandle;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.PStringUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PFont {
  public static final String FILE_EXTENSION = "pfont";
  public static final int MAX_GLYPHS = 512;
  private static final String ASCENT = "ascent";
  private static final String CAP_HEIGHT = "capHeight";
  private static final String DESCENT = "descent";
  private static final String FONT_NAME = "fontName";
  private static final String FONT_SIZE = "fontSize";
  private static final String LINE_HEIGHT = "lineHeight";
  private static final String MEDIAN = "median";
  private static final String SPACE_XADVANCE = "spaceWidth";
  private static final String KERNING = "kerning";
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected String fontName;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected int fontSize;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected float spaceXAdvance, capHeight, ascent, descent, lineHeight, median;
  private GlyphData[] glyphData = new GlyphData[MAX_GLYPHS];
  private int[][] kernings = new int[MAX_GLYPHS][];

  public GlyphData glyphData(int c) {
    return glyphData[c];
  }
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PSDFSheet sdfSheet;

  protected PFont() {
  }

  public static PFont fromFileHandle(FileHandle fileHandle) {
    PAssert.isTrue(fileHandle.extension().equals(FILE_EXTENSION));
    String[] r = PStringUtils.splitByLine(fileHandle.readString());
    PFont font = new PFont();
    String fontName = null;
    int fontSize = 0;
    for (int a = 0; a < r.length; a++) {
      final String line = r[a];
      String[] split = line.split("\\|");
      if (split.length == 0 || line.startsWith("#")) {
        continue;
      }
      String type = split[0];
      switch (type) {
        case ASCENT:
          font.ascent = Float.parseFloat(split[1]);
          break;
        case CAP_HEIGHT:
          font.capHeight = Float.parseFloat(split[1]);
          break;
        case DESCENT:
          font.descent = Float.parseFloat(split[1]);
          break;
        case FONT_NAME:
          font.fontName = split[1];
          break;
        case FONT_SIZE:
          font.fontSize = Integer.parseInt(split[1]);
          break;
        case LINE_HEIGHT:
          font.lineHeight = Float.parseFloat(split[1]);
          break;
        case MEDIAN:
          font.median = Float.parseFloat(split[1]);
          break;
        case SPACE_XADVANCE:
          font.spaceXAdvance = Float.parseFloat(split[1]);
          break;
        case KERNING:
          int startCharForKerning = Integer.parseInt(split[1]);
          int endCharForKerning = Integer.parseInt(split[2]);
          int kerningAmount = Integer.parseInt(split[3]);
          font.addKerning(startCharForKerning,endCharForKerning,kerningAmount);
          break;
        case GlyphData.GLYPH_DATA:
          PAssert.isNotNull(font, "Font was not created yet!");
          GlyphData glyphData = GlyphData.fromString(line);
          glyphData.font = font;
          font.glyphData[glyphData.c] = glyphData;
          break;
      }
    }
    return font;
  }

  public int getKerning(int startChar, int endChar) {
    if (kernings[startChar] == null) {
      return 0;
    }
    return kernings[startChar][endChar];
  }

  public PFont addKerning(int startChar, int endChar, int kerningAmount) {
    if (kernings[startChar] == null) {
      kernings[startChar] = new int[MAX_GLYPHS];
    }
    kernings[startChar][endChar] = kerningAmount;
    return this;
  }

  public PFont addGlyphData(GlyphData glyphData) {
    this.glyphData[glyphData.c] = glyphData;
    glyphData.font = this;
    return this;
  }

  public boolean writeFileHandle(FileHandle fileHandle) {
    PString outData = PString.obtain();
    outData.append("#PFont").appendBr();
    outData.append(FONT_NAME).append("|").append(fontName).appendBr();
    outData.append(FONT_SIZE).append("|").append(fontSize).appendBr();
    outData.append(SPACE_XADVANCE).append("|").append(spaceXAdvance).appendBr();
    outData.append(ASCENT).append("|").append(ascent).appendBr();
    outData.append(CAP_HEIGHT).append("|").append(capHeight).appendBr();
    outData.append(DESCENT).append("|").append(descent).appendBr();
    outData.append(LINE_HEIGHT).append("|").append(lineHeight).appendBr();
    outData.append(MEDIAN).append("|").append(median).appendBr();
    for (int a = 0; a < MAX_GLYPHS; a++) {
      GlyphData glyphData = this.glyphData[a];
      if (glyphData == null) {continue;}
      outData.append(glyphData.toString()).appendBr();
      if (kernings[a] != null) {
        for (int b = 0; b < MAX_GLYPHS; b++) {
          if (kernings[a][b] != 0) {
            outData.append(KERNING).append("|").append(a).append("|").append(b).append("|").append(kernings[a][b]).appendBr();
          }

        }
      }
    }
    fileHandle.writeString(outData.toString(), false);
    outData.free();
    return true;
  }

  @Builder public static class GlyphData {
    private static final String GLYPH_DATA = "GlyphData";
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int c;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private String fontName;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int fontSize;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private String sdfKey;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The x advance amount for the original scale (fontsize). */ private int xAdvance;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The width for the original scale (fontsize). */ private int width;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The height for the original scale (fontsize). */ private int height;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The x offset amount for the original scale (fontsize). */ private int xOffset;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The y offset amount for the original scale (fontsize). */ private int yOffset;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PFont font;
    //      final int cap = drawY;
    //      final int baseline = cap - (int) font.getCapHeight();
    //      Derived -> final int drawY = baseline + (int) font.getCapHeight();
    //      final int glyphLeft = drawX;
    //      final int glyphRight = glyphLeft + glyph.width;
    //      final int glyphBottom = cap + glyph.yoffset + (int) font.getAscent();
    //      Derived -> final int glyphBottom = baseline + (int) font.getCapHeight() + glyph.yoffset + (int) font.getAscent();
    //      final int glyphTop = glyphBottom + glyph.height;
    //      Derived -> final int glyphTop = baseline + (int) font.getCapHeight() + glyph.yoffset + (int) font.getAscent() + glyph.height;
    //      final int glyphTop = glyphBottom + glyph.height;
    //      final int descent = baseline + (int) font.getDescent();
    //      final int advancedX = glyphLeft + glyph.xadvance;
    /** The distance from the top of the glyph to the baseline in the original scale (fontsize). Positive means the top extends up from the baseline.*/
    public int baselineToTop() {
      return (int)font.capHeight + yOffset + (int)font.ascent + height;
    }

    /** The distance from the baseline to the bottom in the original scale (fontsize). Negative means the bottom extends below the baseline. */
    public int baselineToBottom() {
      return (int)font.capHeight + yOffset + (int)font.ascent;
    }

    public static GlyphData fromString(String s) {
      String[] split = s.split("\\|");
      GlyphData.GlyphDataBuilder builder = GlyphData.builder();
      builder.c(Integer.parseInt(split[1]));
      builder.fontName(split[2]);
      builder.fontSize(Integer.parseInt(split[3]));
      builder.sdfKey(split[4]);
      builder.xAdvance(Integer.parseInt(split[5]));
      builder.xOffset(Integer.parseInt(split[6]));
      builder.yOffset(Integer.parseInt(split[7]));
      builder.width(Integer.parseInt(split[8]));
      builder.height(Integer.parseInt(split[9]));
      return builder.build();
    }

    public String toString() {
      PString s = PString.obtain();
      s.append(GLYPH_DATA).append("|");
      s.append(c).append("|");
      s.append(fontName).append("|");
      s.append(fontSize).append("|");
      s.append(sdfKey).append("|");
      s.append(xAdvance).append("|");
      s.append(xOffset).append("|");
      s.append(yOffset).append("|");
      s.append(width).append("|");
      s.append(height).append("|");
      return s.toString();
    }
  }
}
