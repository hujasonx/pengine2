package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PPbrPipeline;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;

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
  private PModelInstance testBoxModelInstance;
  private PModel testModel2;

  @Override public void frameUpdate() {
    renderContext.cameraRange().y(1000);
    renderContext.cameraPos().set(2, 2, 2);
    renderContext.cameraUp().set(0, 1, 0);
    renderContext.cameraDir().set(-1, -1, -1);
    renderContext.start();
    pPbrPipeline.attach(renderContext);
    // Set environment.
    environment.setAmbientLightCol(0, 0, 0);
    PVec3 tempV3 = PVec3.obtain().set(-1, -1, -1).nor();
    environment.setDirectionalLightDir(0, tempV3.x(), tempV3.y(), tempV3.z());
    environment.setDirectionalLightColor(0, 0, 0, 0);
    tempV3.free();
    for (int a = 0; a < testLights.length; a++) {
      testLights[a].transform().setToTranslation(MathUtils.sin(PEngine.t * .5f + a) * 2,
                                                 MathUtils.sin(PEngine.t * .6f + 1f + 2 * a) * 2,
                                                 MathUtils.sin(PEngine.t * .4f + 2f + 3 * a) * 2);
    }
    //    if (catModel != null) {
    //      // Process the cat model instances.
    //      PAnimation animation = catModel.animations().get("All Animations");
    //      for (int a = 0; a < catModelInstances.size; a++) {
    //        PModelInstance modelInstance = catModelInstances.get(a);
    //        modelInstance.worldTransform().idt().setToTranslation(a * .3f, 0, 0).rot(0, 1, 0, 0);
    //        PStringMap<PMat4> transformMap =
    //            modelInstance.outputNodeTransformsToMap(PMat4.getMat4StringMapsPool().obtain(), true, 1);
    //        animation.apply(transformMap, (PEngine.t + a) % animation.getLength(), 1f);
    //        modelInstance.setNodeTransformsFromMap(transformMap, 1f);
    //        transformMap.clearRecursive();
    //        PMat4.getMat4StringMapsPool().free(transformMap);
    //        modelInstance.recalcTransforms();
    //      }
    //      // Enqueue the model instances into the buffer.
    //      catModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, catModelInstances, false);
    //    }
    //    if (duckModel != null) {
    //      for (int a = 0; a < duckModelInstances.size; a++) {
    //        PModelInstance modelInstance = duckModelInstances.get(a);
    //        modelInstance.worldTransform().idt().setToTranslation(a * .2f, 0, 1).scl(.1f).rot(0, 1, 0, a);
    //        modelInstance.recalcTransforms();
    //      }
    //      // Enqueue the model instances into the buffer.
    //      duckModel.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, duckModelInstances, false);
    //    }
    if (testBoxModelInstance != null) {
      testBoxModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
    renderContext.glRenderQueue();
    renderContext.end();
  }

  @Override public void init() {
    new PGltf("engine/model/Persian.glb").loadThenDo(new PGltf.OnloadCallback() {
      @Override public void onLoad(PGltf gltf) {
        catModel = gltf.getModel();
        for (int a = 0; a < 10; a++) {
          catModelInstances.add(new PModelInstance(catModel));
        }
      }
    });
    new PGltf("engine/model/duck.glb").loadThenDo(new PGltf.OnloadCallback() {
      @Override public void onLoad(PGltf gltf) {
        duckModel = gltf.getModel();
        for (int a = 0; a < 10; a++) {
          duckModelInstances.add(new PModelInstance(duckModel));
        }
      }
    });
    renderContext = new PRenderContext();
    pPbrPipeline = new PPbrPipeline();
    environment = new PEnvironment();
    pPbrPipeline.environment(environment);
    for (int a = 0; a < testLights.length; a++) {
      environment.addLight(testLights[a] = new PPointLight());
    }
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PModelGen.Part basePart;

      @Override protected void modelIntro() {
        basePart = addPart("base", new PVertexAttributes(
            new VertexAttribute[]{PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos),
                                  PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.nor)}));
      }

      @Override protected void modelMiddle() {
        basePart.set(PVertexAttributes.Attribute.Keys.nor, 1, 0, 0);
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        basePart.quad(false);
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null), null, "PBR");
        PModel.Builder builder = new PModel.Builder();
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode("box", null, glNodes, PMat4.IDT);
        testBoxModel = builder.build();
        testBoxModelInstance = new PModelInstance(testBoxModel);
      }
    });
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