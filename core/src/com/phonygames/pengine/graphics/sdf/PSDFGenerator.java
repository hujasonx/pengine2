package com.phonygames.pengine.graphics.sdf;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Helper class for generating SDFs.
 */
public class PSDFGenerator {
  private final int sideLength;
  private final PRenderBuffer renderBuffer;
  // R, G, B, and A have their own positions.
  private final int curX[] = new int[] {0, 0, 0, 0};
  private final int curY[] = new int[] {0, 0, 0, 0};
  private final int curNextY[] = new int[] {0, 0, 0, 0};
  private Channel lastDrawnChannel = Channel.R;
  // 5 floats per vertex: x, y, colPacked, u, v.
  private final float[] symbolVertices = new float[20];
  private final ShaderProgram shaderProgram;

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

  public PSDFGenerator(final int sideLength) {
    this.sideLength = sideLength;
    this.renderBuffer =
        new PRenderBuffer.Builder().setStaticSize(sideLength, sideLength).addFloatAttachment("sdf").build();
    this.shaderProgram = null;//new ShaderProgram()
  }

  public void begin() {
    this.renderBuffer.begin(false);
    this.renderBuffer.spriteBatch().begin();
  }

  public SymbolProperties addSymbol(String id, Texture texture, Channel outputChannel) {
    SymbolProperties symbolProperties = new SymbolProperties();
    this.renderBuffer.spriteBatch().setColor(outputChannel.value().r(), outputChannel.value().g(), outputChannel.value().b(), outputChannel.value().a());
    this.renderBuffer.spriteBatch().draw(texture, symbolVertices,0,symbolVertices.length);
    return symbolProperties;
  }

  public static class SymbolProperties {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private String id;

  }

  public void end() {
    this.renderBuffer.spriteBatch().end();
    this.renderBuffer.end();
  }

  public boolean channelFull(Channel channel) {
    return curNextY[channel.index()] == -1;
  }
}
