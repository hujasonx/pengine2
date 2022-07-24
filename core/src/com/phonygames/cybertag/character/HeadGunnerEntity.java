package com.phonygames.cybertag.character;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.character.PLegPlacer;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
import com.phonygames.pengine.navmesh.PPath;
import com.phonygames.pengine.physics.PPhysicsCharacterController;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

public class HeadGunnerEntity extends CharacterEntity {
  /** The character controller should be reused between instances. */
  private final PPhysicsCharacterController characterController;
  private final PVec3 facingDir = PVec3.obtain().set(1, 0, 0), cameraOffsetFromOrigin =
      PVec3.obtain().set(0, 1.6f, .15f);
  private final PVec2 facingDirFlat = PVec2.obtain().set(1, 0), facingLeftFlat = PVec2.obtain().set(0, -1f);
  private final PVec3 pos = PVec3.obtain();
  private final World world;
  private PPath followingPath = null;
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

  public HeadGunnerEntity(World world) {
    super();
    this.world = world;
    characterController = PPhysicsCharacterController.obtain(1, .3f, 1.8f, .5f, .2f);
    characterController.setPos(10, 10, 10);
    characterController.addToDynamicsWorld();
    initModelInstance();
    walkCycleTSpring.setDynamicsParams(2, 1, 0);
    hipYawSpring.setDynamicsParams(2, 1, 0);
  }

