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
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PStringMap;

public class CybertagGame implements PGame {
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];
  protected PShader testShader;
  PEnvironment environment;
  PPointLight[] testLights = new PPointLight[10];
  private PModel catModel, duckModel;
  private PList<PModelInstance> catModelInstances = new PList<>(), duckModelInstances = new PList<>();
  private PPbrPipeline pPbrPipeline;
  private PRenderContext renderContext;
  private PModel testBoxModel;
  private World world;

  @Override public void frameUpdate() {
    renderContext.cameraRange().set(.1f, 1000);
    renderContext.cameraPos().set(.8f, 1, .8f);
    renderContext.cameraUp().set(0, 1, 0);
    renderContext.cameraDir().set(-.2f, -.5f, -.2f);
    renderContext.start();
    renderContext.setPhysicsDebugDrawerCameraFromSelf();
    pPbrPipeline.attach(renderContext);
    world.render(renderContext);
    // Set environment.
    environment.setAmbientLightCol(.1f, .1f, .1f);
    PVec3 tempV3 = PVec3.obtain().set(1, -1, -1).nor();
    environment.setDirectionalLightDir(0, tempV3.x(), tempV3.y(), tempV3.z());
    environment.setDirectionalLightColor(0, .3f, .3f, .3f);
    tempV3.free();
    for (int a = 0; a < testLights.length; a++) {
      testLights[a].transform().setToTranslation(MathUtils.sin(PEngine.t * .5f + a) * 2,
                                                 MathUtils.sin(PEngine.t * .6f + 1f + 2 * a) * 2,
                                                 MathUtils.sin(PEngine.t * .4f + 2f + 3 * a) * 2);
    }
    if (catModel != null) {
      // Process the cat model instances.
      PAnimation animation = catModel.animations().get("All Animations");
      for (int a = 0; a < catModelInstances.size; a++) {
        PModelInstance modelInstance = catModelInstances.get(a);
        modelInstance.worldTransform().idt().setToTranslation(a * .3f, 0, 0).rot(0, 1, 0, 0);
        PStringMap<PMat4> transformMap =
            modelInstance.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), true, 1);
        animation.apply(transformMap, (PEngine.t + a) % animation.getLength(), 1f);
        modelInstance.setNodeTransformsFromMap(transformMap, 1f);
        transformMap.clearRecursive();
        PMat4.getMat4StringMapsPool().free(transformMap);
        modelInstance.recalcTransforms();
      }
      // Enqueue the model instances into the buffer.
      catModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, catModelInstances, false);
    }
    if (duckModel != null) {
      for (int a = 0; a < duckModelInstances.size; a++) {
        PModelInstance modelInstance = duckModelInstances.get(a);
        modelInstance.worldTransform().idt().setToTranslation(a * .7f, 0, 1).scl(.1f).rot(0, 1, 0, a);
        modelInstance.recalcTransforms();
      }
      // Enqueue the model instances into the buffer.
      duckModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, duckModelInstances, false);
    }
    renderContext.glRenderQueue();
    renderContext.end();
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
    renderContext = new PRenderContext();
    pPbrPipeline = new PPbrPipeline();
    environment = new PEnvironment();
    pPbrPipeline.environment(environment);
    for (int a = 0; a < testLights.length; a++) {
      environment.addLight(testLights[a] = new PPointLight());
    }
    world = new World();
  }

  @Override public void logicUpdate() {
  }

  @Override public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(pPbrPipeline.lightedBuffer().texture());
    for (int a = 0; a < pPbrPipeline.gBuffer().numTextures(); a++) {
      if (Gdx.input.isKeyPressed(Input.Keys.NUM_1 + a)) {
        PGLUtils.clearScreen(0, 0, 0, 1);
        PApplicationWindow.drawTextureToScreen(pPbrPipeline.gBuffer().texture(a));
      }
    }
  }

  @Override public void postLogicUpdate() {
  }

  @Override public void preFrameUpdate() {
  }

  @Override public void preLogicUpdate() {
  }
}