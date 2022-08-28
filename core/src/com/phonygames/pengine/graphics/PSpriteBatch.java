package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec4;

import java.nio.Buffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Caller is responsible for starting and ending shaders around flush() or end() calls.
 */
public abstract class PSpriteBatch {
  public static final int SHORTS_PER_SPRITE = 6;
  private static final String SPRITEBATCH = "spritebatch";
  public final int floatsPerSprite;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PRenderContext renderContext = new PRenderContext();
  protected final PTexture texture0 = new PTexture();
  protected final PVertexAttributes vertexAttributes;
  protected final float[] vertices;
  protected int capacity;
  protected PMesh mesh;
  protected int numQueuedSprites = 0;
  protected PMat4 projectionMatrix = PMat4.obtain();
  protected PShader shader;
  protected boolean started = false;

  private PSpriteBatch(int capacity, PVertexAttributes vertexAttributes) {
    this.capacity = capacity;
    this.vertexAttributes = vertexAttributes;
    floatsPerSprite = 4 * vertexAttributes.getNumFloatsPerVertex();
    vertices = new float[floatsPerSprite * capacity];
  }

  public void begin() {
    PAssert.isFalse(started);
    renderContext.start();
    renderContext.resetDefaults();
    renderContext.setCullFaceDisabled();
    started = true;
  }

  public void disableBlending() {
    PRenderContext.activeContext().disableBlending();
  }
  // TODO: support more flexible vertex attributes.
  // TODO: support more flexible vertex attributes.

  /**
   * @param addOne if set, source and destination colors will be added together.
   */
  public void enableBlending(boolean addOne) {
    if (addOne) {
      PRenderContext.activeContext().setBlending(true, GL20.GL_ONE, GL20.GL_ONE, GL20.GL_ONE, GL20.GL_ONE);
    } else {
      PRenderContext.activeContext()
                    .setBlending(true, GL20.GL_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE);
    }
  }

  public void end() {
    PAssert.isTrue(started);
    flush();
    renderContext.end();
    texture0.reset();
    started = false;
  }

  public void flush() {
    if (numQueuedSprites == 0) {
      return;
    }
    makeMeshIfNeeded();
    PAssert.isTrue(started);
    mesh.setVertices(vertices, 0, numQueuedSprites * floatsPerSprite);
    ((Buffer) mesh.getIndicesBuffer()).position(0);
    ((Buffer) mesh.getIndicesBuffer()).limit(numQueuedSprites * SHORTS_PER_SPRITE);
    if (shader != null) {
      texture0.applyShaderWithUniform(UniformConstants.Sampler2D.u_texture0, shader);
      shader.set(UniformConstants.Mat4.u_viewProjTransform, projectionMatrix);
      mesh.glRenderInstanced(shader, 1);
    } else {
      throw new RuntimeException("PSpriteBatch::flush called with null shader.");
    }
    clear();
  }

  private void makeMeshIfNeeded() {
    if (mesh != null) {
      return;
    }
    mesh = new PMesh(false, capacity * floatsPerSprite, capacity * 6, vertexAttributes);
    int len = capacity * 6;
    short[] indices = new short[len];
    short j = 0;
    for (int i = 0; i < len; i += 6, j += 4) {
      indices[i + 0] = (short) (j + 0);
      indices[i + 1] = (short) (j + 1);
      indices[i + 2] = (short) (j + 2);
      indices[i + 3] = (short) (j + 0);
      indices[i + 4] = (short) (j + 2);
      indices[i + 5] = (short) (j + 3);
    }
    mesh.setIndices(indices);
  }

  public void clear() {
    PAssert.isTrue(started);
    mesh.getIndicesBuffer().position(0);
    mesh.getIndicesBuffer().limit(0);
    numQueuedSprites = 0;
  }

  public PSpriteBatch setProjectionMatrix(PMat4 matrix) {
    if (!matrix.equalsT(this.projectionMatrix)) {
      if (started) {
        flush();
      }
      this.projectionMatrix.set(matrix);
    }
    return this;
  }

