package com.phonygames.cybertag;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.PGame;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.shader.PShader;

public class CybertagGame implements PGame {

  protected PShader testShader;
  protected final PRenderBuffer[] gBuffers = new PRenderBuffer[4];

  private PMaterial testMaterial;
  private PModel testModel;

  public void init() {
    for (int a = 0; a < gBuffers.length; a++) {
      gBuffers[a] = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("diffuse", GL30.GL_RGBA16F, GL30.GL_RGBA).build();
    }

    testMaterial = new PMaterial("testMaterial");
    testShader = new PShader(Gdx.files.local("engine/shader/fullscreen_quad.vert.glsl"), Gdx.files.local("shader/test.frag.glsl"));

    new PGltf("engine/model/duck.glb").loadThenDo(
        new PGltf.OnloadCallback() {
          @Override
          public void onLoad(PGltf gltf) {
            testModel = gltf.getModel();
          }
        }
    );
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
    PGLUtils.clearScreen(0, 1, 1, 1);

    if (PMesh.FULLSCREEN_QUAD_MESH != null) {
      testShader.start();
      PMesh.FULLSCREEN_QUAD_MESH.getBackingMesh().render(testShader.getShaderProgram(), PMesh.ShapeType.Filled.getGlType());
      testShader.end();
    }

    gBuffers[0].end();

  }

  public void postFrameUpdate() {
    PGLUtils.clearScreen(1, 1, 1, 1);
    PApplicationWindow.drawTextureToScreen(gBuffers[0].getTexture());
  }
}