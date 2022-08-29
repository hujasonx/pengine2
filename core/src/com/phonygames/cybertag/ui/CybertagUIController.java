package com.phonygames.cybertag.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
import com.phonygames.pengine.graphics.font.PFont;
import com.phonygames.pengine.graphics.font.PTextRenderer;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec4;

/** Processes the UI for a player. */
public class CybertagUIController {
  private final PRenderBuffer uiRenderBuffer;
  private final PTextRenderer textRenderer;
  private final String fontName = "Dosis";
  private final PFont font;
  public CybertagUIController() {
    uiRenderBuffer = new PRenderBuffer.Builder().setWindowScale(1).addFloatAttachment("color").build();
   PSDFSheet fontSDF = PSDFSheet.fromFileHandle(Gdx.files.local("engine/font/fontsdf." + PSDFSheet.FILE_EXTENSION));
    font = PFont.fromFileHandle(Gdx.files.local("engine/font/" + fontName + "." + PFont.FILE_EXTENSION));
    font.sdfSheet(fontSDF);
    textRenderer = new PTextRenderer(uiRenderBuffer);
  }

  public void internalRender() {
    uiRenderBuffer.begin();
    textRenderer.begin();
    textRenderer.font(font);
    textRenderer.fontSize(120);
    textRenderer.italicsAmount(.4f);
    textRenderer.topCorner().set(300, 300);
    textRenderer.addText("Hello World!");

    textRenderer.end();
    uiRenderBuffer.end();
  }

  public Texture getUITexture() {
    return uiRenderBuffer.texture();
  }
}
