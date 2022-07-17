package com.phonygames.cybertag.character;

import com.badlogic.gdx.Input;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.character.PLegPlacer;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
import com.phonygames.pengine.physics.PPhysicsCharacterController;
import com.phonygames.pengine.util.PPool;

public class NpcHumanoidEntity extends CharacterEntity {
  /** The character controller should be reused between instances. */
  private final PPhysicsCharacterController characterController;
  private final PVec3 facingDir = PVec3.obtain().set(1, 0, 0), cameraOffsetFromOrigin =
      PVec3.obtain().set(0, 1.6f, .15f);
  private final PVec2 facingDirFlat = PVec2.obtain().set(1, 0), facingLeftFlat = PVec2.obtain().set(0, -1f);
  private PSODynamics.PSODynamics1 hipYawSpring = PSODynamics.obtain1();
  private PPlanarIKLimb leftLegLimb, leftArmLimb;
  private PLegPlacer.Leg leftLegPlacerLeg, rightLegPlacerLeg;
  private PLegPlacer legPlacer;
  private PModelInstance modelInstance;
  private PPlanarIKLimb rightLegLimb, rightArmLimb;
  /** Below this speed, the hip rotation from velocity will be reduced */
  private float speedForMaxHipRotation = 2;
  private PSODynamics.PSODynamics1 walkCycleTSpring = PSODynamics.obtain1().setGoalFlat(.5f);
  private PSODynamics.PSODynamics3 weaponPosEulRotSpring = PSODynamics.obtain3();
  private PSODynamics.PSODynamics3 weaponPosOffsetSpring = PSODynamics.obtain3();

  public NpcHumanoidEntity() {
    super();
    characterController = PPhysicsCharacterController.obtain(1, .3f, 1.8f, .5f, .2f);
    characterController.setPos(10, 10, 10);
    characterController.addToDynamicsWorld();
    initModelInstance();
    walkCycleTSpring.setDynamicsParams(2, 1, 0);
    hipYawSpring.setDynamicsParams(2, 1, 0);
  }

  private void initModelInstance() {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      modelInstance = new PModelInstance(PAssetManager.model("model/player/female.glb", true));
      final PVec4 hairCol = PVec4.obtain().set(64f / 255f, 51f / 255f, 39f / 255f, 1.0f);
      modelInstance.setDataBufferEmitter(renderContext -> {
        PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
        // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
        // the normal or the Index with this buffer.
        vColIndexBuffer.addData(1, 224f / 255f, 189f / 255f, 1); // Skin color diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .9f); // Skin color emissiveR.
        vColIndexBuffer.addData(.95f, .95f, .95f, 1); // Eye whites diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .2f); // Eye whites emissiveR.
        vColIndexBuffer.addData(.65f, .4f, .4f, 1); // Mouth diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Mouth emissiveR.
        vColIndexBuffer.addData(52f / 255f, 136f / 255f, 232f / 255f, 1); // Iris diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .1f); // Iris emissiveR.
        vColIndexBuffer.addData(.1f, .1f, .1f, 1); // Pupil diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .05f); // Pupil emissiveR.
        vColIndexBuffer.addData(hairCol); // Eyelashes diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Eyelashes emissiveR.
        vColIndexBuffer.addData(hairCol); // Eyebrows diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Eyebrows emissiveR.
      });
      modelInstance.material("matBase").useVColIndex(true);
      modelInstance.material("matHair").set(PMaterial.UniformConstants.Vec4.u_diffuseCol, hairCol).setRoughness(1);
      // Left Leg.
      leftLegLimb =
          PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, 1)).addNode("LegUpper.L").addNode("LegLower.L");
      leftLegLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Foot.L").templateNode().transform().getTranslation(pool.vec3()));
      leftLegLimb.setModelSpaceKneePoleTarget(0, 0, 1);
      leftLegLimb.finalizeLimbSettings();
      // Left Arm.
      leftArmLimb =
          PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, -1)).addNode("ArmUpper.L").addNode("ArmLower.L");
      leftArmLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Wrist.L").templateNode().transform().getTranslation(pool.vec3()));
      leftArmLimb.setModelSpaceKneePoleTarget(2, -1, -1);
      leftArmLimb.finalizeLimbSettings();
      // Right Leg.
      rightLegLimb = PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, 1));
      rightLegLimb.addNode("LegUpper.R").addNode("LegLower.R");
      rightLegLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Foot.R").templateNode().transform().getTranslation(pool.vec3()));
      rightLegLimb.finalizeLimbSettings();
      // Right Arm.
      rightArmLimb = PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, -1));
      rightArmLimb.addNode("ArmUpper.R").addNode("ArmLower.R");
      rightArmLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Wrist.R").templateNode().transform().getTranslation(pool.vec3()));
      rightArmLimb.setModelSpaceKneePoleTarget(-2, -1, -1);
      rightArmLimb.finalizeLimbSettings();
      legPlacer = PLegPlacer.obtain(modelInstance);
      legPlacer.cycleTimeCurve().addKeyFrame(0, 1);
      legPlacer.cycleTimeCurve().addKeyFrame(4, .6f);
      legPlacer.cycleTimeCurve().addKeyFrame(5, .6f);
      leftLegPlacerLeg = legPlacer.addLeg(leftLegLimb, "Foot.L").preventEEAboveBase(true).maximumStrengthDis(1);
      rightLegPlacerLeg = legPlacer.addLeg(rightLegLimb, "Foot.R").preventEEAboveBase(true).maximumStrengthDis(1);
      modelInstance.addBoneRigidBodiesToSimulation();
    }
  }

  @Override public void preLogicUpdate() {
    characterController.preLogicUpdate();
  }

  @Override public void logicUpdate() {
  }

  @Override public void frameUpdate() {
    if (modelInstance == null) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 pos = pool.vec3();
      modelInstance.worldTransform().setToTranslation(characterController.getPos(pos));
      modelInstance.recalcTransforms();
      PDebugRenderer.line(pos, 0, 0, pos, 10, 0, PVec4.ONE, PVec4.ONE, 2, 2);
      if (PKeyboard.isFrameJustDown(Input.Keys.BACKSLASH)) {
        modelInstance.ragdoll(PVec3.Y);
        characterController.removeFromDynamicsWorld();
      }
    }
  }

  @Override public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
