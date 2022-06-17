package com.phonygames.cybertag.world;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;

public class LasertagWorldGen {
  private boolean wasGenned = false;

  public LasertagWorldGen() {
  }

  public void gen(@NonNull final OnFinishedCallback onFinishedCallback) {
    PAssert.isFalse(wasGenned);
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
        // Get the first glNode's mesh and physics status from the model.
        PGlNode basicWallNode = PAssetManager.model("model/template/wall/basic.glb", true).getFirstNode();
        PMesh basicWallMesh = basicWallNode.drawCall().mesh();
        boolean basicWallStaticBody = basicWallNode.drawCall().material().id().contains(".alsoStaticBody");
        basicWallStaticBody = false;
        //        basePart.set(PVertexAttributes.Attribute.Keys.nor, 0, 0, 1);
        //        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        //        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        //        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        //        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        //        basePart.quad(false, basePhysicsPart);
        PMat4 emitTransform = PMat4.obtain();
        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, emitTransform,
                      basePart.vertexAttributes());
        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, emitTransform.translate(1, 0, 0),
                      basePart.vertexAttributes());
        emitTransform.free();
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null), null, "PBR");
        PModel.Builder builder = new PModel.Builder();
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode("box", null, glNodes, PMat4.IDT);
        onFinishedCallback.onFinished(builder.build());
        wasGenned = true;
      }
    });
  }

  static abstract class OnFinishedCallback {
    public abstract void onFinished(PModel model);
  }
}
