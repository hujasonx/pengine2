package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

/** Override this class to be able to process vertices. */
public class PMeshGenVertexProcessor {
  /** Processes the position vector in place. Make sure the parameter can be modified! */
  public PVec3 processPos(PVec3 inPos) {
    return inPos;
  }

  /** Processes the normal vector in place. Make sure the parameter can be modified! */
  public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
    return inNor;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec1 processOther(PVec1 inVec1, VertexAttribute vertexAttribute) {
    return inVec1;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec2 processOther(PVec2 inVec2, VertexAttribute vertexAttribute) {
    return inVec2;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec3 processOther(PVec3 inVec3, VertexAttribute vertexAttribute) {
    return inVec3;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec4 processOther(PVec4 inVec4, VertexAttribute vertexAttribute) {
    return inVec4;
  }


}
