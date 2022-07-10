package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.CharacterEntity;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public abstract class Gun {
  protected final CharacterEntity characterEntity;
  protected final PVec3 standardOffsetFromPlayer = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  protected String reloadAnimation = null;
  private float reloadAnimationT = -1;

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

  public void frameUpdate(PPool.PoolBuffer pool) {
    if (modelInstance != null) {
      PVec3 worldOffsetFromPlayer =
          pool.vec3().set(standardOffsetFromPlayer);
      modelInstance.worldTransform().set(characterEntity.worldTransform()).translate(worldOffsetFromPlayer);
      modelInstance.recalcTransforms();

      if (reloadAnimationT != -1 && reloadAnimation != null) {
        PAnimation reloadPAnimation = modelInstance.model().animations().get(reloadAnimation);
        PStringMap<PMat4> transformMap =
            modelInstance.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), true, 1);
        reloadPAnimation.apply(transformMap, reloadAnimationT % reloadPAnimation.getLength(), 1f);
        modelInstance.setNodeTransformsFromMap(transformMap, 1f);
        transformMap.clearRecursive();
        PMat4.getMat4StringMapsPool().free(transformMap);
        modelInstance.recalcTransforms();
        reloadAnimationT += PEngine.dt;
        if (reloadAnimationT > reloadPAnimation.getLength()) {reloadAnimationT = -1;}
      }
    }
  }

  public void applyTransformsToCharacterModelInstance(PModelInstance modelInstance) {
    if (this.modelInstance != null) {
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance,"Wrist.L");
      this.modelInstance.copySameNameBoneTransformsToWithRoot(modelInstance,"Wrist.R");
    }
  }

  public PMat4 getBoneWorldTransform(String name) {
    return modelInstance.nodes().get(name).worldTransform();
  }

  public void reload() {
    reloadAnimationT = 0;
  }

  public abstract String name();

  public void render(PRenderContext renderContext) {
    if (this.modelInstance != null) {
      this.modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
