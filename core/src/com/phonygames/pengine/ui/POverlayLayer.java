package com.phonygames.pengine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
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
    spritebatchShader =
        new PShader("#define pos2dFlag\n", renderBuffer.fragmentLayout(), PVertexAttributes.Templates.POS2D_UV0_COLPACKED0,
                    Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                    Gdx.files.local("engine/shader/spritebatch/default.frag.glsl"), new String[]{"color"});
  }

  public Texture getTexture() {
    return renderBuffer.texture();
  }

  public void update() {
    renderBuffer.begin(true);
    PGLUtils.clearScreen(0, 0, 0, 0);
    renderBuffer.prepSpriteBatchForRender(PSpriteBatch.PGdxSpriteBatch.staticBatch());
    PSpriteBatch.PGdxSpriteBatch.staticBatch().begin();
    spritebatchShader.start(PSpriteBatch.PGdxSpriteBatch.staticBatch().renderContext());
    PSpriteBatch.PGdxSpriteBatch.staticBatch().setShader(spritebatchShader);
    texture.set(PAssetManager.textureRegion("textureAtlas/particles.atlas", "Glow", true));
    PSpriteBatch.PGdxSpriteBatch.staticBatch().enableBlending(false);
    PSpriteBatch.PGdxSpriteBatch.staticBatch()
                .draw(texture, 0, 0, PColor.WHITE, 100, 0, PColor.WHITE, 100, 100, PColor.WHITE, 0, 100, PColor.WHITE);

    PSpriteBatch.PGdxSpriteBatch.staticBatch().end();
    spritebatchShader.end();
    renderBuffer.end();
  }
}
