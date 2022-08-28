package com.phonygames.pengine.util.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.font.generator.PFreetypeFontGenerator;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.sdf.PSDFGenerator;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;

public class PUtilGameFontGen {
  PFreetypeFontGenerator DEBUG_fGen;
  PSDFGenerator DEBUG_sdfGen;

  protected void debugFontGen() {
    String fontName = "Dosis";
    String fontFileName = "DosisSemibold-pxJd.ttf";
    DEBUG_fGen = new PFreetypeFontGenerator(fontName, Gdx.files.absolute(
        "D:/Coding/pengine2/assets-raw/freetype/" + fontFileName), 750);
    DEBUG_sdfGen = new PSDFGenerator(1024);
    //    DEBUG_fGen.gen('M',DEBUG_sdfGen,.1f, 8);
    DEBUG_fGen.genAll(DEBUG_sdfGen, .1f, 8);
    DEBUG_sdfGen.emitToFile(Gdx.files.local("engine/font/fontsdf.png"));
    if (DEBUG_fGen != null) {
      PApplicationWindow.drawTextureToScreen(DEBUG_fGen.previewRenderBufferTexture());
    }
  }

  protected void testRenderFont() {
    PRenderBuffer testFontRenderBuffer =
        new PRenderBuffer.Builder().setStaticSize(512, 512).addFloatAttachment("color").build();
    testFontRenderBuffer.begin();
    PShader renderTextShader = new PShader("#define pos2dFlag\n", testFontRenderBuffer.fragmentLayout(),
                                           PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                                           Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                                           Gdx.files.local("engine/shader/sdf/sdf.frag.glsl"), new String[]{"color"});
    testFontRenderBuffer.spriteBatch().setShader(renderTextShader);
    PTexture texture = new PTexture();
    texture.set(new Texture(Gdx.files.internal("engine/font/fontsdf.png")));
    testFontRenderBuffer.spriteBatch().begin();
    renderTextShader.start(testFontRenderBuffer.spriteBatch().renderContext());
    testFontRenderBuffer.spriteBatch()
                        .draw(texture, 0, 512, PColor.WHITE, 512, 512, PColor.WHITE, 512, 0, PColor.WHITE, 0, 0,
                              PColor.WHITE);
    testFontRenderBuffer.spriteBatch().end();
    renderTextShader.end();
    testFontRenderBuffer.end();
    testFontRenderBuffer.emitPNG(Gdx.files.absolute("D:/testfont.png"), 0);
  }
}