  private void initModelInstance() {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      modelInstance = new PModelInstance(PAssetManager.model("model/robot/headgunner.glb", true));
      final PVec4 hairCol = PVec4.obtain().set(64f / 255f, 51f / 255f, 39f / 255f, 1.0f);
      modelInstance.setDataBufferEmitter(renderContext -> {
        PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
        vColIndexBuffer.addData(1, 224f / 255f, 189f / 255f, 1); // Skin color diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .9f); // Skin color emissiveI.
        vColIndexBuffer.addData(.95f, .95f, .95f, 1); // Eye whites diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .2f); // Eye whites emissiveI.
        vColIndexBuffer.addData(.65f, .4f, .4f, 1); // Mouth diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Mouth emissiveI.
        vColIndexBuffer.addData(52f / 255f, 136f / 255f, 232f / 255f, 1); // Iris diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .1f); // Iris emissiveI.
        vColIndexBuffer.addData(.1f, .1f, .1f, 1); // Pupil diffuseM.
        vColIndexBuffer.addData(0, 0, 0, .05f); // Pupil emissiveI.
        vColIndexBuffer.addData(hairCol); // Eyelashes diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Eyelashes emissiveI.
        vColIndexBuffer.addData(hairCol); // Eyebrows diffuseM.
        vColIndexBuffer.addData(0, 0, 0, 1); // Eyebrows emissiveI.
      });
      modelInstance.material("matBase").useVColIndex(true);
      // Left Leg.
      leftLegLimb = PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, -1)).addNode("LegUpper.L")
                                 .addNode("LegMiddle.L", true).addNode("LegLower.L");
      leftLegLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Foot.L").templateNode().transform().getTranslation(pool.vec3()));
      leftLegLimb.setModelSpaceKneePoleTarget(0, 0, -1);
      leftLegLimb.finalizeLimbSettings();
      // Right Leg.
      rightLegLimb = PPlanarIKLimb.obtain(modelInstance, pool.vec3().set(0, 0, -1));
      rightLegLimb.addNode("LegUpper.R").addNode("LegMiddle.R", true).addNode("LegLower.R");
      //      rightLegLimb.addNode("LegMiddle.R").addNode("LegLower.R");
      rightLegLimb.setEndLocalTranslationFromLastNode(
          modelInstance.getNode("Foot.R").templateNode().transform().getTranslation(pool.vec3()));
      rightLegLimb.setModelSpaceKneePoleTarget(0, 0, -1);
      rightLegLimb.finalizeLimbSettings();
      legPlacer = PLegPlacer.obtain(modelInstance);
      legPlacer.cycleTimeCurve().addKeyFrame(0, 1);
      legPlacer.cycleTimeCurve().addKeyFrame(4, .6f);
      legPlacer.cycleTimeCurve().addKeyFrame(5, .6f);
      leftLegPlacerLeg = legPlacer.addLeg(leftLegLimb, "Foot.L").preventEEAboveBase(true).maximumStrengthDis(1);
      rightLegPlacerLeg = legPlacer.addLeg(rightLegLimb, "Foot.R").preventEEAboveBase(true).maximumStrengthDis(1);
      modelInstance.addBoneRigidBodiesToSimulation();
    }
  }

  @Override public void frameUpdate() {
    if (modelInstance == null) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      float facingDirAng = PNumberUtils.angle(0, 0, facingDir.x(), facingDir.z()) - MathUtils.HALF_PI;
      PVec3 pos = pool.vec3();
      worldTransform().setToTranslation(characterController.getPos(pos)).rotate(0, -1, 0, facingDirAng);
      modelInstance.worldTransform().set(worldTransform());
      modelInstance.resetTransformsFromTemplates();
      modelInstance.recalcTransforms();
      // Apply the walk/run cycle animation.
      frameUpdateSpine(pool);
      modelInstance.recalcTransforms();
      // Apply the leg placer.
      legPlacer.frameUpdate(characterController.getVel(pool.vec3()), characterController.isOnGround());
      //      rightLegLimb.performIkToReach(world.playerCharacter.pos());
      PDebugRenderer.line(pos, 0, 0, pos, 10, 0, PVec4.ONE, PVec4.ONE, 2, 2);
      if (PKeyboard.isFrameJustDown(Input.Keys.BACKSLASH) && !modelInstance.isRagdoll()) {
        modelInstance.ragdoll(PVec3.Y);
        characterController.removeFromDynamicsWorld();
      }
      if (PKeyboard.isFrameJustDown(Input.Keys.ENTER)) {
        if (followingPath != null) {
          followingPath.free();
        }
        followingPath = world.tileCache.obtainPath(pos(), world.playerCharacter.pos(), pool.vec3(2, 4, 2));
      }
      if (followingPath != null) {
        followingPath.backingCurve().debugRender();
      }
    }
  }

  @Override public void logicUpdate() {
  }

  @Override public PVec3 pos() {
    return characterController.getPos(pos);
  }

  @Override public void preLogicUpdate() {
    characterController.preLogicUpdate();
  }

  @Override public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }

  private void frameUpdateSpine(PPool.PoolBuffer pool) {
    // Apply the walkcycle animation.
    float rawWalkRunCycleT = rawWalkRunCycleTFrameUpdate();
    PAnimation walkCycleAnimation = modelInstance.model().animations().get("WalkCycle");
    PStringMap<PMat4> transformMap =
        walkCycleAnimation.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), rawWalkRunCycleT);
    modelInstance.setNodeTransformsFromMap(transformMap, 1f);
    transformMap.free();
  }

  /** Calculates the walkRunCycleT [0, 1] based on the leg cycleT values. */
  public float rawWalkRunCycleTFrameUpdate() {
    float leftT = leftLegPlacerLeg.inCycle() ? leftLegPlacerLeg.cycleT() : 0;
    float rightT = rightLegPlacerLeg.inCycle() ? rightLegPlacerLeg.cycleT() : 0;
    float upMixAmountL = (1 - Math.abs(leftT - .5f) * 2) * leftLegPlacerLeg.cycleStrength();
    float upMixAmountR = (1 - Math.abs(rightT - .5f) * 2) * rightLegPlacerLeg.cycleStrength();
    float result = .5f * (1 + (upMixAmountL - upMixAmountR));
    walkCycleTSpring.setGoal(result);
    walkCycleTSpring.frameUpdate();
    return PNumberUtils.clamp(walkCycleTSpring.pos().x(), 0, 1);
  }
}
