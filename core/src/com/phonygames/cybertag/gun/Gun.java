package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

public abstract class Gun {
  protected final PlayerCharacterEntity playerCharacter;
  protected final PVec3 standardOffsetFromPlayer = PVec3.obtain();
  protected PModelInstance modelInstance;

  protected Gun(String modelName, PlayerCharacterEntity playerCharacter) {
    this.playerCharacter = playerCharacter;
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
      modelInstance.worldTransform().set(playerCharacter.worldTransform()).translate(worldOffsetFromPlayer);
      modelInstance.recalcTransforms();
    }
  }

  public abstract String name();

  public void render(PRenderContext renderContext) {
    if (this.modelInstance != null) {
      this.modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
