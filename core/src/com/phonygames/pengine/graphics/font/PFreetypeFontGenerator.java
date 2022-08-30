package com.phonygames.pengine.graphics.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.font.PFont;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.sdf.PSDFGenerator;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PFreetypeFontGenerator {
  public static final String charsToGen =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
  private final static int GLYPH_OFFSET_X = 200;
  private final static int GLYPH_OFFSET_Y = 200;
  private final static int GLYPH_TEX_SIZE = 1024;
  private static GlyphLayout layout = new GlyphLayout();
  final private BitmapFont font;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final FileHandle fontFile;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final String fontName;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final int fontSize;
  private final PStringMap<PFont.GlyphData> glyphDataMap = new PStringMap<>();
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();
  private final SpriteBatch spriteBatch = new SpriteBatch();
  private PRenderBuffer renderBuffer;
  private float[] spritebatchLineFloats = new float[20];
  private PTexture texture = new PTexture();
  private final PFont outputFont;

  public PFreetypeFontGenerator(String fontName, FileHandle fontFile, int fontSize) {
    this.fontName = fontName;
    this.fontFile = fontFile;
    this.fontSize = fontSize;
    FreeTypeFontGenerator.setMaxTextureSize(GLYPH_TEX_SIZE);
    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
    FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
    parameter.size = fontSize;
    parameter.packer = new PixmapPacker(2048, 2048, Pixmap.Format.RGBA8888, 4, true);
    FreeTypeFontGenerator.FreeTypeBitmapFontData data = generator.generateData(parameter);
    this.font = generator.generateFont(parameter);
    generator.dispose();
    this.renderBuffer =
        new PRenderBuffer.Builder().setStaticSize(GLYPH_TEX_SIZE, GLYPH_TEX_SIZE).addFloatAttachment("color").build();
    orthographicCamera.setToOrtho(false, renderBuffer.width(), renderBuffer.height());
    orthographicCamera.update(true);
    spriteBatch.setProjectionMatrix(orthographicCamera.combined);
    outputFont = new PFont();
    outputFont.fontSize = fontSize;
    outputFont.fontName = fontName;
    outputFont.spaceXAdvance = font.getSpaceXadvance();
    outputFont.capHeight = font.getCapHeight();
    outputFont.ascent = font.getAscent();
    outputFont.descent = font.getDescent();
    outputFont.lineHeight = font.getLineHeight();
    outputFont.median = font.getXHeight();
  }

  public void genAll(PSDFGenerator sdfGenerator, float scale, int sheetPadding) {
    for (int a = 0; a < charsToGen.length(); a++) {
      char c = charsToGen.charAt(a);
      gen(c, sdfGenerator, scale, sheetPadding);
    }
  }

  /**
   * Renders the glyph to the sdf generator.
   */
  public PFont.GlyphData gen(char c, PSDFGenerator sdfGenerator, float scale, int sheetPadding) {
    // Get the original glyph.
    BitmapFont.Glyph glyph = font.getData().getGlyph(c);
    if (glyph == null) {
      return null;
    }
    String sdfKey = fontName + "_" + fontSize + "_" + (int) c;
    PFont.GlyphData.GlyphDataBuilder builder = PFont.GlyphData.builder();
    builder.sdfKey(sdfKey);
    builder.fontSize(this.fontSize);
    builder.fontName(this.fontName);
    builder.c(c);
    builder.xAdvance(glyph.xadvance);
    builder.xOffset(glyph.xoffset);
    builder.yOffset(glyph.yoffset);
    builder.width(glyph.width);
    builder.height(glyph.height);
    // Render the glyph.
    renderBuffer.begin();
    PGLUtils.clearScreen(0, 0, 0, 1);
    layout.setText(font, "" + c);
    font.getCache().clear();
    font.getCache().setColor(Color.WHITE);
    int drawX = GLYPH_OFFSET_X;
    int drawY = GLYPH_TEX_SIZE - GLYPH_OFFSET_Y;
    font.getCache().addText(layout, drawX, drawY);
    spriteBatch.begin();
    final int cap = drawY;
    final int baseline = cap - (int) font.getCapHeight();
    final int glyphLeft = drawX;
    final int glyphRight = glyphLeft + glyph.width;
    final int glyphBottom = cap + glyph.yoffset + (int) font.getAscent();
    final int glyphTop = glyphBottom + glyph.height;
    final int descent = baseline + (int) font.getDescent();
    final int advancedX = glyphLeft + glyph.xadvance;
    // Cap Height.
    spriteBatch.setColor(Color.YELLOW);
    spritebatchLine(spriteBatch, 0, cap, GLYPH_TEX_SIZE, cap, 2);
    // Baseline.
    spriteBatch.setColor(Color.RED);
    spritebatchLine(spriteBatch, 0, baseline, GLYPH_TEX_SIZE, baseline, 2);
    // Descent.
    spriteBatch.setColor(Color.FIREBRICK);
    spritebatchLine(spriteBatch, 0, descent, GLYPH_TEX_SIZE, descent, 2);
    // GlyphLeft.
    spriteBatch.setColor(Color.GRAY);
    spritebatchLine(spriteBatch, glyphLeft, 0, glyphLeft, GLYPH_TEX_SIZE, 2);
    // GlyphBottom.
    spriteBatch.setColor(Color.PINK);
    spritebatchLine(spriteBatch, 0, glyphBottom, GLYPH_TEX_SIZE, glyphBottom, 2);
    // GlyphTop.
    spriteBatch.setColor(Color.BLUE);
    spritebatchLine(spriteBatch, 0, glyphTop, GLYPH_TEX_SIZE, glyphTop, 2);
    // GlyphRight.
    spriteBatch.setColor(Color.GRAY);
    spritebatchLine(spriteBatch, glyphRight, 0, glyphRight, GLYPH_TEX_SIZE, 2);
    // Advanced X.
    spriteBatch.setColor(Color.BROWN);
    spritebatchLine(spriteBatch, advancedX, baseline, advancedX, baseline - 20, 2);
    font.getCache().draw(spriteBatch);
    spriteBatch.end();
    renderBuffer.end();
    texture.set(renderBuffer.texture());
    texture.uvOS().set(((float) glyphLeft) / GLYPH_TEX_SIZE, ((float) glyphBottom) / GLYPH_TEX_SIZE,
                       ((float) glyphRight - glyphLeft) / GLYPH_TEX_SIZE,
                       ((float) glyphTop - glyphBottom) / GLYPH_TEX_SIZE);
    sdfGenerator.begin();
    sdfGenerator.addSymbol(sdfKey, texture, scale, sheetPadding);
    sdfGenerator.end();
    PFont.GlyphData glyphData = builder.build();
    outputFont.addGlyphData(glyphData);
    for (int a = 0; a < 256; a++) {
      int kern = glyph.getKerning((char)a);
      if (kern != 0) {
        outputFont.addKerning(c, (char)a,kern);
      }
    }
    glyphDataMap.put((int) c + "", glyphData);
    return glyphData;
  }

  public void emitToFile() {
    outputFont.writeFileHandle(Gdx.files.local("engine/font/"+fontName + "."+PFont.FILE_EXTENSION));
  }

  private void spritebatchLine(SpriteBatch spriteBatch, float x0, float y0, float x1, float y1, float lineWidth) {
    float xdif = x1 - x0;
    float ydif = y1 - y0;
    float l2 = xdif * xdif + ydif * ydif;
    float invl = (float) (lineWidth / Math.sqrt(l2)) * .5f;
    xdif *= invl;
    ydif *= invl;
    int index = 0;
    float c = spriteBatch.getColor().toFloatBits();
    spritebatchLineFloats[index++] = x0 + ydif;
    spritebatchLineFloats[index++] = y0 - xdif;
    spritebatchLineFloats[index++] = c;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = x0 - ydif;
    spritebatchLineFloats[index++] = y0 + xdif;
    spritebatchLineFloats[index++] = c;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = x1 - ydif;
    spritebatchLineFloats[index++] = y1 + xdif;
    spritebatchLineFloats[index++] = c;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = x1 + ydif;
    spritebatchLineFloats[index++] = y1 - xdif;
    spritebatchLineFloats[index++] = c;
    spritebatchLineFloats[index++] = 0;
    spritebatchLineFloats[index++] = 0;
    spriteBatch.draw(PTexture.WHITE_PIXEL().getBackingTexture(), spritebatchLineFloats, 0,
                     spritebatchLineFloats.length);
  }

  public Texture previewRenderBufferTexture() {
    return renderBuffer.texture();
  }
}
