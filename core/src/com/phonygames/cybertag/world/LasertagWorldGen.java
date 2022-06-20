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
        boolean basicWallStaticBody = basicWallNode.drawCall().material().id().contains(".alsoStaticBody") && false;
        PGlNode basicFloorNode = PAssetManager.model("model/template/floor/basic.glb", true).getFirstNode();
        PMesh basicFloorMesh = basicFloorNode.drawCall().mesh();
        boolean basicFloorStaticBody = basicFloorNode.drawCall().material().id().contains(".alsoStaticBody") && false;
        Part.VertexProcessor vertexProcessor = Part.VertexProcessor.staticPool().obtain();
        PMat4 emitTransform = PMat4.obtain();
        vertexProcessor.setTransform(emitTransform);
        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, vertexProcessor,
                      basePart.vertexAttributes());
        basePart.emit(basicFloorMesh, basicFloorStaticBody ? basePhysicsPart : null, vertexProcessor,
                      basePart.vertexAttributes());
        //        vertexProcessor.setTransform(emitTransform.translate(1, 0, 0));
        vertexProcessor.setWall(0, 0, 1, 1, 0, -.5f, 1.4f, 1f);
        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, vertexProcessor,
                      basePart.vertexAttributes());
//        vertexProcessor.setFlatQuad(0, 0, 1, 1, 0, 1, 1, -.4f, 2, 0, -.6f, 2);
        vertexProcessor.setFlatQuad(0, -.6f, 2, 0, 0, 1, 1, 0, 1, 1, -.4f, 2);
        basePart.emit(basicFloorMesh, basicFloorStaticBody ? basePhysicsPart : null, vertexProcessor,
                      basePart.vertexAttributes());
        emitTransform.free();
        Part.VertexProcessor.staticPool().free(vertexProcessor);
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
