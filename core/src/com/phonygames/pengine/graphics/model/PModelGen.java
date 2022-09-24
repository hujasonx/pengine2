package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.collection.PStringMap;

public class PModelGen implements PPostableTask {
  /** Map containing all meshes, regardless of type. */
  private final PStringMap<PMeshGen> meshGenMap = new PStringMap<>();
  /** Map containing all static physics mesh gens. */
  private final PStringMap<PMeshGen> staticPhysicsMeshGenMap = new PStringMap<>();

  public PMeshGen addMesh(String name, PVertexAttributes vertexAttributes) {
    PMeshGen p = new PMeshGen(name, vertexAttributes);
    meshGenMap.put(name, p);
    return p;
  }

  public PMeshGen addStaticPhysicsMesh(String name) {
    PMeshGen p = addMesh(name,PVertexAttributes.getPHYSICS());
    staticPhysicsMeshGenMap.put(name, p);
    return p;
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

}
