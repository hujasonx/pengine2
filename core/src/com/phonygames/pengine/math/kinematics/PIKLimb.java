package com.phonygames.pengine.math.kinematics;

import com.badlogic.gdx.Input;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.input.PKeyboard;
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
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 endLocalTranslationFromLastNode = PVec3.obtain();
  private final PList<PVec3> localSpaceRotationAxes = new PList<>(PVec3.getStaticPool());
  private final PList<PVec3> modelSpaceRotationAxes = new PList<>(PVec3.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  private final PList<PModelInstance.Node> nodes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 pole = PVec3.obtain(), modelSpacePoleTarget = PVec3.obtain();
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

  public void performIkToReach(PVec3 vec3) {
    performIkToReach(vec3.x(), vec3.y(), vec3.z());
  }

  public void performIkToReach(float x, float y, float z) {
    PAssert.isTrue(nodes.size() > 0 && !endLocalTranslationFromLastNode().isZero());
    PModelInstance.Node baseNode = nodes.get(0);
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 t = pool.vec3().set(x, y, z);
    PVec3 basePos = baseNode.worldTransform().getTranslation(pool.vec3());
    PVec3 currentEndPos = pool.vec3().set(endLocalTranslationFromLastNode()).mul(nodes.peek().worldTransform(), 1);
    PVec4 aGRot = baseNode.worldTransform().getRotation(pool.vec4());
    PVec4 aLRotChange = pool.vec4();
    if (nodes.size() == 1) {
      PInverseKinematicsUtils.oneJointRotationToPointTo(aLRotChange, basePos, currentEndPos, t, aGRot);
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
    PInverseKinematicsUtils.twoJointIk(baseNode, kneeNode, endLocalTranslationFromLastNode(), t, .001f,
                                       pole.isZero(.001f) || PKeyboard.isDown(Input.Keys.N) ? null : pole);
//        System.out.println("DST:" + pool.vec3().set(endLocalTranslationFromLastNode()).mul(nodes.peek()
//        .worldTransform(), 1).dst(t));
    // Rotate so that the pole faces towards the pole target.
    if (!modelSpacePoleTarget.isZero() && !PKeyboard.isDown(Input.Keys.M)) {
      PVec3 kneePos = kneeNode.worldTransform().getTranslation(pool.vec3());
      baseNode.worldTransform().getRotation(aGRot);
      PInverseKinematicsUtils.oneJointRotationToAngleKneeTo(aLRotChange,
                                                            baseNode.worldTransform().getTranslation(pool.vec3()),
                                                            kneePos, t, pool.vec3().set(modelSpacePoleTarget)
                                                                            .mul(modelInstance.worldTransform(), 1),
                                                            aGRot);
      baseNode.transform().rotate(aLRotChange);
      baseNode.recalcNodeWorldTransformsRecursive(true);
    }
    pool.free();
  }

  @Override public void reset() {
    nodes.clear();
    kneeNode = null;
    modelSpaceRotationAxes.clearAndFreePooled();
    localSpaceRotationAxes.clearAndFreePooled();
    pole.setZero();
    modelSpacePoleTarget.setZero();
    modelInstance = null;
  }

  public PIKLimb setEndLocalTranslationFromLastNode(PVec3 translation) {
    endLocalTranslationFromLastNode().set(translation);
    return this;
  }

  public PIKLimb setEndLocalTranslationFromLastNode(int x, int y, int z) {
    endLocalTranslationFromLastNode().set(x, y, z);
    return this;
  }

  public PIKLimb setModelSpacePoleTarget(PVec3 target) {
    modelSpacePoleTarget().set(target);
    return this;
  }

  public PIKLimb setModelSpacePoleTarget(int x, int y, int z) {
    modelSpacePoleTarget().set(x, y, z);
    return this;
  }

  public PIKLimb setPole(PVec3 pole) {
    this.pole().set(pole);
    return this;
  }

  public PIKLimb setPole(int x, int y, int z) {
    this.pole().set(x, y, z);
    return this;
  }
}
