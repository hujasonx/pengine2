package com.phonygames.pengine.graphics.sdf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
import com.phonygames.pengine.graphics.font.PFont;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.sdf.PSDFSheet.Channel;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Helper class for generating SDFs.
 */
public class PSDFGenerator {
  private final int curNextY[] = new int[]{0, 0, 0, 0};
  // R, G, B, and A have their own positions.
  private final int curX[] = new int[]{0, 0, 0, 0};
  private final int curY[] = new int[]{0, 0, 0, 0};
  private final PRenderBuffer renderBuffer;
  private final PShader shader;
  private final int sideLength;
  // 5 floats per vertex: x, y, colPacked, u, v.
  private final float[] symbolVertices = new float[20];
  private boolean full = false;
  private Channel lastDrawnChannel = Channel.A; // Start with A, then go to R. This allows there to be opaque stuff.
  private final PSDFSheet outputSheet;
  private int additionalPadding = 1;

  public PSDFGenerator(String name, final int sideLength) {
    this.sideLength = sideLength;
    this.renderBuffer =
        new PRenderBuffer.Builder().setStaticSize(sideLength, sideLength).addFloatAttachment("sdf").build();
    this.shader = new PShader("", renderBuffer.fragmentLayout(), PVertexAttributes.Templates.POS2D_UV0_COLPACKED0,
                              Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                              Gdx.files.local("engine/shader/sdfgen/sdfgen.frag.glsl"), new String[]{"sdf"});
    outputSheet = PSDFSheet.blank(name);
  }

  /**
   * Adds the symbol to the SDF sheet and returns the properties for that symbol.
   *
   * @param id
   * @param texture // Expects uvOS to be applied to the input.
   * @param scale
   * @return
   */
  public PSDFSheet.Symbol addSymbol(String id, PTexture texture, float scale, int sheetPadding) {
    PAssert.isTrue(this.shader.isActive(), "Call begin() first");
    PSDFSheet.Symbol.SymbolBuilder symbolBuilder = PSDFSheet.Symbol.builder();
    symbolBuilder.id(id);
    Channel outputChannel = lastDrawnChannel;
    symbolBuilder.channel(outputChannel);
    symbolBuilder.scale(scale);
    symbolBuilder.sheetPadding(sheetPadding);
    int sheetWidth = (int) (texture.width() * (texture.uvOS().z()) * scale) + 2 * sheetPadding;
    symbolBuilder.sheetWidth(sheetWidth);
    int sheetHeight = (int) (texture.height() * (texture.uvOS().w()) * scale) + 2 * sheetPadding;
    symbolBuilder.sheetHeight(sheetHeight);
    symbolBuilder.sheetX(curX[outputChannel.index()]);
    symbolBuilder.sheetY(curY[outputChannel.index()]);
    boolean changedSheet = false;
    if (sheetHeight + curY[outputChannel.index()] >= sideLength) {
      // Move to the next channel.
      switch (outputChannel) {
        case A:
          outputChannel = Channel.B;
          break;
        case B:
          outputChannel = Channel.G;
          break;
        case G:
          outputChannel = Channel.R;
          break;
        case R:
          // Filled!
          full = true;
          return null;
      }
      changedSheet = true;
    }
    if (sheetWidth + curX[outputChannel.index()] >= sideLength || changedSheet) {
      // Move to the next row.
      symbolBuilder.sheetX(0);
      symbolBuilder.sheetY(curNextY[outputChannel.index()]);
    }
    PSDFSheet.Symbol symbol = symbolBuilder.build();
    curX[outputChannel.index()] = symbol.sheetX + symbol.sheetWidth + additionalPadding;
    curY[outputChannel.index()] = symbol.sheetY;
    curNextY[outputChannel.index()] =
        Math.max(curNextY[outputChannel.index()], symbol.sheetY + symbol.sheetHeight + additionalPadding);
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      shader.set(UniformConstants.Vec4.u_inputUVOS, texture.uvOS());
      shader.set(UniformConstants.Vec1.u_sheetPadding, sheetPadding);
      shader.set(UniformConstants.Vec1.u_scale, scale);
      shader.set(UniformConstants.Vec4.u_sheetPixelXYWH, symbol.sheetX, symbol.sheetY,
                 symbol.sheetWidth, symbol.sheetHeight);
      PSpriteBatch.PGdxSpriteBatch.staticBatch().enableBlending(true);
      PSpriteBatch.PGdxSpriteBatch.staticBatch().draw(texture.getBackingTexture(), pool.vec4().set(0, 0, 1, 1), symbol.sheetX,
                                      symbol.sheetY, outputChannel.value(),
                                      symbol.sheetX + symbol.sheetWidth, symbol.sheetY,
                                      outputChannel.value(), symbol.sheetX + symbol.sheetWidth,
                                      symbol.sheetY + symbol.sheetHeight, outputChannel.value(),
                                      symbol.sheetX, symbol.sheetY + symbol.sheetHeight,
                                      outputChannel.value());
    }
    lastDrawnChannel = outputChannel;
    outputSheet.registerSymbol(symbol);
    return symbol;
  }

  public void begin() {
    this.renderBuffer.begin(false);
    this.renderBuffer.prepSpriteBatchForRender(PSpriteBatch.PGdxSpriteBatch.staticBatch());
    PSpriteBatch.PGdxSpriteBatch.staticBatch().begin();
    PSpriteBatch.PGdxSpriteBatch.staticBatch().setAndStartShader(this.shader);
  }

  /**
   *
   * @param out the .psdf file.
   * @return
   */
  public boolean emitToFile(FileHandle out) {
    PAssert.isTrue(out.extension().equals(PSDFSheet.FILE_EXTENSION));
    String baseName = out.nameWithoutExtension();
    String imageSource = baseName+"PSDF.png";
    outputSheet.writePSDFFileHandle(out, imageSource);
    if (!renderBuffer.emitPNG(out.parent().child(imageSource), 0)) {
      return false;
    }
    return true;
  }

  public void end() {
    PSpriteBatch.PGdxSpriteBatch.staticBatch().end();
    this.shader.end();
    this.renderBuffer.end();
  }

  public static class UniformConstants {
    public static class Vec1 {
      public static String u_sheetPadding = "u_sheetPadding";
      public static String u_scale = "u_scale";
    }

    public static class Vec4 {
      public static String u_inputUVOS = "u_inputUVOS";
      public static String u_sheetPixelXYWH = "u_sheetPixelXYWH";
    }
  }
}
