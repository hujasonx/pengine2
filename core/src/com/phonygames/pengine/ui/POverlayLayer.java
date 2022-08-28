package com.phonygames.pengine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.gl.PGLUtils;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;

public class POverlayLayer {
  private PRenderBuffer renderBuffer;
  private PShader spritebatchShader;
  private PTexture texture = new PTexture();

  public POverlayLayer() {
    renderBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("color").build();
    spritebatchShader = new PShader("", renderBuffer.fragmentLayout(), PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                                    Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                                    Gdx.files.local("engine/shader/spritebatch/default.frag.glsl"), null);
  }

  public Texture getTexture() {
    return renderBuffer.texture();
  }

  public void update() {
    renderBuffer.begin(true);
    PGLUtils.clearScreen(0, .3f, 0, 1);
    renderBuffer.spriteBatch().begin();
    renderBuffer.spriteBatch().setShader(spritebatchShader);
    texture.set(PAssetManager.textureRegion("textureAtlas/particles.atlas", "Glow", true));
    renderBuffer.spriteBatch().disableBlending();
    renderBuffer.spriteBatch()
                .draw(texture, 0, 0, PColor.BLACK, 100, 0, PColor.RED, 1, 100, PColor.YELLOW, 0, 1, PColor.GREEN);
    //        for (int a = 0; a < 10; a++) {
    //          renderBuffer.spriteBatch().draw(texture, MathUtils.random(), MathUtils.random(), PColor.BLACK, MathUtils
    //          .random(),
    //                                          MathUtils.random(), PColor.RED, MathUtils.random(), MathUtils.random(),
    //                                          PColor.YELLOW, MathUtils.random(), MathUtils.random(), PColor.GREEN);
    //        }
    renderBuffer.spriteBatch().end();
    renderBuffer.end();
  }
}
