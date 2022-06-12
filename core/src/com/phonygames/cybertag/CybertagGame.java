package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;

import lombok.val;

public class CybertagGame implements PGame {

  protected PShader testShader;
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];

  private PModel testModel;
  private PModel testModel2;
  private PModel testBoxModel;
  private PRenderContext renderContext;

  private PPbrPipeline pPbrPipeline;
  PEnvironment environment;
  PPointLight[] testLights = new PPointLight[16];

  private PList<PModelInstance> testModelInstances = new PList<>();
  private PList<PModelInstance> testModelInstances2 = new PList<>();

  public void init() {
    new PGltf("engine/model/RiggedFigure.glb").loadThenDo(
        new PGltf.OnloadCallback() {
          @Override
          public void onLoad(PGltf gltf) {
            testModel = gltf.getModel();
            testModelInstances.add(new PModelInstance(testModel));
          }
        }
    );
    new PGltf("engine/model/blender.glb").loadThenDo(
        new PGltf.OnloadCallback() {
          @Override
          public void onLoad(PGltf gltf) {
            testModel2 = gltf.getModel();
//            testModelInstance2 = new PModelInstance(testModel2, PGltf.DEFAULT_SHADER_PROVIDER);
          }
        }
    );

    renderContext = new PRenderContext();
    pPbrPipeline = new PPbrPipeline();
    environment = new PEnvironment();

    pPbrPipeline.setEnvironment(environment);

    for (int a = 0; a < testLights.length; a++) {
      environment.addLight(testLights[a] = new PPointLight());
    }

    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PModelGen.Part basePart;

      @Override
      protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getPOSITION());
      }

      @Override
      protected void modelMiddle() {
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        basePart.quad(false);
      }

      @Override
      protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.getName(), null), null);
        PModel.Builder builder = new PModel.Builder();
        builder.addNode("box", null, glNodes, PMat4.IDT);
        testBoxModel = builder.build();
//        testBoxModelInstance = new PModelInstance(testBoxModel, PGltf.DEFAULT_SHADER_PROVIDER);
      }
    });
  }

  public void preLogicUpdate() {

  }

  public void logicUpdate() {

  }

  public void postLogicUpdate() {

  }

  public void preFrameUpdate() {

  }

  public void frameUpdate() {
    renderContext.getCameraRange().y(1000);
    renderContext.getCameraPos().set(5, 5, 5);
    renderContext.getCameraUp().set(0, 1, 0);
    renderContext.getCameraDir().set(-1, -1, -1);
    renderContext.start();
    pPbrPipeline.attach(renderContext);

    environment.setAmbientLightCol(.0f, .0f, .0f);
    environment.setDirectionalLightDir(0, -1, -1, 1);


    for (int a = 0; a < testLights.length; a++) {
      testLights[a].getTransform()
          .setToTranslation(MathUtils.sin(PEngine.t * .5f + a) * 2 - a,
                            MathUtils.sin(PEngine.t * .6f + 1f + 2 * a) * 2,
                            MathUtils.sin(PEngine.t * .4f + 2f + 3 * a) * 2);
    }

    val modelI = testModelInstances.isEmpty() ? null : testModelInstances.get(0);
    if (modelI != null) {
      modelI.getWorldTransform().idt().scl(1, 1, 1).rot(0, 1, 0, PEngine.t);
      modelI.recalcTransforms();

//      modelI.outputBonesToBuffers(renderContext);
      for (val e : modelI.getMaterials()) {
        e.getValue().setTex("test", PTexture.getWHITE_PIXEL());
      }
      modelI.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER, testModelInstances);
    }

    renderContext.glRenderQueue();
    renderContext.end();
  }

  public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(pPbrPipeline.getLightedBuffer().getTexture());
    for (int a = 0; a < pPbrPipeline.getGBuffer().numTextures(); a++) {
      if (Gdx.input.isKeyPressed(Input.Keys.NUM_1 + a)) {
        PGLUtils.clearScreen(0, 0, 0, 1);
        PApplicationWindow.drawTextureToScreen(pPbrPipeline.getGBuffer().getTexture(a));
      }
    }
  }
}