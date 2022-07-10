package com.phonygames.pengine.math.kinematics;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
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
  private final PList<PVec3> modelSpaceRotationAxes = new PList<>(PVec3.getStaticPool());
  private final PList<PVec3> localSpaceRotationAxes = new PList<>(PVec3.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  private final PList<PModelInstance.Node> nodes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  private PModelInstance.Node kneeNode = null;
  private PModelInstance modelInstance;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 pole = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 endLocalTranslationFromLastNode = PVec3.obtain();

  private PIKLimb() {
  }

  public static PIKLimb obtain(PModelInstance modelInstance, String baseNode) {
    PIKLimb ret = staticPool().obtain();
    ret.modelInstance = modelInstance;
    ret.addNode(baseNode, 0, 0, 0);
    return ret;
  }

  public PIKLimb addNode(String nodeName, float modelSpaceAxisX, float modelSpaceAxisY, float modelSpaceAxisZ) {
    PAssert.isNotNull(modelInstance);
    PModelInstance.Node node = modelInstance.getNode(nodeName);
    nodes.add(node);
    PVec4 temp4 = node.templateNode().modelSpaceTransform().getRotation(PVec4.obtain());
    modelSpaceRotationAxes.genPooledAndAdd().set(modelSpaceAxisX, modelSpaceAxisY, modelSpaceAxisZ);
    temp4.applyAsQuat(localSpaceRotationAxes.genPooledAndAdd().set(modelSpaceAxisX, modelSpaceAxisY, modelSpaceAxisZ));
    temp4.free();
    return this;
  }

  public PIKLimb setPole(PVec3 pole) {
    PAssert.isNotNull(kneeNode, "Cant set a pole if there is no knee node set.");
    this.pole().set(pole);
    return this;
  }

  public PIKLimb setEndLocalTranslationFromLastNode(PVec3 translation) {
    PAssert.isTrue(nodes.size() > 0);
    endLocalTranslationFromLastNode().set(translation);
    return this;
  }

  public void performIkToReach(PVec3 vec3) {
    performIkToReach(vec3.x(), vec3.y(), vec3.z());
  }

  public void performIkToReach(float x, float y, float z) {
    PAssert.isTrue(nodes.size() > 0);
    PModelInstance.Node baseNode = nodes.get(0);
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 t = pool.vec3().set(x, y, z);
    PVec3 basePos = baseNode.worldTransform().getTranslation(pool.vec3());
    PVec3 currentEndPos = nodes.peek().worldTransform().translate(endLocalTranslationFromLastNode()).getTranslation(pool.vec3());
    PVec4 aGRot = baseNode.worldTransform().getRotation(pool.vec4());
    PVec4 aLRotChange = pool.vec4();
    if (nodes.size() == 1) {
      PInverseKinematicsUtils.oneJointRotationToPointTo(aLRotChange, basePos, currentEndPos, t,aGRot);
      baseNode.transform().rotate(aLRotChange);
      baseNode.recalcNodeWorldTransformsRecursive(true);
      pool.free();
      return;
    }
    PModelInstance.Node kneeNode = this.kneeNode;
    if (kneeNode == null) {
      if (nodes.size() == 2) {
        // Use the second node as the knee node if there are two nodes.
        kneeNode = nodes.get(1);
      } else {
        PAssert.fail("No valid knee was found for ik.");
      }
    }
    PVec4 bLRotChange = pool.vec4();
    pool.free();
  }

  @Override public void reset() {
    nodes.clear();
    kneeNode = null;
    modelSpaceRotationAxes.clearAndFreePooled();
    localSpaceRotationAxes.clearAndFreePooled();
    modelInstance = null;
  }
}
