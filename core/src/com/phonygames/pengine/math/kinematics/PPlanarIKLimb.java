package com.phonygames.pengine.math.kinematics;

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
          PVec3.obtain().set(endLocalTranslationFromLastNode()).mul(lastNode.templateNode().modelSpaceTransform(), 1);
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

  public void performIkToReach(PVec3 vec3) {
    performIkToReach(vec3.x(), vec3.y(), vec3.z());
  }

  public void performIkToReach(float x, float y, float z) {
    processLocalSpaceRotationAxes();
    PAssert.isTrue(nodes.size() > 0 && !endLocalTranslationFromLastNode().isZero());
    PModelInstance.Node baseNode = nodes.get(0);
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec3 t = pool.vec3().set(x, y, z);
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
    PModelInstance.Node kneeNode = kneeNodeName == null ? null : modelInstance.nodes().get(kneeNodeName);
    if (kneeNode == null) {
      if (nodes.size() == 2) {
        // Use the second node as the knee node if there are two nodes.
        kneeNode = nodes.get(1);
      } else {
        PAssert.fail("No valid knee was found for ik.");
      }
    }
    PVec3 worldSpaceBindPole = pool.vec3().set(bindPole).mul(modelInstance.worldTransform(), 0);
    worldSpaceBindPole = worldSpaceBindPole.isZero(.001f) || PKeyboard.isDown(Input.Keys.N) ? null : worldSpaceBindPole;
    PVec3 worldSpaceGoalPole = pool.vec3().set(modelSpaceKneePoleTarget).mul(modelInstance.worldTransform(), 0);
    if (modelSpaceKneePoleTarget.isZero()) {
      worldSpaceGoalPole = null;
    }
    PInverseKinematicsUtils.twoJointIK(baseNode, kneeNode, endLocalTranslationFromLastNode(), t, .001f,
                                       worldSpaceBindPole, worldSpaceGoalPole);
    pool.free();
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

  public void applyRotationAxes() {
    for (int a = 0; a < nodes.size(); a++) {
      PModelInstance.Node node = nodes.get(a);
      node.transform().rotate(nodeLocalRotationAxes.get(a), nodeRotationOffsets.get(a).x());
    }
    nodes.get(0).recalcNodeWorldTransformsRecursive(true);
  }
}
