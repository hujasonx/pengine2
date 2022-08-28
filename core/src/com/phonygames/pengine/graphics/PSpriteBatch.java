package com.phonygames.pengine.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec4;

import java.nio.Buffer;

public class PSpriteBatch {
  public static final int SHORTS_PER_SPRITE = 6;
  private static final String SPRITEBATCH = "spritebatch";
  public final int FLOATS_PER_SPRITE;
  private final PTexture texture = new PTexture();
  private final PVertexAttributes vertexAttributes;
  private final float[] vertices;
  private int capacity;
  private PMaterial material;
  private PMesh mesh;
  private int numQueuedSprites = 0;
  private PMat4 projectionMatrix = PMat4.obtain();
  private PShader shader;
  private boolean started = false;
  private final PRenderContext renderContext = new PRenderContext();
  public PSpriteBatch(int capacity) {
    this.capacity = capacity;
    vertexAttributes = PVertexAttributes.getPOS2D_UV0_COLPACKED0();
    FLOATS_PER_SPRITE = 4 * vertexAttributes.getNumFloatsPerVertex();
    vertices = new float[FLOATS_PER_SPRITE * capacity];
  }

  public void begin() {
    PAssert.isFalse(started);
    renderContext.start();
    renderContext.resetDefaults();
    renderContext.setCullFaceDisabled();
    started = true;
    // Bind the textures, which should be blank right now.
    if (shader != null) {
      shader.start(renderContext);
      texture.applyShaderWithUniform(UniformConstants.Sampler2D.u_texture0, shader);
    }
  }

  public PSpriteBatch setShader(PShader shader) {
    if (this.shader != null) {
      if (this.shader.isActive() && started) {
        flush();
        this.shader.end();
      }
    }
    this.shader = shader;
    if (started) {
      shader.start(renderContext);
      texture.applyShaderWithUniform(UniformConstants.Sampler2D.u_texture0, shader);
    }
    return this;
  }

  public void disableBlending() {
    PRenderContext.activeContext().disableBlending();
  }

  public void draw(PTexture texture, float x00, float y00, PVec4 colPacked00, float x10,
                   float y10, PVec4 colPacked10, float x11, float y11, PVec4 colPacked11,
                   float x01, float y01, PVec4 colPacked01) {
    if (numQueuedSprites >= capacity - 1) {
      flush();
    }
    if (this.texture.getBackingTexture() != texture.getBackingTexture()) {
      // Bind the new texture if needed.
      flush();
      // Don't copy over the uvOS, since that will be set via vertex attributes.
      this.texture.setBackingTexture(texture.getBackingTexture());
      this.texture.applyShaderWithUniform(UniformConstants.Sampler2D.u_texture0, shader);
    }
    int vIndex = numQueuedSprites * FLOATS_PER_SPRITE;
    vertices[vIndex++] = x00;
    vertices[vIndex++] = y00;
    vertices[vIndex++] = texture.uvOS().x();
    vertices[vIndex++] = texture.uvOS().y();
    vertices[vIndex++] = colPacked00.toFloatBits();
    vertices[vIndex++] = x10;
    vertices[vIndex++] = y10;
    vertices[vIndex++] = texture.uvOS().x() + texture.uvOS().z();
    vertices[vIndex++] = texture.uvOS().y();
    vertices[vIndex++] = colPacked10.toFloatBits();
    vertices[vIndex++] = x11;
    vertices[vIndex++] = y11;
    vertices[vIndex++] = texture.uvOS().x() + texture.uvOS().z();
    vertices[vIndex++] = texture.uvOS().y() + texture.uvOS().w();
    vertices[vIndex++] = colPacked11.toFloatBits();
    vertices[vIndex++] = x01;
    vertices[vIndex++] = y01;
    vertices[vIndex++] = texture.uvOS().x();
    vertices[vIndex++] = texture.uvOS().y() + texture.uvOS().w();
    vertices[vIndex++] = colPacked01.toFloatBits();
    numQueuedSprites++;
  }

  public void flush() {
    if (numQueuedSprites == 0) {
      return;
    }
    makeMeshIfNeeded();
    PAssert.isTrue(started);
    mesh.setVertices(vertices, 0, numQueuedSprites * FLOATS_PER_SPRITE);
    ((Buffer) mesh.getIndicesBuffer()).position(0);
    ((Buffer) mesh.getIndicesBuffer()).limit(numQueuedSprites * SHORTS_PER_SPRITE);
    if (shader != null) {
      shader.set(UniformConstants.Mat4.u_viewProjTransform, projectionMatrix);
      mesh.glRenderInstanced(shader, 1);
    } else {
      PAssert.warn("PSpriteBatch::flush called with null shader.");
    }
    clear();
  }

  private void makeMeshIfNeeded() {
    if (mesh != null) {
      return;
    }
    mesh = new PMesh(false, capacity * FLOATS_PER_SPRITE, capacity * 6, vertexAttributes);
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
    flush();
    if (shader != null && shader.isActive()) {
      shader.end();
    }
    renderContext.end();
    texture.reset();
    PAssert.isTrue(started);
    started = false;
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

  public static class UniformConstants {
    public static class Sampler2D {
      public static final String u_texture0 = "u_texture0Tex";
    }

    public static class Mat4 {
      public static final String  u_viewProjTransform = "u_viewProjTransform";
    }
  }
}
