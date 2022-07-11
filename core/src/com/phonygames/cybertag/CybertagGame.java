package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PPbrPipeline;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PCharacterCameraController;
import com.phonygames.pengine.util.PFlyingCameraController;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

public class CybertagGame implements PGame {
  protected PShader testShader;
  PEnvironment environment;
  PPointLight[] testLights = new PPointLight[10];
  private PModel catModel, duckModel, femaleModel;
  private PList<PModelInstance> catModelInstances = new PList<>(), duckModelInstances = new PList<>();
  private PModelInstance femaleModelInstance;
  private PFlyingCameraController flyingCameraController;
  private PRenderBuffer gbufferPreviewRenderBuffer;
  private PShader gbufferPreviewShader;
  private PPbrPipeline pPbrPipeline;
  private PRenderContext renderContext;
  private PModel testBoxModel;
  private World world;

  @Override public void frameUpdate() {
    PPool.PoolBuffer pool = PPool.getBuffer();
    if (PKeyboard.isFrameJustDown(Input.Keys.ESCAPE)) {
      PMouse.setCatched(!PMouse.isCatched());
    }
    if (!PMouse.isCatched() && PMouse.isFrameJustDown()) {
      PMouse.setCatched(true);
    }
    if (PCharacterCameraController.activeCharacterCameraController() != null) {
      PCharacterCameraController.activeCharacterCameraController().frameUpdate();
      PCharacterCameraController.activeCharacterCameraController().applyToRenderContext(renderContext);
    } else {
      flyingCameraController.frameUpdate();
    }
    renderContext.start();
    renderContext.setPhysicsDebugDrawerCameraFromSelf();
    pPbrPipeline.attach(renderContext);
    world.frameUpdate();
    world.render(renderContext);
    // Set environment.
    environment.setAmbientLightCol(.1f, .1f, .1f);
    PVec3 tempV3 = pool.vec3().set(1, -1, -1).nor();
    environment.setDirectionalLightDir(0, tempV3.x(), tempV3.y(), tempV3.z());
    environment.setDirectionalLightColor(0, .3f, .3f, .3f);
    for (int a = 0; a < testLights.length; a++) {
      testLights[a].transform().setToTranslation(MathUtils.sin(PEngine.t * .5f + a) * 2,
                                                 MathUtils.sin(PEngine.t * .6f + 1f + 2 * a) * 2,
                                                 MathUtils.sin(PEngine.t * .4f + 2f + 3 * a) * 2);
    }
    if (catModel != null) {
      // Process the cat model instances.
      PAnimation animation = catModel.animations().get("All Animations");
      for (int a = 0; a < catModelInstances.size(); a++) {
        PModelInstance modelInstance = catModelInstances.get(a);
        modelInstance.worldTransform().idt().setToTranslation(a * .3f, 0, 0).rot(0, 1, 0, a + PEngine.t);
        PStringMap<PMat4> transformMap =
            modelInstance.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), true, 1);
        animation.apply(transformMap, (PEngine.t + a) % animation.getLength(), 1f);
        modelInstance.setNodeTransformsFromMap(transformMap, 1f);
        transformMap.clearRecursive();
        transformMap.free();
        modelInstance.recalcTransforms();
      }
      // Enqueue the model instances into the buffer.
      catModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, catModelInstances, false);
    }
    if (duckModel != null) {
      for (int a = 0; a < duckModelInstances.size(); a++) {
        PModelInstance modelInstance = duckModelInstances.get(a);
        modelInstance.worldTransform().idt()
                     .set(pool.vec3().set(5 * a, 1.5f, 10), pool.vec4().setToRotation(0, 1, 0, a),
                          pool.vec3().set(.1f, .1f, .1f));
        modelInstance.recalcTransforms();
      }
      // Enqueue the model instances into the buffer.
      duckModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, duckModelInstances, false);
    }
    //    if (femaleModelInstance != null) {
    //      femaleModelInstance.recalcTransforms();
    //      femaleModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    //    }
    renderContext.glRenderQueue();
    renderContext.end();
    pool.free();
  }

  @Override public void init() {
    catModel = PAssetManager.model("engine/model/Persian.glb", true);
    for (int a = 0; a < 10; a++) {
      catModelInstances.add(new PModelInstance(catModel));
    }
    duckModel = PAssetManager.model("engine/model/duck.glb", true);
    for (int a = 0; a < 10; a++) {
      duckModelInstances.add(new PModelInstance(duckModel));
    }
    //    femaleModel = PAssetManager.model("model/player/female.glb", true);
    //    femaleModelInstance = new PModelInstance(femaleModel);
    //    final PVec4 hairCol = PVec4.obtain().set(64f / 255f, 51f / 255f, 39f / 255f, 1.0f);
    //    femaleModelInstance.setDataBufferEmitter(new PRenderContext.DataBufferEmitter() {
    //      @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    //        PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
    //        // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
    //        // the normal or the Index with this buffer.
    //        vColIndexBuffer.addData(1, 224f / 255f, 189f / 255f, 1); // Skin color diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, .9f); // Skin color emissiveR.
    //        vColIndexBuffer.addData(.95f, .95f, .95f, 1); // Eye whites diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, .2f); // Eye whites emissiveR.
    //        vColIndexBuffer.addData(.65f, .4f, .4f, 1); // Mouth diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, 1); // Mouth emissiveR.
    //        vColIndexBuffer.addData(52f/255f, 136f/255f, 232f/255f, 1); // Iris diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, .1f); // Iris emissiveR.
    //        vColIndexBuffer.addData(.1f, .1f, .1f, 1); // Pupil diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, .05f); // Pupil emissiveR.
    //        vColIndexBuffer.addData(hairCol); // Eyelashes diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, 1); // Eyelashes emissiveR.
    //        vColIndexBuffer.addData(hairCol); // Eyebrows diffuseM.
    //        vColIndexBuffer.addData(0, 0, 0, 1); // Eyebrows emissiveR.
    //      }
    //    });
    //    femaleModelInstance.material("matBase").useVColIndex(true);
    //    femaleModelInstance.material("matHair")
    //                       .set(PMaterial.UniformConstants.Vec4.u_diffuseCol, hairCol).setRoughness(1);
    renderContext = new PRenderContext();
    renderContext.cameraRange().set(.1f, 1000);
    renderContext.cameraPos().set(2, 2, 2);
    renderContext.cameraUp().set(0, 1, 0);
    renderContext.cameraDir().set(-1, -1, -1).nor();
    pPbrPipeline = new PPbrPipeline();
    environment = new PEnvironment();
    pPbrPipeline.environment(environment);
    for (int a = 0; a < testLights.length; a++) {
      environment.addLight(testLights[a] = new PPointLight());
      testLights[a].setColor(1, 1, 1, 1);
    }
    flyingCameraController = new PFlyingCameraController(renderContext);
    world = new World();
    gbufferPreviewRenderBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuse").build();
    gbufferPreviewShader = gbufferPreviewRenderBuffer.getQuadShader(Gdx.files.local("shader/previewgbuffer.quad.glsl"));
  }

  @Override public void logicUpdate() {
    world.logicUpdate();
  }

  @Override public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(pPbrPipeline.lightedBuffer().texture());
    for (int a = 0; a < pPbrPipeline.gBuffer().numTextures(); a++) {
      if (Gdx.input.isKeyPressed(Input.Keys.NUM_1 + a)) {
        renderContext.start();
        gbufferPreviewRenderBuffer.begin();
        gbufferPreviewShader.start(renderContext);
        gbufferPreviewShader.setWithUniform("u_dataTex", pPbrPipeline.gBuffer().texture(a));
        gbufferPreviewShader.set("u_useAlpha", 0);
        gbufferPreviewRenderBuffer.renderQuad(gbufferPreviewShader);
        gbufferPreviewShader.end();
        gbufferPreviewRenderBuffer.end();
        renderContext.end();
        PApplicationWindow.drawTextureToScreen(gbufferPreviewRenderBuffer.texture());
      }
    }
  }

  @Override public void postLogicUpdate() {
  }

  @Override public void preFrameUpdate() {
  }

  @Override public void preLogicUpdate() {
    world.preLogicUpdate();
  }
}