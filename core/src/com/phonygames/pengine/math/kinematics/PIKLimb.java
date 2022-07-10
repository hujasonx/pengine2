package com.phonygames.pengine.math.kinematics;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** A chain of PModelInstance.Nodes that should be treated as a limb for IK purposes. */
public class PIKLimb implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PIKLimb> staticPool = new PPool<PIKLimb>() {
    @Override protected PIKLimb newObject() {
      return new PIKLimb();
    }
  };
  private final PList<PVec3> bindSpaceRotationAxes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  private final PList<PModelInstance.Node> nodes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  private PModelInstance.Node kneeNode = null;
  private PModelInstance modelInstance;

  private PIKLimb() {
  }

  public static PIKLimb obtain(PModelInstance modelInstance, String baseNode) {
    PIKLimb ret = staticPool().obtain();
    ret.modelInstance = modelInstance;
    ret.addNode(baseNode, 0, 0, 0);
    return ret;
  }

  public PIKLimb addNode(String nodeName, float bindAxisX, float bindAxisY, float bindAxisZ) {
    PAssert.isNotNull(modelInstance);
    nodes.add(modelInstance.getNode(nodeName));
    return this;
  }

  @Override public void reset() {
    nodes.clear();
    kneeNode = null;
    bindSpaceRotationAxes.clear();
    modelInstance = null;
  }
}
