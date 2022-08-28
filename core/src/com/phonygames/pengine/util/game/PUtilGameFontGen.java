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
    PRenderBuffer testFontRenderBuffer =
        new PRenderBuffer.Builder().setStaticSize(512, 512).addFloatAttachment("color").build();
    testFontRenderBuffer.begin();
    testFontRenderBuffer.prepSpriteBatchForRender(PSpriteBatch.PGdxSpriteBatch.staticBatch());
    PShader renderTextShader = new PShader("#define pos2dFlag\n", testFontRenderBuffer.fragmentLayout(),
                                           PVertexAttributes.getPOS2D_UV0_COLPACKED0(),
                                           Gdx.files.local("engine/shader/spritebatch/default.vert.glsl"),
                                           Gdx.files.local("engine/shader/sdf/sdf.frag.glsl"), new String[]{"color"});
    PSpriteBatch.PGdxSpriteBatch.staticBatch().setShader(renderTextShader);
    PTexture texture = new PTexture();
    Texture t = new Texture(Gdx.files.internal("engine/font/fontsdfPSDF.png"));
    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    texture.set(t);
    PSpriteBatch.PGdxSpriteBatch.staticBatch().begin();
    renderTextShader.start(PSpriteBatch.PGdxSpriteBatch.staticBatch().renderContext());
    float drawScale = 10;
    PSpriteBatch.PGdxSpriteBatch.staticBatch()
                        .draw(texture, 0, 512 * drawScale, PColor.WHITE, 512 * drawScale, 512 * drawScale, PColor.WHITE,
                              512 * drawScale, 0, PColor.WHITE, 0, 0, PColor.WHITE);
    PSpriteBatch.PGdxSpriteBatch.staticBatch().end();
    renderTextShader.end();
    testFontRenderBuffer.end();
    testFontRenderBuffer.emitPNG(Gdx.files.absolute("D:/testfont.png"), 0);
    PSDFSheet fontSDF = PSDFSheet.fromFileHandle(Gdx.files.local("engine/font/fontsdf." + PSDFSheet.FILE_EXTENSION));
    String fontName = "Dosis";
    PFont font = PFont.fromFileHandle(Gdx.files.local("engine/font/" + fontName + "." + PFont.FILE_EXTENSION));
    System.out.println(font);
  }
}
