package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;

import lombok.Getter;
import lombok.Setter;

public class PGlNode {
  public static class UniformConstants {

    public static class Vec2 {
    }

    public static class Vec3 {
    }

    public static class Vec4 {
    }

    public static class Sampler2d {
    }

    public static class Float {
    }

    public static class Mat4 {
      public final static String u_worldTransform = "u_worldTransform";
      public final static String u_worldTransformInvTra = "u_worldTransformInvTra";
    }
  }

  public static final int MAX_BONES = 64;
  private static final float[] boneTransformBuffer = new float[MAX_BONES * 16];

  @Getter
  @Setter
  private String id;
  @Getter
  @Setter
  PMesh mesh;
  @Getter
  @Setter
  PMaterial material;
  @Getter
  private final PMat4 worldTransform = new PMat4();
  @Getter
  private final PMat4 worldTransformInvTra = new PMat4();

  @Getter
  @Setter
  private boolean useAlphaBlend;
  @Getter
  @Setter
  private PShader defaultShader = null;
  @Getter
  @Setter
  private int numInstances = 1;

  public PGlNode(String id) {
    this.id = id;
  }

  public boolean genDefaultShader(String fragmentLayout, PShaderProvider shaderProvider) {
    if (defaultShader == null) {
      if (shaderProvider != null && PRenderBuffer.getActiveBuffer() != null) {
        defaultShader = shaderProvider.provide(fragmentLayout, this);
      }
    }

    if (defaultShader == null) {
      return false;
    }

    return true;
  }

  public void renderGl(PShader shader) {
    PAssert.isTrue(shader.isActive());
    shader.set(UniformConstants.Mat4.u_worldTransform, worldTransform);
    shader.set(UniformConstants.Mat4.u_worldTransformInvTra, worldTransformInvTra);
    material.applyUniforms(shader);

    mesh.getBackingMesh().bind(shader.getShaderProgram());
    if (numInstances > 1) {
      Gdx.gl30.glDrawElementsInstanced(GL20.GL_TRIANGLES, mesh.getBackingMesh().getNumIndices(), GL20.GL_UNSIGNED_SHORT, 0, numInstances);
    } else if (numInstances == 1) {
      Gdx.gl30.glDrawElements(GL20.GL_TRIANGLES, mesh.getBackingMesh().getNumIndices(), GL20.GL_UNSIGNED_SHORT, 0);
    }

    mesh.getBackingMesh().unbind(shader.getShaderProgram());
  }

  public final PGlNode setWorldTransform(PMat4 worldTransform, PMat4 worldTransformInvTrad) {
    this.worldTransform.set(worldTransform);
    this.worldTransformInvTra.set(worldTransformInvTrad);
    return this;
  }

  public PGlNode tryDeepCopy() {
    PGlNode node = new PGlNode(id);
    node.id = this.id;
    if (material != null) {
      node.material = material.cpy(material.getId());
    }

    node.setMesh(mesh);
    node.setDefaultShader(defaultShader);
    node.useAlphaBlend = useAlphaBlend;
    node.numInstances = numInstances;

    node.worldTransform.set(this.worldTransform);
    node.worldTransformInvTra.set(this.worldTransformInvTra);
    return node;
  }

  public PVertexAttributes getVertexAttributes() {
    return mesh.getVertexAttributes();
  }
}