  public PSpriteBatch setProjectionMatrix(Matrix4 matrix) {
    if (!matrix.equals(this.projectionMatrix.getBackingMatrix4())) {
      if (started) {
        flush();
      }
      this.projectionMatrix.set(matrix);
    }
    return this;
  }

  /**
   * The spritebatch is not responsible for starting and ending the shader.
   *
   * @param shader
   * @return
   */
  public PSpriteBatch setShader(PShader shader) {
    flush();
    this.shader = shader;
    return this;
  }

  /** POS2D_UV0_COLPACKED0 */
  public static class PGdxSpriteBatch extends PSpriteBatch {
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private static final PGdxSpriteBatch staticBatch = new PGdxSpriteBatch(1000);

    public PGdxSpriteBatch(int capacity) {
      super(capacity, PVertexAttributes.getPOS2D_UV0_COLPACKED0());
    }

    public void draw(PTexture texture, float x00, float y00, PVec4 colPacked00, float x10, float y10, PVec4 colPacked10,
                     float x11, float y11, PVec4 colPacked11, float x01, float y01, PVec4 colPacked01) {
      draw(texture.getBackingTexture(), texture.uvOS(), x00, y00, colPacked00, x10, y10, colPacked10, x11, y11,
           colPacked11, x01, y01, colPacked01);
    }

    public void draw(Texture texture, PVec4 uvOS, float x00, float y00, PVec4 colPacked00, float x10, float y10,
                     PVec4 colPacked10, float x11, float y11, PVec4 colPacked11, float x01, float y01,
                     PVec4 colPacked01) {
      if (numQueuedSprites >= capacity - 1) {
        flush();
      }
      if (this.texture0.getBackingTexture() != texture) {
        // Bind the new texture if needed.
        flush();
        // Don't copy over the uvOS, since that will be set via vertex attributes.
        this.texture0.setBackingTexture(texture);
      }
      int vIndex = numQueuedSprites * floatsPerSprite;
      vertices[vIndex++] = x00;
      vertices[vIndex++] = y00;
      vertices[vIndex++] = uvOS.x();
      vertices[vIndex++] = uvOS.y();
      vertices[vIndex++] = colPacked00.toFloatBits();
      vertices[vIndex++] = x10;
      vertices[vIndex++] = y10;
      vertices[vIndex++] = uvOS.x() + uvOS.z();
      vertices[vIndex++] = uvOS.y();
      vertices[vIndex++] = colPacked10.toFloatBits();
      vertices[vIndex++] = x11;
      vertices[vIndex++] = y11;
      vertices[vIndex++] = uvOS.x() + uvOS.z();
      vertices[vIndex++] = uvOS.y() + uvOS.w();
      vertices[vIndex++] = colPacked11.toFloatBits();
      vertices[vIndex++] = x01;
      vertices[vIndex++] = y01;
      vertices[vIndex++] = uvOS.x();
      vertices[vIndex++] = uvOS.y() + uvOS.w();
      vertices[vIndex++] = colPacked01.toFloatBits();
      numQueuedSprites++;
    }
  }

  /**
   * a_pos (2d) a_uv0, a_channel (ColorPacked) a_threshold (float) : above this threshold will be rendered.
   * a_borderThresholdInwardsOffset (float) : the border threshold inward offset. Will be subtracted. a_baseColor
   * (ColorPacked) a_borderColor (ColorPacked)
   */
  public static class PSDFSpriteBatch extends PSpriteBatch {
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private static final PSDFSpriteBatch staticBatch = new PSDFSpriteBatch(1000);

