package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.physics.collisionshape.PPhysicsBvhTriangleMeshShape;
import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.PPostableTaskQueue;
import com.phonygames.pengine.util.collection.PFloatList;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.Getter;
import lombok.val;

public class PModelGen implements PPostableTask {
  /** Static postable task queue for asynchronous model gen jobs. */
  @Getter
  private static final PPostableTaskQueue postableTaskQueue = new PPostableTaskQueue();
  /** A map of base part names -> the number of alphaBlend parts that have that base name. */
  protected final PStringMap<Integer> alphaBlendPartNameCounter = new PStringMap<>();
  /** Map containing all opaque mesh gens. */
  protected final PStringMap<PMeshGen> opaqueMeshGenMap = new PStringMap<>();
  /** Map containing all static physics mesh gens. */
  protected final PStringMap<PMeshGen> staticPhysicsMeshGenMap = new PStringMap<>();
  /**
   * A list of all the alphaBlend parts. Since alpha blend parts should be small to allow for depth sorting, it is not
   * easy to retrieve them by id if there are multiple with the same base name.
   */
  protected final PList<PMeshGen> alphaBlendParts = new PList<>();

  /**
   * Adds an alpha blend part to this model. Name conflicts are handled with an underscore followed by a counter that
   * starts at 1, if there already exists a part with this name.
   */
  public PMeshGen addAlphaBlendPart(String baseName, PVertexAttributes vertexAttributes) {
    int counterIndex = alphaBlendPartNameCounter.has(baseName) ? alphaBlendPartNameCounter.get(baseName) : 0;
    PMeshGen p = new PMeshGen(counterIndex == 0 ? baseName : (baseName + "_" + counterIndex), vertexAttributes);
    alphaBlendPartNameCounter.put(baseName, counterIndex + 1);
    alphaBlendParts.add(p);
    return p;
  }

  /** Emits the static physics parts to the model builder. */
  public void emitStaticPhysicsPartIntoModelBuilder(PModel.Builder builder) {
    try ( PPooledIterable.PPoolableIterator<PMap.Entry<String, PMeshGen>> it = staticPhysicsMeshGenMap.obtainIterator()) {
      while (it.hasNext()) {
        PMap.Entry<String, PMeshGen> e = it.next();
        if (e.v().isEmpty()) {continue;}
        btBvhTriangleMeshShape triangleMeshShape = e.v().getTriangleMeshShape();
        triangleMeshShape.setMargin(.04f);
        PPhysicsBvhTriangleMeshShape collisionShape = new PPhysicsBvhTriangleMeshShape(triangleMeshShape) {};
        builder.model.staticCollisionShapes().put(e.k(), collisionShape);
      }
    }
  }

  /** Emits the static physics parts to the vertex and index buffers. */
  public void emitStaticPhysicsPartIntoVerticesAndIndices(PFloatList v, PList<Integer> i) {
    try ( PPooledIterable.PPoolableIterator<PMap.Entry<String, PMeshGen>> it = staticPhysicsMeshGenMap.obtainIterator()) {
      int startIndex = v.size() / 3;
      while (it.hasNext()) {
        PMap.Entry<String, PMeshGen> e = it.next();
        if (e.v().isEmpty()) {continue;}
        v.addAll(e.v().vertices());
        for (int a = 0; a < e.v().indices().size(); a++) {
          i.add(startIndex + e.v().indices().get(a));
        }
      }
    }

  }

  public void buildSynchronous() {
    intro();
    middle();
    end();
  }

  @Override public void end() {
    modelEnd();
  }

  @Override public void intro() {
    modelIntro();
  }

  @Override public void middle() {
    modelMiddle();
  }

  protected void modelIntro() {
  }

  protected void modelMiddle() {
  }

  protected void modelEnd() {
  }

  /** Gets or adds an opaque mesh gen with the given name. */
  public PMeshGen getOrAddOpaqueMesh(String name, PVertexAttributes vertexAttributes) {
    PMeshGen meshGen = opaqueMeshGenMap.get(name);
    if (meshGen != null) {
      PAssert.isTrue(vertexAttributes.equals(meshGen.vertexAttributes()));
    }
    PMeshGen p = new PMeshGen(name, vertexAttributes);
    opaqueMeshGenMap.put(name, p);
    return p;
  }

  /** Gets or adds a static physics mesh with the given name. */
  public PMeshGen getOrAddStaticPhysicsMesh(String name) {
    PMeshGen p = staticPhysicsMeshGenMap.get(name);
    if (p != null) {return p;}
    p = new PMeshGen(name, PVertexAttributes.Templates.PHYSICS);
    staticPhysicsMeshGenMap.put(name, p);
    return p;
  }
}
