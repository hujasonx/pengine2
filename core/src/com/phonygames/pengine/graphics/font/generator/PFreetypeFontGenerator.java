package com.phonygames.pengine.graphics.font.generator;

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
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.sdf.PSDFGenerator;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PFreetypeFontGenerator {
  private final static int GLYPH_TEX_SIZE = 1024;
  private final static int GLYPH_OFFSET_X = 200;
  private final static int GLYPH_OFFSET_Y = 200;
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
  private final PStringMap<GlyphData> glyphDataMap = new PStringMap<>();
  private PRenderBuffer renderBuffer;
  private PTexture texture = new PTexture();
  private final SpriteBatch spriteBatch = new SpriteBatch();
  private final OrthographicCamera orthographicCamera = new OrthographicCamera();

  public Texture previewRenderBufferTexture() {
    return renderBuffer.texture();
  }

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

    this.renderBuffer = new PRenderBuffer.Builder().setStaticSize(GLYPH_TEX_SIZE, GLYPH_TEX_SIZE).addFloatAttachment("color").build();
    orthographicCamera.setToOrtho(false, renderBuffer.width(), renderBuffer.height());
    orthographicCamera.update(true);
    spriteBatch.setProjectionMatrix(orthographicCamera.combined);
  }

  /**
   * Renders the glyph to the sdf generator.
   */
  public GlyphData gen(char c, PSDFGenerator sdfGenerator, float scale, int sheetPadding) {
    // Get the original glyph.
    BitmapFont.Glyph glyph = font.getData().getGlyph(c);
    if (glyph == null) {
      return null;
    }
    GlyphData glyphData = new GlyphData();
    glyphData.fontSize = this.fontSize();
    glyphData.fontName = this.fontName;
    glyphData.c = c;
    // Render the glyph.
    renderBuffer.begin();
    layout.setText(font,"" + c);
    font.getCache().clear();
    font.getCache().setColor(Color.WHITE);
    font.getCache().addText(layout, GLYPH_OFFSET_X, GLYPH_TEX_SIZE - GLYPH_OFFSET_Y);
    spriteBatch.begin();
    spriteBatch.setColor(Color.YELLOW);
    spritebatchLine(spriteBatch, GLYPH_OFFSET_X, GLYPH_TEX_SIZE - GLYPH_OFFSET_Y, GLYPH_OFFSET_X + glyph.width, GLYPH_TEX_SIZE - GLYPH_OFFSET_Y, 2);
    font.getCache().draw(spriteBatch);
    spriteBatch.end();
    renderBuffer.end();
    texture.set(renderBuffer.texture());

    texture.uvOS().set(0, 0, 1, 1);

    sdfGenerator.begin();
    glyphData.symbolProperties =
        sdfGenerator.addSymbol(fontName + "_" + fontSize + "_" + (int)c + "_"+c, texture, scale, sheetPadding);
    sdfGenerator.end();
    if (glyphData.symbolProperties == null) {
      return null;
    }

    glyphDataMap.put((int)c + "", glyphData);
    return glyphData;
  }

  private float[] spritebatchLineFloats = new float[20];

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
    spriteBatch.draw(PTexture.WHITE_PIXEL().getBackingTexture(), spritebatchLineFloats, 0, spritebatchLineFloats.length);
  }

  public static class GlyphData {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private String fontName;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int fontSize;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int c;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PSDFGenerator.SymbolProperties symbolProperties;
  }
}
