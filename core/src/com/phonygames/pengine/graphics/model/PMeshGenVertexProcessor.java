package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Override this class to be able to process vertices. */
public class PMeshGenVertexProcessor {
  /** Processes the normal vector in place. Make sure the parameter can be modified! */
  public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
    return inNor;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec1 processOther(PVec1 inVec1, PVertexAttribute vertexAttribute) {
    return inVec1;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec2 processOther(PVec2 inVec2, PVertexAttribute vertexAttribute) {
    return inVec2;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec3 processOther(PVec3 inVec3, PVertexAttribute vertexAttribute) {
    return inVec3;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec4 processOther(PVec4 inVec4, PVertexAttribute vertexAttribute) {
    return inVec4;
  }

  /** Processes the position vector in place. Make sure the parameter can be modified! */
  public PVec3 processPos(PVec3 inPos) {
    return inPos;
  }

  /** Vertex processor that transforms using a transformation matrix. */
  public static class Transform extends PMeshGenVertexProcessor {
    /** The transformation matrix. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PMat4 transform = PMat4.obtain();

    /** Resets the transformation matrix. */
    public Transform reset() {
      transform.idt();
      return this;
    }
 @Override public PVec3 processPos(PVec3 inPos) {
      return inPos.mul(transform, 1);
  }
 @Override public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
      return inNor.mul(transform, 0);
  }
    }
}
