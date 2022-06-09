package com.phonygames.cybertag;

import com.badlogic.gdx.graphics.GL30;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
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
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;

public class CybertagGame implements PGame {

  protected PShader testShader;
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];

  private PMaterial testMaterial;
  private PModel testModel;
  private PModel testBoxModel;
  private PModelInstance testModelInstance;
  private PModelInstance testBoxModelInstance;
  private PRenderContext renderContext;

  public void init() {
    for (int a = 0; a < gBuffers.length; a++) {
      gBuffers[a] = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuse", GL30.GL_RGBA16F, GL30.GL_RGBA).build();
    }

    testMaterial = new PMaterial("testMaterial");

    new PGltf("engine/model/blender.glb").loadThenDo(
        new PGltf.OnloadCallback() {
          @Override
          public void onLoad(PGltf gltf) {
            testModel = gltf.getModel();
//            testModelInstance = new PModelInstance(testModel, PGltf.DEFAULT_SHADER_PROVIDER);
          }
        }
    );

    renderContext = new PRenderContext();


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
        chainGlNode(glNodes, basePart, new PMaterial(basePart.getName()), null);
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
    gBuffers[0].begin();

    renderContext.getCameraPos().set(10, 10, 10);
    renderContext.getCameraUp().set(0, 1, 0);
    renderContext.getCameraDir().set(-1, -1, -1);
    renderContext.setRenderBuffer(gBuffers[0]);
    renderContext.updatePerspectiveCamera();
    renderContext.start();
    PGLUtils.clearScreen(0, 1, 1, 1);

    if (PMesh.FULLSCREEN_QUAD_MESH != null) {
//      PMesh.FULLSCREEN_QUAD_MESH.getBackingMesh().render(testShader.getShaderProgram(), PMesh.ShapeType.Filled.getGlType());
    }

    if (testModelInstance != null) {
//      testModelInstance.renderDefault(renderContext);
    }

    if (testBoxModelInstance != null) {
      testBoxModelInstance.renderDefault(renderContext);
    }
    renderContext.emit();
    renderContext.end();
    gBuffers[0].end();

  }

  public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(gBuffers[0].getTexture());
  }
}