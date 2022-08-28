package com.phonygames.pengine.util.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PApplicationWindow;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.font.PFont;
import com.phonygames.pengine.graphics.font.PFreetypeFontGenerator;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.sdf.PSDFGenerator;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
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
    DEBUG_sdfGen = new PSDFGenerator("font", 1024);
    //    DEBUG_fGen.gen('M',DEBUG_sdfGen,.1f, 8);
    DEBUG_fGen.genAll(DEBUG_sdfGen, .06f, 4);
    DEBUG_sdfGen.emitToFile(Gdx.files.local("engine/font/fontsdf." + PSDFSheet.FILE_EXTENSION));
    if (DEBUG_fGen != null) {
      PApplicationWindow.drawTextureToScreen(DEBUG_fGen.previewRenderBufferTexture());
    }
    DEBUG_fGen.emitToFile();
  }

  protected void testRenderFont() {
    PSpriteBatch.PSDFSpriteBatch sdfSB = PSpriteBatch.PSDFSpriteBatch.staticBatch();
    PRenderBuffer testFontRenderBuffer =
        new PRenderBuffer.Builder().setStaticSize(512, 512).addFloatAttachment("color").build();
    testFontRenderBuffer.begin();
    testFontRenderBuffer.prepSpriteBatchForRender(sdfSB);
    PShader renderTextShader = new PShader("#define pos2dFlag\n", testFontRenderBuffer.fragmentLayout(),
                                           PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                                           Gdx.files.local("engine/shader/sdf/sdf.vert.glsl"),
                                           Gdx.files.local("engine/shader/sdf/sdf.frag.glsl"), new String[]{"color"});
    sdfSB.begin();
    sdfSB.enableBlending(false);
    sdfSB.setShader(renderTextShader);
    renderTextShader.start(sdfSB.renderContext());
    PSDFSheet fontSDF = PSDFSheet.fromFileHandle(Gdx.files.local("engine/font/fontsdf." + PSDFSheet.FILE_EXTENSION));
    String fontName = "Dosis";
    PFont font = PFont.fromFileHandle(Gdx.files.local("engine/font/" + fontName + "." + PFont.FILE_EXTENSION));
    String sdfKey = font.glyphData('c').sdfKey();

    sdfSB.draw(fontSDF.get(sdfKey), .5f, .1f, 0, 0, PColor.WHITE, PColor.RED, 100, 0, PColor.WHITE, PColor.RED, 100,
               100, PColor.WHITE, PColor.RED, 0, 100, PColor.WHITE, PColor.RED);
    sdfSB.end();
    renderTextShader.end();

    testFontRenderBuffer.end();
    testFontRenderBuffer.emitPNG(Gdx.files.absolute("D:/testfont.png"), 0);
    System.out.println(font);
    PApplicationWindow.drawTextureToScreen(testFontRenderBuffer.texture());
  }
}
