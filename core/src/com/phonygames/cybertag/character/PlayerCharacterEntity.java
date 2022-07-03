package com.phonygames.cybertag.character;

import com.badlogic.gdx.Input;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.physics.PPhysicsCharacterController;

public class PlayerCharacterEntity extends CharacterEntity {
  private final PPhysicsCharacterController characterController;
  private PModelInstance modelInstance;

  public PlayerCharacterEntity() {
    super();
    PModel model = PAssetManager.model("model/player/female.glb", true);
    modelInstance = new PModelInstance(model);
    final PVec4 hairCol = PVec4.obtain().set(64f / 255f, 51f / 255f, 39f / 255f, 1.0f);
    modelInstance.setDataBufferEmitter(new PRenderContext.DataBufferEmitter() {
      @Override public void emitDataBuffersInto(PRenderContext renderContext) {
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
      }
    });
    modelInstance.material("matBase").useVColIndex(true);
    modelInstance.material("matHair").set(PMaterial.UniformConstants.Vec4.u_diffuseCol, hairCol).setRoughness(1);
    characterController = new PPhysicsCharacterController(1, .3f, 1.8f, .5f, .2f);
    characterController.pos(10, 10, 10);
  }

  @Override public void preLogicUpdate() {
    characterController.preLogicUpdate();
  }

  @Override public void logicUpdate() {
    PVec3 curPos = characterController.pos();
    if (PKeyboard.isDown(Input.Keys.UP)) {
      characterController.velXZ(1, 0);
    } else {
      characterController.velXZ(0, 0);
    }
  }

  @Override public void frameUpdate() {
  }

  @Override public void render(PRenderContext renderContext) {
    if (modelInstance == null) {return;}
    modelInstance.worldTransform().updateTranslation(characterController.pos());
    modelInstance.recalcTransforms();
    modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
  }
}
