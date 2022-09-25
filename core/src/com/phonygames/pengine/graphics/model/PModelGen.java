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
  /** A map of base part names -> the number of dup parts that have that base name. */
  protected final PStringMap<Integer> dupPartNameCounter = new PStringMap<>();
  /** Map containing all non static physics mesh gens. */
  protected final PStringMap<PMeshGen> meshGenMap = new PStringMap<>();
  /** Map containing all static physics mesh gens. */
  protected final PStringMap<PMeshGen> staticPhysicsMeshGenMap = new PStringMap<>();

  /**
   * Adds a dup part to this model. Name conflicts are handled with an underscore followed by a counter that
   * starts at 0.
   */
  public PMeshGen addDupMeshGen(String baseName, PVertexAttributes vertexAttributes) {
    int counterIndex = dupPartNameCounter.has(baseName) ? dupPartNameCounter.get(baseName) : 0;
    String name = baseName + "_" + counterIndex;
    PMeshGen p = new PMeshGen(name, vertexAttributes);
    dupPartNameCounter.put(baseName, counterIndex + 1);
    meshGenMap.put(name, p);
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

  /** Gets or adds a non-dup mesh gen with the given name. */
  public PMeshGen getOrAddMeshGen(String name, PVertexAttributes vertexAttributes) {
    PMeshGen meshGen = meshGenMap.get(name);
    if (meshGen != null) {
      PAssert.isTrue(vertexAttributes.equals(meshGen.vertexAttributes()));
      return meshGen;
    }
    PMeshGen p = new PMeshGen(name, vertexAttributes);
    meshGenMap.put(name, p);
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
