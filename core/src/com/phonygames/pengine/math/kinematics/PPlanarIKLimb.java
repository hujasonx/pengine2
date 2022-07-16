package com.phonygames.pengine.math.kinematics;

import android.support.annotation.Nullable;

import com.badlogic.gdx.Input;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** A chain of PModelInstance.Nodes that should be treated as a limb for IK purposes. */
public class PPlanarIKLimb implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PPool<PPlanarIKLimb> staticPool = new PPool<PPlanarIKLimb>() {
    @Override protected PPlanarIKLimb newObject() {
      return new PPlanarIKLimb();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 bindPole = PVec3.obtain(), modelSpaceKneePoleTarget = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 endLocalTranslationFromLastNode = PVec3.obtain();
  private final PList<PVec3> nodeLocalRotationAxes = new PList<>(PVec3.getStaticPool());
  private final PList<PVec1> nodePlanarLengths = new PList<>(PVec1.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PVec1> nodeRotationOffsets = new PList<>(PVec1.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  private final PList<PModelInstance.Node> nodes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  private String kneeNodeName = null;
  private PModelInstance modelInstance;

  private PPlanarIKLimb() {
  }

  public static PPlanarIKLimb obtain(PModelInstance modelInstance, PVec3 bindPole) {
    PPlanarIKLimb ret = staticPool().obtain();
    ret.modelInstance = modelInstance;
    ret.bindPole().set(bindPole).nor();
    return ret;
  }

  public PPlanarIKLimb addNode(String nodeName) {
    PAssert.isNotNull(modelInstance);
    PModelInstance.Node node = modelInstance.getNode(nodeName);
    nodeRotationOffsets.genPooledAndAdd();
    PAssert.isNotNull(node, "No node found with name: " + nodeName);
    nodes.add(node);
    return this;
  }

  public void applyRotationAxes() {
    for (int a = 0; a < nodes.size(); a++) {
      PModelInstance.Node node = nodes.get(a);
      node.transform().rotate(nodeLocalRotationAxes.get(a), nodeRotationOffsets.get(a).x());
    }
    nodes.get(0).recalcNodeWorldTransformsRecursive(true);
  }

  public PPlanarIKLimb finalizeLimbSettings() {
    processLocalSpaceRotationAxes();
    return this;
  }

  private void processLocalSpaceRotationAxes() {
    if (!nodeLocalRotationAxes.isEmpty()) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PModelInstance.Node firstNode = nodes.get(0);
      PModelInstance.Node lastNode = nodes.peek();
      PVec3 firstMSPos = firstNode.templateNode().modelSpaceTransform().getTranslation(PVec3.obtain());
      PVec3 endMSPos =
          pool.vec3(endLocalTranslationFromLastNode()).mul(lastNode.templateNode().modelSpaceTransform(), 1);
      PVec3 limbMSDelta = pool.vec3().set(endMSPos).sub(firstMSPos);
      PVec3 limbMSLongAxis = pool.vec3().set(limbMSDelta).nor();
      PVec3 rotAxisMS = pool.vec3().set(limbMSDelta).crs(bindPole).nor();
      PVec3 crossedBindPole = pool.vec3().set(bindPole).crs(rotAxisMS).nor();
      PModel.Node firstTemplate = firstNode.templateNode();
      PMat4 curTransform = pool.mat4();
      for (int a = 0; a < nodes.size(); a++) {
        PModelInstance.Node node = nodes.get(a);
        PModel.Node nodeTemplate = node.templateNode();
        PMat4 nodeBindModelSpaceTransform = nodeTemplate.modelSpaceTransform();
        PVec4 nodeBindMSRotationInv = nodeBindModelSpaceTransform.getRotation(pool.vec4()).invQuat();
        nodeBindMSRotationInv.applyAsQuat(nodeLocalRotationAxes.genPooledAndAdd().set(rotAxisMS));
      }
    }
  }

  public float maximumExtendedLength() {
    PModelInstance.Node kneeNode = getKneeNode();
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 startSPos = pool.vec3(endLocalTranslationFromLastNode()).mul(nodes.get(0).worldTransform(), 1);
      PVec3 midWSPos = pool.vec3(endLocalTranslationFromLastNode()).mul(kneeNode.worldTransform(), 1);
      PVec3 endWSPos = pool.vec3(endLocalTranslationFromLastNode()).mul(nodes.peek().worldTransform(), 1);
      return startSPos.dst(midWSPos) + endWSPos.dst(midWSPos);
    }
  }

  private PModelInstance.Node getKneeNode() {
    PModelInstance.Node kneeNode = kneeNodeName == null ? null : modelInstance.nodes().get(kneeNodeName);
    if (kneeNode == null) {
      if (nodes.size() == 2) {
        // Use the second node as the knee node if there are two nodes.
        kneeNode = nodes.get(1);
      } else {
        PAssert.fail("No valid knee was found for ik.");
      }
    }
    return kneeNode;
  }

  public void performIkToReach(PVec3 vec3) {
    performIkToReach(vec3, null, null);
  }

  public void performIkToReach(@Nullable PVec3 endEffectorGoalPosition, @Nullable PMat4 endEffectorTransform,
                               @Nullable PMat4 endEffectorBindMSTransform) {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PAssert.isFalse(endEffectorGoalPosition == null && endEffectorTransform == null);
    if (endEffectorGoalPosition == null) {
      endEffectorGoalPosition = endEffectorTransform.getTranslation(pool.vec3());
    }
    processLocalSpaceRotationAxes();
    PAssert.isTrue(nodes.size() > 0 && !endLocalTranslationFromLastNode().isZero());
    PModelInstance.Node baseNode = nodes.get(0);
    PVec3 t = pool.vec3(endEffectorGoalPosition);
    PVec3 basePos = baseNode.worldTransform().getTranslation(pool.vec3());
    PVec3 currentEndPos = pool.vec3().set(endLocalTranslationFromLastNode()).mul(nodes.peek().worldTransform(), 1);
    PVec4 aGRot = baseNode.worldTransform().getRotation(pool.vec4());
    PVec4 aLRotChange = pool.vec4();
    if (nodes.size() == 1) {
      PInverseKinematicsUtils.oneJointRotationToPointTo(aLRotChange, null, basePos, currentEndPos, t, aGRot);
      baseNode.transform().rotate(aLRotChange);
      baseNode.recalcNodeWorldTransformsRecursive(true);
      pool.free();
      return;
    }
    PModelInstance.Node kneeNode = getKneeNode();
    PVec3 worldSpaceBindPole = pool.vec3().set(bindPole).mul(modelInstance.worldTransform(), 0);
    worldSpaceBindPole = worldSpaceBindPole.isZero(.001f) || PKeyboard.isDown(Input.Keys.N) ? null : worldSpaceBindPole;
    PVec3 worldSpaceGoalPole = pool.vec3().set(modelSpaceKneePoleTarget).mul(modelInstance.worldTransform(), 0);
    if (modelSpaceKneePoleTarget.isZero()) {
      worldSpaceGoalPole = null;
    }
    PInverseKinematicsUtils.twoJointIK(baseNode, kneeNode, endLocalTranslationFromLastNode(), t, .001f,
                                       worldSpaceBindPole, worldSpaceGoalPole);
    if (endEffectorTransform != null && endEffectorBindMSTransform != null) {
      PMat4 bcLocalTransform = pool.mat4(kneeNode.worldTransform()).inv().mul(endEffectorTransform);
      PVec4 bcLocalRot = bcLocalTransform.getRotation(pool.vec4());
      PMat4 bcBindLocalTransform =
          pool.mat4(kneeNode.templateNode().modelSpaceTransform()).inv().mul(endEffectorBindMSTransform);
      PVec4 bcBindLocalRot = bcBindLocalTransform.getRotation(pool.vec4());
      PVec3 bcLocalAxisN = bcLocalTransform.getTranslation(pool.vec3()).nor();
      PVec4 bcLocalTwist = pool.vec4();
      PVec4 bcBindLocalTwist = pool.vec4();
      bcLocalRot.swingTwistDecompose(bcLocalAxisN, null, bcLocalTwist);
      bcBindLocalRot.swingTwistDecompose(bcLocalAxisN, null, bcBindLocalTwist);
      PVec3 bcLocalTwistAxis = pool.vec3();
      float bcLocalTwistAngle = bcLocalTwist.nor().getAxisAngle(bcLocalTwistAxis);
      PVec3 bcBindLocalTwistAxis = pool.vec3();
      float bcBindLocalTwistAngle = bcBindLocalTwist.nor().getAxisAngle(bcBindLocalTwistAxis);
      float bRotateMixAmount = 1;
      // Note: the local and bind quaternions might be pointing in opposite directions, but this code seems to work?
      PVec4 bLocalRotChange =
          pool.vec4().setToRotation(bcLocalAxisN, (bcLocalTwistAngle - bcBindLocalTwistAngle) * bRotateMixAmount);
      kneeNode.transform().rotate(bLocalRotChange);
      kneeNode.recalcNodeWorldTransformsRecursive(true);
    }
    pool.free();
  }

  public void performIkToReach(PMat4 mat4, PMat4 endEffectorBindMSTransform) {
    performIkToReach(null, mat4, endEffectorBindMSTransform);
  }

  @Override public void reset() {
    nodes.clear();
    kneeNodeName = null;
    nodeLocalRotationAxes.clearAndFreePooled();
    nodePlanarLengths.clearAndFreePooled();
    nodeRotationOffsets.clearAndFreePooled();
    bindPole.setZero();
    modelSpaceKneePoleTarget.setZero();
    modelInstance = null;
  }

  public PPlanarIKLimb setBindPole(PVec3 pole) {
    this.bindPole().set(pole);
    return this;
  }

  public PPlanarIKLimb setBindPole(float x, float y, float z) {
    this.bindPole().set(x, y, z);
    return this;
  }

  public PPlanarIKLimb setEndLocalTranslationFromLastNode(PVec3 translation) {
    endLocalTranslationFromLastNode().set(translation);
    return this;
  }

  public PPlanarIKLimb setEndLocalTranslationFromLastNode(float x, float y, float z) {
    endLocalTranslationFromLastNode().set(x, y, z);
    return this;
  }

  public PPlanarIKLimb setModelSpaceKneePoleTarget(PVec3 target) {
    modelSpaceKneePoleTarget().set(target);
    return this;
  }

  public PPlanarIKLimb setModelSpaceKneePoleTarget(float x, float y, float z) {
    modelSpaceKneePoleTarget().set(x, y, z);
    return this;
  }
}
