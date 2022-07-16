package com.phonygames.cybertag.gun;

import android.support.annotation.Nullable;

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
import com.phonygames.pengine.math.PVec3;
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
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  protected String reloadAnimation = null;
  /** How much to scale the x offset from walking; will be multiplied with [-1, 1] */
  protected float walkCycleXOffsetScale = .014f;
  /** How much to scale the y offset from walking; will be multiplied with [0, 1] */
  protected float walkCycleYOffsetScale = .02f;
  private transient float reloadAnimationT = -1;

  protected Gun(String modelName, CharacterEntity characterEntity) {
    this.characterEntity = characterEntity;
    modelInstance = new PModelInstance(PAssetManager.model(modelName, true));
    modelInstance.setDataBufferEmitter(renderContext -> {
      PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
      // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
      // the normal or the Index with this buffer.
      vColIndexBuffer.addData(.1f, .2f, .5f, 1); // Base color diffuseM.
      vColIndexBuffer.addData(0, 0, 0, .3f); // Base color emissiveR.
    });
    modelInstance.material("matBase").useVColIndex(true);
  }

  public void applyTransformsToCharacterModelInstance(PModelInstance modelInstance) {
    if (this.modelInstance != null) {
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance, "Wrist.L");
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance, "Wrist.R");
    }
  }

  public void frameUpdate(PPool.PoolBuffer pool, @Nullable PMat4 cameraTransform) {
    if (modelInstance != null) {
      PVec3 worldOffsetFromCamera = pool.vec3().set(firstPersonStandardOffsetFromCamera);
      modelInstance.worldTransform().set(cameraTransform).translate(worldOffsetFromCamera);
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
  }

  public PMat4 getBoneWorldTransform(String name) {
    return modelInstance.nodes().get(name).worldTransform();
  }

  public abstract String name();

  public void reload() {
    reloadAnimationT = 0;
  }

  @Override public void render(PRenderContext renderContext) {
    if (this.modelInstance != null) {
      this.modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }

  public void setGoalsForOffsetSprings(float walkCycleT, PSODynamics.PSODynamics3 weaponPosOffsetSpring,
                                       PSODynamics.PSODynamics3 weaponPosEulRotSpring) {
    // Shake the weapon based on the walk cycle T.
    float walkCycleTMin = .2f, walkCycleTMax = .8f;
    float cycleTScaled =
        (PNumberUtils.clamp(walkCycleT, walkCycleTMin, walkCycleTMax) - .2f) / (walkCycleTMax - walkCycleTMin);
    float xFactor = ((cycleTScaled - .5f) * 2);
    float yFactor = PNumberUtils.pow(Math.abs(cycleTScaled - .5f) * 2, .75f);
    weaponPosOffsetSpring.goal().x(xFactor * walkCycleXOffsetScale); // Left.
    weaponPosOffsetSpring.goal().y(yFactor * walkCycleYOffsetScale); // Left.
  }
}
