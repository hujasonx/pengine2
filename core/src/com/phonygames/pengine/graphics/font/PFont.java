package com.phonygames.pengine.graphics.font;

import com.badlogic.gdx.files.FileHandle;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.PStringUtils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PFont {
  public static final String FILE_EXTENSION = "pfont";
  public static final int MAX_GLYPHS = 1024;
  private static final String ASCENT = "ascent";
  private static final String CAP_HEIGHT = "capHeight";
  private static final String DESCENT = "descent";
  private static final String FONT_NAME = "fontName";
  private static final String FONT_SIZE = "fontSize";
  private static final String LINE_HEIGHT = "lineHeight";
  private static final String MEDIAN = "median";
  private static final String SPACE_XADVANCE = "spaceWidth";
  protected String fontName;
  protected int fontSize;
  protected float spaceXAdvance, capHeight, ascent, descent, lineHeight, median;
  private GlyphData[] glyphData = new GlyphData[MAX_GLYPHS];

  public GlyphData glyphData(int c) {
    return glyphData[c];
  }

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
        case GlyphData.GLYPH_DATA:
          PAssert.isNotNull(font, "Font was not created yet!");
          GlyphData glyphData = GlyphData.fromString(line);
          font.glyphData[glyphData.c] = glyphData;
          break;
      }
    }
    return font;
  }

  public PFont addGlyphData(GlyphData glyphData) {
    this.glyphData[glyphData.c] = glyphData;
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
    /** The x offset amount for the original scale (fontsize). */ private int xOffset;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The y offset amount for the original scale (fontsize). */ private int yOffset;

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
      return s.toString();
    }
  }
}
