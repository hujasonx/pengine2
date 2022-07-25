package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PPbrPipeline;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.input.PMouse;
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.kinematics.PPlanarIKLimb;
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
  private PFlyingCameraController flyingCameraController;
  private PRenderBuffer gbufferPreviewRenderBuffer;
  private PShader gbufferPreviewShader;
  private PPbrPipeline pPbrPipeline;
  private PRenderContext renderContext;
  private PModel testBoxModel;
  private PVec3 testIKFrontGoal = PVec3.obtain();
  private PModelInstance testikModelInstance;
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
        modelInstance.worldTransform().idt().setToTranslation(a * .3f, 0, 0).rotate(0, 1, 0, a + PEngine.t);
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
        if (a == 3) {
          modelInstance.worldTransform().idt()
                       .set(testIKFrontGoal, pool.vec4().setToRotation(0, 1, 0, a), pool.vec3().set(.1f, .1f, .1f));
        } else {
          modelInstance.worldTransform().idt()
                       .set(pool.vec3().set(5 * a, 1.5f, 10), pool.vec4().setToRotation(0, 1, 0, a),
                            pool.vec3().set(.1f, .1f, .1f));
        }
        modelInstance.recalcTransforms();
      }
      // Enqueue the model instances into the buffer.
      duckModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, duckModelInstances, false);
    }
    //    if (femaleModelInstance != null) {
    //      femaleModelInstance.recalcTransforms();
    //      femaleModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    //    }
//    testIk();
    renderContext.glRenderQueue();
    renderContext.end();
    pool.free();
  }

