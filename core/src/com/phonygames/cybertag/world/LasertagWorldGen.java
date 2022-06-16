package com.phonygames.cybertag.world;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;

public class LasertagWorldGen {
  public LasertagWorldGen() {
  }

  public void gen(@NonNull final OnFinishedCallback onFinishedCallback) {
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
        basePart.set(PVertexAttributes.Attribute.Keys.nor, 0, 0, 1);
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
        onFinishedCallback.onFinished(builder.build());
      }
    });
  }

  static abstract class OnFinishedCallback {
    public abstract void onFinished(PModel model);
  }
}
