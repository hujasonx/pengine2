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
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.lighting.PEnvironment;
import com.phonygames.pengine.lighting.PPointLight;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;

public class CybertagGame implements PGame {

  protected PShader testShader;
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];

  private PModel testModel;
  private PModel testBoxModel;
  private PModelInstance testModelInstance;
  private PModelInstance testBoxModelInstance;
  private PRenderContext renderContext;

  private PPbrPipeline pPbrPipeline;
  PEnvironment environment;
  PPointLight[] testLights = new PPointLight[16];

  public void init() {
    new PGltf("engine/model/RiggedFigure.glb").loadThenDo(
        new PGltf.OnloadCallback() {
          @Override
          public void onLoad(PGltf gltf) {
            testModel = gltf.getModel();
            testModelInstance = new PModelInstance(testModel, PGltf.DEFAULT_SHADER_PROVIDER);
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
        chainGlNode(glNodes, basePart, new PMaterial(basePart.getName(), null));
        PModel.Builder builder = new PModel.Builder();
        builder.addNode("box", null, glNodes, PMat4.IDT);
        testBoxModel = builder.build();
        testBoxModelInstance = new PModelInstance(testBoxModel, PGltf.DEFAULT_SHADER_PROVIDER);
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
          .setToTranslation(MathUtils.sin(PEngine.t * .5f + a) * 2 - a, MathUtils.sin(PEngine.t * .6f + 1f + 2 * a) * 2, MathUtils.sin(PEngine.t * .4f + 2f + 3 * a) * 2);
    }

    if (PMesh.FULLSCREEN_QUAD_MESH != null) {
//      PMesh.FULLSCREEN_QUAD_MESH.getBackingMesh().render(testShader.getShaderProgram(), PMesh.ShapeType.Filled.getGlType());
    }

    if (testModelInstance != null) {
      testModelInstance.getWorldTransform().idt().scl(1, 1, 1).rot(0, 1, 0, PEngine.t);
      testModelInstance.recalcTransforms();
      testModelInstance.renderWithoutBones(renderContext);
    }

    if (testBoxModelInstance != null) {
//      testBoxModelInstance.renderDefault(renderContext);

    }

    renderContext.emit();
    renderContext.end();

  }

  public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(pPbrPipeline.getLightedBuffer().getTexture());
    for (int a = 0; a < pPbrPipeline.getGBuffer().numTextures(); a++) {
      if (Gdx.input.isKeyPressed(Input.Keys.NUM_1 + a)) {
        PApplicationWindow.drawTextureToScreen(pPbrPipeline.getGBuffer().getTexture(a));

      }
    }
  }
}