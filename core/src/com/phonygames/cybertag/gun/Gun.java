package com.phonygames.cybertag.gun;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.character.CharacterEntity;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.PRenderable;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public abstract class Gun implements PRenderable {
  protected final CharacterEntity characterEntity;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  // Guns are placed based on their offset from the camera eyes.
  protected final PVec3 firstPersonStandardOffsetFromCamera = PVec3.obtain();
  /** The FOV when aiming down sights. */
  protected float adsFOV = PRenderContext.defaultFOV();
  /** Spring used for things like ADS. */
  protected PSODynamics.PSODynamics3 cameraOffsetSpring = PSODynamics.obtain3();
  protected PSODynamics.PSODynamics1 fovSpring = PSODynamics.obtain1().setGoal(PRenderContext.defaultFOV());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  protected PVec3 recoilCameraEulRotImpulse = PVec3.obtain();
  protected PSODynamics.PSODynamics3 recoilCameraEulRotSpring = PSODynamics.obtain3();
  protected PVec3 recoilEulRotImpulse = PVec3.obtain();
  protected PSODynamics.PSODynamics3 recoilEulRotSpring = PSODynamics.obtain3();
  protected PVec3 recoilOffsetImpulse = PVec3.obtain();
  protected PSODynamics.PSODynamics3 recoilOffsetSpring = PSODynamics.obtain3();
  protected String reloadAnimation = null;
  /** The walk cycle will be scaled between [inset, 1 - inset] */
  protected float walkCycleShakeTEdgeInset = 1;
  /** How much to scale the x offset from walking; will be multiplied with [-1, 1] */
  protected float walkCycleXOffsetScale = 1;
  /** The power to raise the Y offset to. */
  protected float walkCycleYOffsetPower = 1;
  /** How much to scale the y offset from walking; will be multiplied with [0, 1] */
  protected float walkCycleYOffsetScale = 1;
  private float lifeT = 0;
  private transient float reloadAnimationT = -1;
  /** [scale, period] */
  protected final PVec2 weaponIdleSwayLeftSettings = PVec2.obtain(), weaponIdleSwayUpSettings = PVec2.obtain(),
      weaponIdleSwayDirSettings = PVec2.obtain();

  protected Gun(String modelName, CharacterEntity characterEntity) {
    this.characterEntity = characterEntity;
    modelInstance = new PModelInstance(PAssetManager.model(modelName, true));
    modelInstance.setDataBufferEmitter(renderContext -> {
      PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
      vColIndexBuffer.addData(.1f, .2f, .5f, 1); // Base color diffuseM.
      vColIndexBuffer.addData(0, 0, 0, .3f); // Base color emissiveI.
    });
    modelInstance.material("matBase").useVColIndex(true);
  }

  public void applyTransformsToCharacterModelInstance(PModelInstance modelInstance) {
    if (this.modelInstance != null) {
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance, "Wrist.L");
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance, "Wrist.R");
    }
  }

  public float desiredFOV() {
    return fovSpring.pos().x();
  }

  public void frameUpdate(PPool.PoolBuffer pool, @Nullable PMat4 cameraTransform) {
    if (lifeT == 0) {
      cameraOffsetSpring.setGoalFlat(firstPersonStandardOffsetFromCamera);
    }
    recoilEulRotSpring.frameUpdate();
    recoilOffsetSpring.frameUpdate();
    recoilCameraEulRotSpring.frameUpdate();
    cameraOffsetSpring.frameUpdate();
    fovSpring.frameUpdate();
    if (modelInstance != null) {
      PVec4 recoilRotation = pool.vec4().setToRotationEuler(recoilEulRotSpring.pos());
      modelInstance.worldTransform().set(cameraTransform).translate(cameraOffsetSpring.pos())
                   .translate(recoilOffsetSpring.pos()).rotate(recoilRotation);
      modelInstance.recalcTransforms();
      if (reloadAnimationT != -1 && reloadAnimation != null) {
        PAnimation reloadPAnimation = modelInstance.model().animations().get(reloadAnimation);
        PStringMap<PMat4> transformMap =
            modelInstance.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), true, 1);
        reloadPAnimation.apply(transformMap, reloadAnimationT % reloadPAnimation.getLength(), 1f);
        modelInstance.setNodeTransformsFromMap(transformMap, 1f);
        transformMap.free();
        modelInstance.recalcTransforms();
        reloadAnimationT += PEngine.dt;
        if (reloadAnimationT > reloadPAnimation.getLength()) {reloadAnimationT = -1;}
      }
    }
    lifeT += PEngine.t;
  }

  public PMat4 getBoneWorldTransform(String name) {
    return modelInstance.nodes().get(name).worldTransform();
  }

  public abstract String name();

  public void primaryTriggerDown() {
  }

  public void primaryTriggerJustDown() {
    recoilEulRotSpring.vel().add(recoilEulRotImpulse);
    recoilOffsetSpring.vel().add(recoilOffsetImpulse);
    recoilCameraEulRotSpring.vel().add(recoilCameraEulRotImpulse);
  }

  public void primaryTriggerJustUp() {
  }

  public PVec3 recoilCameraEulRot() {
    return recoilCameraEulRotSpring.pos();
  }

  public void reload() {
    reloadAnimationT = 0;
  }

  @Override public void render(PRenderContext renderContext) {
    if (this.modelInstance != null) {
      this.modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }

  public void secondaryTriggerDown() {
  }

  public void secondaryTriggerJustDown() {
    if (modelInstance == null) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
    }
    float eyeDirOffset = 0.09f;
    PModelInstance.Node ironSightsNode = modelInstance.getNode("IronSights");
    if (ironSightsNode != null) {
      ironSightsNode.templateNode().modelSpaceTransform().getTranslation(cameraOffsetSpring.goal()).scl(-1)
                    .add(0, 0, eyeDirOffset);
    }
    fovSpring.setGoal(adsFOV);
  }

  public void secondaryTriggerJustUp() {
    cameraOffsetSpring.goal().set(firstPersonStandardOffsetFromCamera);
    fovSpring.setGoal(PRenderContext.defaultFOV());
    System.out.println(cameraOffsetSpring.goal());
  }

  public void setGoalsForOffsetSprings(float walkCycleT, PSODynamics.PSODynamics3 weaponPosOffsetSpring,
                                       PSODynamics.PSODynamics3 weaponPosEulRotSpring) {
    float cycleTScaled = (PNumberUtils.clamp(walkCycleT, walkCycleShakeTEdgeInset, 1 - walkCycleShakeTEdgeInset) -
                          walkCycleShakeTEdgeInset) / (1 - 2 * walkCycleShakeTEdgeInset);
    float xFactor = ((cycleTScaled - .5f) * 2);
    float yFactor = PNumberUtils.pow(Math.abs(cycleTScaled - .5f) * 2, walkCycleYOffsetPower);
    weaponPosOffsetSpring.goal().x(xFactor * walkCycleXOffsetScale); // Left.
    weaponPosOffsetSpring.goal().y(yFactor * walkCycleYOffsetScale); // Up.
    weaponPosOffsetSpring.goal().z(0);
    weaponPosOffsetSpring.goal().add(weaponIdleSwayLeftSettings.y() == 0 ? 0 : weaponIdleSwayLeftSettings.x() *
                                                                               MathUtils.sin(MathUtils.PI2 /
                                                                                             weaponIdleSwayLeftSettings.y() *
                                                                                             PEngine.t),
                                     weaponIdleSwayUpSettings.y() == 0 ? 0 : weaponIdleSwayUpSettings.x() *
                                                                             MathUtils.sin(MathUtils.PI2 /
                                                                                           weaponIdleSwayUpSettings.y() *
                                                                                           PEngine.t),
                                     weaponIdleSwayDirSettings.y() == 0 ? 0 : weaponIdleSwayDirSettings.x() *
                                                                              MathUtils.sin(MathUtils.PI2 /
                                                                                            weaponIdleSwayDirSettings.y() *
                                                                                            PEngine.t));
  }
}
