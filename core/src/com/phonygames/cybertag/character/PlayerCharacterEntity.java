package com.phonygames.cybertag.character;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.gun.Gun;
import com.phonygames.cybertag.gun.Pistol0;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.character.PLegPlacer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
import com.phonygames.pengine.physics.PPhysicsCharacterController;
import com.phonygames.pengine.util.PCharacterCameraController;
import com.phonygames.pengine.util.PPool;

public class PlayerCharacterEntity extends CharacterEntity implements PCharacterCameraController.Delegate {
  private final PPhysicsCharacterController characterController;
  private final PVec3 facingDir = PVec3.obtain().set(1, 0, 0), cameraOffsetFromOrigin =
      PVec3.obtain().set(0, 1.6f, .15f);
  private final PVec2 facingDirFlat = PVec2.obtain().set(1, 0), facingLeftFlat = PVec2.obtain().set(0, -1f);
  private PCharacterCameraController cameraController;
  private Gun gun;
  private PPlanarIKLimb leftLegLimb, leftArmLimb;
  private PLegPlacer legPlacer;
  private PModelInstance modelInstance;
  private PPlanarIKLimb rightLegLimb, rightArmLimb;

  public PlayerCharacterEntity() {
    super();
    cameraController = new PCharacterCameraController(this);
    cameraController.setActive();
    characterController = new PPhysicsCharacterController(1, .3f, 1.8f, .5f, .2f);
    characterController.setPos(10, 10, 10);
    cameraController.minPitch(-MathUtils.HALF_PI + .33f);
    initModelInstance();
    gun = new Pistol0(this);
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
      legPlacer.addLeg(leftLegLimb,"Foot.L");
      legPlacer.addLeg(rightLegLimb,"Foot.R");
    }
  }

  @Override public void getFirstPersonCameraPosition(PVec3 out, PVec3 dir) {
    if (characterController == null) {return;}
    facingDir.set(dir);
    if (!PNumberUtils.epsilonEquals(0, dir.x()) || !PNumberUtils.epsilonEquals(0, dir.z())) {
      facingDirFlat.set(dir.x(), dir.z()).nor();
    }
    facingLeftFlat.set(facingDirFlat.y(), -facingDirFlat.x());
    characterController.getPos(out).add(0, cameraOffsetFromOrigin.y(), 0)
                       .add(facingLeftFlat.x() * cameraOffsetFromOrigin.x(), 0,
                            facingLeftFlat.y() * cameraOffsetFromOrigin.x())
                       .add(facingDirFlat.x() * cameraOffsetFromOrigin.z(), 0,
                            facingDirFlat.y() * cameraOffsetFromOrigin.z());
  }

  @Override public void preLogicUpdate() {
    characterController.preLogicUpdate();
  }

  @Override public void logicUpdate() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    PVec2 inputVelocity = pool.vec2();
    PVec2 outputVelocity = pool.vec2();
    characterController.velXZ(0, 0);
    float forwardSpeed = PKeyboard.isDown(Input.Keys.SHIFT_LEFT) ? 10 : 3;
    if (PKeyboard.isDown(Input.Keys.W)) {
      inputVelocity.add(0, 1);
    }
    if (PKeyboard.isDown(Input.Keys.S)) {
      inputVelocity.add(0, -1);
    }
    if (PKeyboard.isDown(Input.Keys.A)) {
      inputVelocity.add(1, 0);
    }
    if (PKeyboard.isDown(Input.Keys.D)) {
      inputVelocity.add(-1, 0);
    }
    outputVelocity.add(facingDirFlat, forwardSpeed * inputVelocity.y());
    outputVelocity.add(facingLeftFlat, forwardSpeed * inputVelocity.x());
    characterController.velXZ(outputVelocity.x(), outputVelocity.y());
    pool.free();
  }

  @Override public void frameUpdate() {
    if (modelInstance == null) {return;}
    PPool.PoolBuffer pool = PPool.getBuffer();
    float facingDirAng = PNumberUtils.angle(0, 0, facingDir.x(), facingDir.z()) - MathUtils.HALF_PI;
    worldTransform().setToTranslation(characterController.getPos(pool.vec3())).rotate(0, -1, 0, facingDirAng);
    modelInstance.worldTransform().set(worldTransform());
    modelInstance.resetTransformsFromTemplates();
    legPlacer.frameUpdate(characterController.getVel(pool.vec3()));
    // Stop the wrist transforms from propagating recursive transform recalcs, since hand animations will be applied
    // by the gun.
    PModelInstance.Node wristR = modelInstance.getNode("Wrist.R");
    PModelInstance.Node wristL = modelInstance.getNode("Wrist.L");
    wristR.stopWorldTransformRecursionAt(true);
    wristL.stopWorldTransformRecursionAt(true);
    modelInstance.recalcTransforms();
    if (PKeyboard.isFrameJustDown(Input.Keys.R)) {
      gun.reload();
    }
    PMat4 gunTransform = pool.mat4().set(cameraController.worldTransform());
    if (PKeyboard.isDown(Input.Keys.H)) {
      gunTransform.set(worldTransform()).translate(cameraOffsetFromOrigin);
    }
    gun.frameUpdate(pool, gunTransform);
    // Ik arms.
    PVec3 wristLGoalPos = gun.getBoneWorldTransform("Wrist.L").getTranslation(pool.vec3());
    PVec3 wristRGoalPos = gun.getBoneWorldTransform("Wrist.R").getTranslation(pool.vec3());
    leftArmLimb.performIkToReach(wristLGoalPos);
    rightArmLimb.performIkToReach(gun.getBoneWorldTransform("Wrist.R"), wristR.templateNode().modelSpaceTransform());
    wristR.stopWorldTransformRecursionAt(false);
    wristL.stopWorldTransformRecursionAt(false);
    // Apply the hand animations to this model instance.
    gun.applyTransformsToCharacterModelInstance(modelInstance);
    modelInstance.recalcTransforms();
    pool.free();
  }

  @Override public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
    gun.render(renderContext);
  }
}
