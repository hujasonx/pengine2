package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.GL20;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PMat4;

import lombok.Getter;
import lombok.Setter;

public class PGlNode {
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
  private final PMat4 worldTransformI = new PMat4();

  @Getter
  @Setter
  private PShader defaultShader = null;

  public PGlNode(String id) {
    this.id = id;
  }

  public boolean tryRenderDefault(PRenderContext renderContext) {
    if (defaultShader == null) {
      return false;
    }

    render(defaultShader, renderContext);
    return true;
  }

  public void render(PShader shader, PRenderContext renderContext) {
    renderContext.enqueue(shader, this);
  }

  public void renderGl(PShader shader) {
    PAssert.isTrue(shader.isActive());
    material.applyUniforms(shader);
    mesh.getBackingMesh().render(shader.getShaderProgram(), GL20.GL_TRIANGLES);
  }

  private final PGlNode setWorldTransform(PMat4 worldTransform) {
    this.worldTransform.set(worldTransform);
    this.worldTransformI.set(worldTransform).inv();
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

    node.worldTransform.set(this.worldTransform);
    node.worldTransformI.set(this.worldTransformI);
    return node;
  }

  public PVertexAttributes getVertexAttributes() {
    return mesh.getVertexAttributes();
  }
}
