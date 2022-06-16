package com.phonygames.cybertag.world;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class World {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PModelInstance> modelInstances = new PStringMap<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PModel> models = new PStringMap<>();
  private PModelInstance testBoxModelInstance;
  private PModel testBoxModel;

  public World() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PModelGen.Part basePart;
      PModelGen.StaticPhysicsPart basePhysicsPart;

      @Override protected void modelIntro() {
        basePart = addPart("base", new PVertexAttributes(
            new VertexAttribute[]{PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos),
                                  PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.nor)}));
        basePhysicsPart = addStaticPhysicsPart("base");
      }

      @Override protected void modelMiddle() {
        basePart.set(PVertexAttributes.Attribute.Keys.nor, 1, 0, 0);
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        basePart.quad(false, basePhysicsPart);
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null), null, "PBR");
        PModel.Builder builder = new PModel.Builder();
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode("box", null, glNodes, PMat4.IDT);
        testBoxModel = builder.build();
        testBoxModelInstance = new PModelInstance(testBoxModel);
        testBoxModelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
      }
    });
  }

  public void render(PRenderContext renderContext) {

    if (testBoxModelInstance != null) {
      testBoxModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
