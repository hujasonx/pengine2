package com.phonygames.pengine.graphics.sdf;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec4;

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
  private Channel lastDrawnChannel = Channel.R;

  public PSDFGenerator(final int sideLength) {
    this.sideLength = sideLength;
    this.renderBuffer =
        new PRenderBuffer.Builder().setStaticSize(sideLength, sideLength).addFloatAttachment("sdf").build();
    this.shader = new PShader("", renderBuffer.fragmentLayout(), PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                              Gdx.files.local("engine/shader/spritebatch/gdxspritebatch.vert.glsl"),
                              Gdx.files.local("engine/shader/spritebatch/gdxspritebatch.frag.glsl"), null);
  }

  public SymbolProperties addSymbol(String id, PTexture texture, Channel outputChannel) {
    SymbolProperties symbolProperties = new SymbolProperties();
//    this.renderBuffer.spriteBatch()
//                     .setColor(outputChannel.value().r(), outputChannel.value().g(), outputChannel.value().b(),
//                               outputChannel.value().a());
    //    this.renderBuffer.spriteBatch().draw(texture, symbolVertices,0,symbolVertices.length);
    return symbolProperties;
  }

  public void begin() {
    this.renderBuffer.begin(false);
    this.renderBuffer.spriteBatch().begin();
  }

  public boolean channelFull(Channel channel) {
    return curNextY[channel.index()] == -1;
  }

  public void end() {
    this.renderBuffer.spriteBatch().end();
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
    private String id;
  }
}