    public PSDFSpriteBatch(int capacity) {
      super(capacity, new PVertexAttributes(
          new VertexAttribute[]{PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos2d),
                                PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.uv[0]),
                                PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_channel"),
                                PVertexAttributes.Attribute.genGenericAttribute("a_threshold", 1),
                                PVertexAttributes.Attribute.genGenericAttribute("a_borderThresholdInwardsOffset", 1),
                                PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_baseColor"),
                                PVertexAttributes.Attribute.genGenericColorPackedAttribute("a_borderColor"),}));
    }

    public void draw(PSDFSheet.Symbol symbol, float threshold, float borderThresholdInwardOffset, float x00, float y00,
                     PVec4 baseColPacked00, PVec4 borderColPacked00, float x10, float y10, PVec4 baseColPacked10,
                     PVec4 borderColPacked10, float x11, float y11, PVec4 baseColPacked11, PVec4 borderColPacked11,
                     float x01, float y01, PVec4 baseColPacked01, PVec4 borderColPacked01) {
      if (numQueuedSprites >= capacity - 1) {
        flush();
      }
      if (this.texture0.getBackingTexture() != symbol.sheet().texture().getBackingTexture()) {
        // Bind the new texture if needed.
        flush();
        // Don't copy over the uvOS, since that will be set via vertex attributes.
        this.texture0.setBackingTexture(symbol.sheet().texture().getBackingTexture());
      }
      int vIndex = numQueuedSprites * floatsPerSprite;
      float channelFloatBits = symbol.channel().value().toFloatBits();
      PVec4 uvOS = PVec4.obtain();
      uvOS.set(((float) symbol.sheetX()) / symbol.sheet().texture().width(),
               1 - ((float) symbol.sheetY()) / symbol.sheet().texture().height(),
               ((float) symbol.sheetWidth()) / symbol.sheet().texture().width(),
               -((float) symbol.sheetHeight()) / symbol.sheet().texture().height());
      vertices[vIndex++] = x00;
      vertices[vIndex++] = y00;
      vertices[vIndex++] = uvOS.x();
      vertices[vIndex++] = uvOS.y();
      vertices[vIndex++] = channelFloatBits;
      vertices[vIndex++] = threshold;
      vertices[vIndex++] = borderThresholdInwardOffset;
      vertices[vIndex++] = baseColPacked00.toFloatBits();
      vertices[vIndex++] = borderColPacked00.toFloatBits();
      vertices[vIndex++] = x10;
      vertices[vIndex++] = y10;
      vertices[vIndex++] = uvOS.x() + uvOS.z();
      vertices[vIndex++] = uvOS.y();
      vertices[vIndex++] = channelFloatBits;
      vertices[vIndex++] = threshold;
      vertices[vIndex++] = borderThresholdInwardOffset;
      vertices[vIndex++] = baseColPacked10.toFloatBits();
      vertices[vIndex++] = borderColPacked10.toFloatBits();
      vertices[vIndex++] = x11;
      vertices[vIndex++] = y11;
      vertices[vIndex++] = uvOS.x() + uvOS.z();
      vertices[vIndex++] = uvOS.y() + uvOS.w();
      vertices[vIndex++] = channelFloatBits;
      vertices[vIndex++] = threshold;
      vertices[vIndex++] = borderThresholdInwardOffset;
      vertices[vIndex++] = baseColPacked11.toFloatBits();
      vertices[vIndex++] = borderColPacked11.toFloatBits();
      vertices[vIndex++] = x01;
      vertices[vIndex++] = y01;
      vertices[vIndex++] = uvOS.x();
      vertices[vIndex++] = uvOS.y() + uvOS.w();
      vertices[vIndex++] = channelFloatBits;
      vertices[vIndex++] = threshold;
      vertices[vIndex++] = borderThresholdInwardOffset;
      vertices[vIndex++] = baseColPacked01.toFloatBits();
      vertices[vIndex++] = borderColPacked01.toFloatBits();
      uvOS.free();
      numQueuedSprites++;
    }
  }

  public static class UniformConstants {
    public static class Mat4 {
      public static final String u_viewProjTransform = "u_viewProjTransform";
    }

    public static class Sampler2D {
      public static final String u_texture0 = "u_texture0Tex";
    }
  }
}
