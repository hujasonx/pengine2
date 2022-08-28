package com.phonygames.pengine.graphics.sdf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;
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
  private Channel lastDrawnChannel = Channel.R;
  private PStringMap<SymbolProperties> symbolProperties = new PStringMap<>();

  public PSDFGenerator(final int sideLength) {
    this.sideLength = sideLength;
    this.renderBuffer =
        new PRenderBuffer.Builder().setStaticSize(sideLength, sideLength).addFloatAttachment("sdf").build();
    this.shader = new PShader("", renderBuffer.fragmentLayout(), PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                              Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                              Gdx.files.local("engine/shader/sdfgen/sdfgen.frag.glsl"), new String[] {"sdf"});
  }

  /**
   * Adds the symbol to the SDF sheet and returns the properties for that symbol.
   *
   * @param id
   * @param texture // Expects uvOS to be applied to the input.
   * @param scale
   * @return
   */
  public SymbolProperties addSymbol(String id, PTexture texture, float scale, int sheetPadding) {
    PAssert.isTrue(this.shader.isActive(), "Call begin() first");
    SymbolProperties symbolProperties = new SymbolProperties();
    symbolProperties.id = id;
    Channel outputChannel = lastDrawnChannel;
    symbolProperties.channel = outputChannel;
    symbolProperties.scale = scale;
    symbolProperties.sheetPadding = sheetPadding;
    symbolProperties.sheetWidth = (int) (texture.width() * (texture.uvOS().z()) * scale) + 2 * sheetPadding;
    symbolProperties.sheetHeight = (int) (texture.height() * (texture.uvOS().w()) * scale) + 2 * sheetPadding;
    symbolProperties.sheetX = curX[outputChannel.index()];
    symbolProperties.sheetY = curY[outputChannel.index()];
    boolean changedSheet = false;
    if (symbolProperties.sheetTopY() >= sideLength) {
      // Move to the next channel.
      switch (outputChannel) {
        case R:
          outputChannel = Channel.G;
          break;
        case G:
          outputChannel = Channel.B;
          break;
        case B:
          outputChannel = Channel.A;
          break;
        case A:
          // Filled!
          full = true;
          return null;
      }
      changedSheet = true;
    }
    if (symbolProperties.sheetRightX() >= sideLength || changedSheet) {
      // Move to the next row.
      symbolProperties.sheetX = 0;
      symbolProperties.sheetY = curNextY[outputChannel.index()];
      curY[outputChannel.index()] = symbolProperties.sheetY;
    }
    curX[outputChannel.index()] = symbolProperties.sheetX + symbolProperties.sheetWidth;
    curNextY[outputChannel.index()] =
        Math.max(curNextY[outputChannel.index()], symbolProperties.sheetY + symbolProperties.sheetHeight);
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      renderBuffer.spriteBatch().enableBlending(true);
      renderBuffer.spriteBatch().draw(texture.getBackingTexture(), pool.vec4().set(0, 0, 1, 1), symbolProperties.sheetX,
                                      symbolProperties.sheetY, outputChannel.value(),
                                      symbolProperties.sheetX + symbolProperties.sheetWidth, symbolProperties.sheetY,
                                      outputChannel.value(), symbolProperties.sheetX + symbolProperties.sheetWidth,
                                      symbolProperties.sheetY + symbolProperties.sheetHeight, outputChannel.value(),
                                      symbolProperties.sheetX, symbolProperties.sheetY + symbolProperties.sheetHeight,
                                      outputChannel.value());
    }
    lastDrawnChannel = outputChannel;
    this.symbolProperties.put(id, symbolProperties);
    return symbolProperties;
  }

  public void begin() {
    this.renderBuffer.begin(false);
    this.renderBuffer.spriteBatch().begin();
    this.shader.start(this.renderBuffer.spriteBatch().renderContext());
    this.renderBuffer.spriteBatch().setShader(this.shader);
  }

  public boolean emitToFile(FileHandle out) {
    if (!renderBuffer.emitPNG(out, 0)) {
      return false;
    }
    return true;
  }

  public void end() {
    this.renderBuffer.spriteBatch().end();
    this.shader.end();
    this.renderBuffer.end();
  }

  enum Channel {
    R, G, B, A;

    int index() {
      switch (this) {
        case A:
          return 3;
        case B:
          return 2;
        case G:
          return 1;
        case R:
        default:
          return 0;
      }
    }

    PVec4 value() {
      switch (this) {
        case A:
          return PVec4.W;
        case B:
          return PVec4.Z;
        case G:
          return PVec4.Y;
        case R:
        default:
          return PVec4.X;
      }
    }
  }

  public static class SymbolProperties {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private Channel channel;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private String id;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private float scale;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** Inlcudes padding on both sides. */ private int sheetHeight;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int sheetPadding = 2;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** Inlcudes padding on both sides. */ private int sheetWidth;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int sheetX;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int sheetY;

    public int sheetRightX() {
      return sheetWidth + sheetX;
    }

    public int sheetTopY() {
      return sheetHeight + sheetY;
    }
  }

  public static class UniformConstants {
    public static class Vec4 {
      public static String u_inputUVOS = "u_inputUVOS";
    }
  }
}
