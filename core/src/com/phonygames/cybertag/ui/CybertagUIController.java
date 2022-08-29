package com.phonygames.cybertag.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
import com.phonygames.pengine.graphics.font.PFont;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;

/** Processes the UI for a player. */
public class CybertagUIController {
  private final PRenderBuffer uiRenderBuffer;
  private final PShader textShader;
  private final String fontName = "Dosis";
  private final PFont font;
  public CybertagUIController() {
    uiRenderBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("color").build();
    textShader = new PShader("#define pos2dFlag\n", uiRenderBuffer.fragmentLayout(),
                             PSpriteBatch.PSDFSpriteBatch.genVertexAttributes(),
                             Gdx.files.local("engine/shader/sdf/sdf.vert.glsl"),
                             Gdx.files.local("engine/shader/sdf/sdf.frag.glsl"), new String[]{"color"});
   PSDFSheet fontSDF = PSDFSheet.fromFileHandle(Gdx.files.local("engine/font/fontsdf." + PSDFSheet.FILE_EXTENSION));
    font = PFont.fromFileHandle(Gdx.files.local("engine/font/" + fontName + "." + PFont.FILE_EXTENSION));
    font.sdfSheet(fontSDF);
  }

  public void internalRender() {
    uiRenderBuffer.begin();
    PSpriteBatch.PSDFSpriteBatch sdfSB = PSpriteBatch.PSDFSpriteBatch.staticBatch();
    uiRenderBuffer.prepSpriteBatchForRender(sdfSB);
    sdfSB.begin();
    sdfSB.enableBlending(false);
    sdfSB.setAndStartShader(this.textShader);
    String sdfKey = font.glyphData('c').sdfKey();
    PVec4 baseColor = PVec4.obtain().set(1, 1, 1, 1);
    PVec4 borderColor = PVec4.obtain().set(.8f, .8f, .8f, 1);
    sdfSB.draw(font.sdfSheet().get(sdfKey), .5f, .3f, 0, 0, baseColor, borderColor, 100, 0, baseColor, borderColor, 100, 100,
               baseColor, borderColor, 0, 100, baseColor, borderColor);

    sdfSB.end();
    this.textShader.end();
    uiRenderBuffer.end();
  }

  public Texture getUITexture() {
    return uiRenderBuffer.texture();
  }
}