//  private void testIk() {
//    if (testikModelInstance == null) {return;}
//    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
//      testikModelInstance.resetTransformsFromTemplates();
//      testikModelInstance.recalcTransforms();
//      PVec3 frontLimbBindPole = pool.vec3().set(0, 0, 1);
//      if (PKeyboard.isDown(Input.Keys.B)) {frontLimbBindPole.set(1, 0, 1);}
//      PPlanarIKLimb frontLimb = PPlanarIKLimb.obtain(testikModelInstance, frontLimbBindPole);
//      frontLimb.addNode("FrontUpper").addNode("FrontLower").setKneeNodeName("FrontLower");
//      //      frontLimb.addNode("FrontLower");
//      frontLimb.setEndLocalTranslationFromLastNode(
//          testikModelInstance.getNode("FrontTip").templateNode().transform().getTranslation(pool.vec3()));
//      if (!PKeyboard.isDown(Input.Keys.V)) {
//        frontLimb.setModelSpaceKneePoleTarget(0, 0, 1);
//      } else {
//        frontLimb.setModelSpaceKneePoleTarget(1, 0, 1);
//      }
//      frontLimb.finalizeLimbSettings();
//      //      frontLimb.nodeRotationOffsets().get(0).set(testIKFrontGoal.x());
//      //      frontLimb.nodeRotationOffsets().get(1).set(testIKFrontGoal.z());
//      frontLimb.performIkToReach(testIKFrontGoal);
//      testikModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
//      float moveSpeed = .35f * PEngine.dt;
//      if (PKeyboard.isDown(Input.Keys.UP)) {
//        testIKFrontGoal.add(0, 0, moveSpeed);
//      } else if (PKeyboard.isDown(Input.Keys.DOWN)) {
//        testIKFrontGoal.add(0, 0, -moveSpeed);
//      }
//      if (PKeyboard.isDown(Input.Keys.LEFT)) {
//        testIKFrontGoal.add(moveSpeed, 0, 0);
//      } else if (PKeyboard.isDown(Input.Keys.RIGHT)) {
//        testIKFrontGoal.add(-moveSpeed, 0, 0);
//      }
//      if (PKeyboard.isDown(Input.Keys.LEFT_BRACKET)) {
//        testIKFrontGoal.add(0, -moveSpeed, 0);
//      } else if (PKeyboard.isDown(Input.Keys.RIGHT_BRACKET)) {
//        testIKFrontGoal.add(0, moveSpeed, 0);
//      }
//    }
//  }

  private void testIk() {
    if (testikModelInstance == null) {return;}
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      testikModelInstance.resetTransformsFromTemplates();
      testikModelInstance.recalcTransforms();
      PVec3 outLimbBindPole = pool.vec3().set(0, 0, -1);
      if (PKeyboard.isDown(Input.Keys.B)) {outLimbBindPole.set(1, 0, -1);}
      PPlanarIKLimb outLimb = PPlanarIKLimb.obtain(testikModelInstance, outLimbBindPole);
      outLimb.addNode("OutUpper").addNode("OutLower").setKneeNodeName("OutLower");
      //      outLimb.addNode("OutLower");
      outLimb.setEndLocalTranslationFromLastNode(
          testikModelInstance.getNode("OutTip").templateNode().transform().getTranslation(pool.vec3()));
      if (!PKeyboard.isDown(Input.Keys.V)) {
        outLimb.setModelSpaceKneePoleTarget(0, 0, -1);
      } else {
        outLimb.setModelSpaceKneePoleTarget(1, -1, -1);
      }
      outLimb.finalizeLimbSettings();
      //      outLimb.nodeRotationOffsets().get(0).set(testIKOutGoal.x());
      //      outLimb.nodeRotationOffsets().get(1).set(testIKOutGoal.z());
      outLimb.performIkToReach(testIKFrontGoal);
      testikModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
      float moveSpeed = .35f * PEngine.dt;
      if (PKeyboard.isDown(Input.Keys.UP)) {
        testIKFrontGoal.add(0, 0, moveSpeed);
      } else if (PKeyboard.isDown(Input.Keys.DOWN)) {
        testIKFrontGoal.add(0, 0, -moveSpeed);
      }
      if (PKeyboard.isDown(Input.Keys.LEFT)) {
        testIKFrontGoal.add(moveSpeed, 0, 0);
      } else if (PKeyboard.isDown(Input.Keys.RIGHT)) {
        testIKFrontGoal.add(-moveSpeed, 0, 0);
      }
      if (PKeyboard.isDown(Input.Keys.LEFT_BRACKET)) {
        testIKFrontGoal.add(0, -moveSpeed, 0);
      } else if (PKeyboard.isDown(Input.Keys.RIGHT_BRACKET)) {
        testIKFrontGoal.add(0, moveSpeed, 0);
      }
    }
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
    renderContext.cameraRange().set(.1f, 1000);
    renderContext.cameraPos().set(2, 2, 2);
    renderContext.cameraUp().set(0, 1, 0);
    renderContext.cameraDir().set(-1, -1, -1).nor();
    pPbrPipeline = new PPbrPipeline();
    environment = new PEnvironment();
    pPbrPipeline.environment(environment);
    for (int a = 0; a < testLights.length; a++) {
      environment.addLight(testLights[a] = new PPointLight());
      testLights[a].setColor(30, 30, 30, 1);
    }
    flyingCameraController = new PFlyingCameraController(renderContext);
    world = new World();
    gbufferPreviewRenderBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuse").build();
    gbufferPreviewShader = gbufferPreviewRenderBuffer.getQuadShader(Gdx.files.local("shader/previewgbuffer.quad.glsl"));
    testikModelInstance = new PModelInstance(PAssetManager.model("model/testik.glb", true));
    testikModelInstance.material("matBase").useVColIndex(true);
    testikModelInstance.setDataBufferEmitter(renderContext -> {
      PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
      // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
      // the normal or the Index with this buffer.
      vColIndexBuffer.addData(.5f, .8f, 1, 1); // Skin color diffuseM.
      vColIndexBuffer.addData(0, 0, 0, 0); // Skin color emissiveI.
    });
    testikModelInstance.worldTransform().setToRotation(0, 1, 0, 2).translate(0, 0, .2f);
  }

  @Override public void logicUpdate() {
    world.logicUpdate();
  }

  @Override public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(pPbrPipeline.getTexture());
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
    PDebugRenderer.render(renderContext);
    PDebugRenderer.clear();
  }

  @Override public void postLogicUpdate() {
  }

  @Override public void preFrameUpdate() {
  }

  @Override public void preLogicUpdate() {
    world.preLogicUpdate();
  }
}