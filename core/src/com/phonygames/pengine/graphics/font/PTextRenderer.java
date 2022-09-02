package com.phonygames.pengine.graphics.font;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PSpriteBatch;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PString;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Not poolable since it owns a shader and holds a reference to a renderBuffer.
 */
public class PTextRenderer {
  /** All text that has been rendered since the last clear(). */
  private final PString fullText = PString.obtain();
  private final PList<Glyph> glyphs = new PList<>(Glyph.staticPool);
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The offset that the next character should be placed at. X always increases, even for rtl. */ private final PVec2
      nextCharOffset = PVec2.obtain();
  private final PRenderBuffer renderBuffer;
  private final PShader textShader;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec2 xAxis = PVec2.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec2 yAxis = PVec2.obtain();
  /**
   * Whether or not to horizontally center text before flushing. Text will be centered within the text region width, or
   * the top corner (if the region width is set to -1)
   */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean centerTextHorizontalBeforeFlushing = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int charIndexInVisualLine;
  /** The size, which can be modified by the input text. */
  private float curFontSize;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PTextRendererDelegate delegate;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PFont font;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float fontSize;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** 0 for vertical, 1 for 45 degrees. */ private float italicsAmount;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private OverflowBehaviour overflowBehaviour;
  /** Right to left. */
  private boolean rtl = false;
  private boolean started = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The width of the region this text renderer is allowed to render in. Set to -1 for unlimited. */ private float
      textRegionWidth;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The top corner from which offsets are calculated. Can be on the left or right depending on rtl. */ private PVec2
      topCorner = PVec2.obtain();
  /** The index of the last laid out glyph that is in the glyph list (not yet flushed). */
  private int lastLaidOutGlyphIndex = -1;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int nextVisualLineNo = 0;

  public PTextRenderer(PRenderBuffer renderBuffer) {
    this.renderBuffer = renderBuffer;
    textShader = new PShader("#define pos2dFlag\n", renderBuffer.fragmentLayout(),
                             PSpriteBatch.PSDFSpriteBatch.genVertexAttributes(),
                             Gdx.files.local("engine/shader/sdf/sdf.vert.glsl"),
                             Gdx.files.local("engine/shader/sdf/sdf.frag.glsl"),
                             new String[]{renderBuffer.getTextureName(0)});
    reset();
  }

  public void reset() {
    textRegionWidth = -1;
    rtl = false;
    fontSize = 32;
    curFontSize = fontSize;
    delegate = null;
    font = null;
    centerTextHorizontalBeforeFlushing =false;
    xAxis.set(1, 0);
    yAxis.set(0, 1);
    italicsAmount = 0;
    topCorner.setZero();
    overflowBehaviour = OverflowBehaviour.None;
    lastLaidOutGlyphIndex = -1;
    clear();
  }

  /** Clears the buffered text. */
  public void clear() {
    nextCharOffset.setZero();
    fullText.clear();
    charIndexInVisualLine = 0;
    glyphs.clearAndFreePooled();
    nextVisualLineNo = 0;
  }

  /** Adds the glyphs from the text to the queue in a new line. */
  public PTextRenderer addText(String text) {
    if (text == null || text.length() == 0) {
      return this;
    }
    PAssert.isNotNull(font);
    int charIndex = fullText.length();
    int prevFullTextLength = fullText.length();
    fullText.append(text);
    /** Scale values from font size to the desired output size */
    float fromFontScale = fontSize / font.fontSize;
    int lastSpaceIndex = -1;
    int prevChar = fullText.length() == 0 ? 0 : fullText.charAt(fullText.length() - 1);
    for (int a = 0; a < text.length(); a++) {
      char c = text.charAt(a);
      boolean isAtEndOfWord = c == ' ' || a == text.length() - 1;
      boolean isAtEndOfLine = c == '\n' || a == text.length() - 1;
      /** Horizontal offset before this char was added. */
      float originalHorizontalOffset = nextCharOffset.x();
      // Special cases.
      switch (c) {
        case ' ':
          lastSpaceIndex = a;
          nextCharOffset.x(nextCharOffset.x() + font.spaceXAdvance * fromFontScale);
          break;
        case '\n':
          nextCharOffset.x(0);
          nextCharOffset.add(0, font.lineHeight() * fromFontScale);
          break;
        default:
          // Regular character; track the corresponding glyph and set its parameters.
          PFont.GlyphData glyphData = font.glyphData(c);
          Glyph glyph = genAndAddGlyph(glyphData, font.sdfSheet());
          glyph.charIndex = charIndex;
          glyph.fontSize = fontSize;
          glyph.textRenderer = this;
          glyph.xAxis.set(xAxis);
          glyph.yAxis.set(yAxis);
          glyph.italicsAmount = italicsAmount;
          glyph.charIndexInVisualLine = charIndexInVisualLine;
          glyph.visualLineNo = nextVisualLineNo;
          int fontScaleKerning = 0;
          // Apply kernings.
          if (glyphData != null) {
            if (charIndexInVisualLine > 0) {
              fontScaleKerning = font.getKerning(prevChar, c);
              if (fontScaleKerning != 0) {nextCharOffset.add(fromFontScale * fontScaleKerning, 0);}
            }
          }
          glyph.layoutOffset.set(nextCharOffset);
          nextCharOffset.add(fromFontScale * glyphData.xAdvance(), 0);
          break;
      }
      if (isAtEndOfWord) {
        // We are at the end of a word, so check to see if there is any overflowBehaviour stuff that needs to happen.
        switch (overflowBehaviour) {
          case None:
            break;
          case Wrap:
            PAssert.failNotImplemented("Wrap");
            break;
          case Ellipsis:
            PAssert.failNotImplemented("Ellipsis");
            break;
        }
      }
      if (isAtEndOfLine) {
        layoutLineAndMoveToNext(fromFontScale);
      } else {
        charIndexInVisualLine++;
      }
      prevChar = c;
      charIndex++;
    }
    return this;
  }

  /** Lays out the last queued glyphs, calls the delegate functions on any newly laid out glyphs. */
  private void layoutLineAndMoveToNext(float fromFontScale) {
    // Layout the glyphs up to this point. They should all be on the same line.
    int startGlyphIndex = lastLaidOutGlyphIndex + 1;
    float lineStartLayoutX = glyphs.get(startGlyphIndex).layoutOffset.x();
    float lineEndLayoutX = nextCharOffset.x();
    float lineLayoutWidth = lineEndLayoutX - lineStartLayoutX;
    PAssert.isTrue(lineLayoutWidth >= 0);
    if (centerTextHorizontalBeforeFlushing) {
      // Center the text.
      float lineLayoutCenter = (lineStartLayoutX + lineEndLayoutX) * .5f;
      float desiredLayoutCenter = textRegionWidth == -1 ? 0 : textRegionWidth * .5f;
      float layoutXDelta = desiredLayoutCenter - lineLayoutCenter;
      for (int a = startGlyphIndex; a < glyphs.size(); a++) {
        glyphs.get(a).layoutOffset.add(layoutXDelta, 0);
      }
    }
    // Call the delegate functions and lay out the glyphs.
    for (int a = startGlyphIndex; a < glyphs.size(); a++) {
      Glyph glyph = glyphs.get(a);
      glyph.setCapCornerFromLayout(topCorner);
      if (delegate != null) {
        delegate.processGlyphBeforeTextRendererFlush(glyph);
      }
      glyph.setMeshCornersFromSettings();
    }
    lastLaidOutGlyphIndex = glyphs.size() - 1;
    // Move to the next line.
    nextCharOffset.x(0);
    nextCharOffset.add(0, -font.lineHeight() * fromFontScale);
    // Prepare the char index and visual line for the next character.
    charIndexInVisualLine = 0;
    nextVisualLineNo ++;
  }

  /** Tracks another glyph. */
  private Glyph genAndAddGlyph(PFont.GlyphData glyphData, PSDFSheet sheet) {
    Glyph glyph = glyphs.genPooledAndAdd();
    glyph.glyphData = glyphData;
    glyph.sdfSheet = sheet;
    return glyph;
  }

  public void begin() {
    PAssert.isFalse(started);
    PSpriteBatch.PSDFSpriteBatch sdfSB = PSpriteBatch.PSDFSpriteBatch.staticBatch();
    renderBuffer.prepSpriteBatchForRender(sdfSB);
    sdfSB.begin();
    sdfSB.renderContext().setCullFaceDisabled();
    sdfSB.setAndStartShader(textShader);
    started = true;
  }

  public void end() {
    PAssert.isTrue(started);
    flush();
    clear();
    PSpriteBatch.PSDFSpriteBatch sdfSB = PSpriteBatch.PSDFSpriteBatch.staticBatch();
    sdfSB.end();
    started = false;
  }

  public PTextRenderer flush() {
    PSpriteBatch.PSDFSpriteBatch sdfSB = PSpriteBatch.PSDFSpriteBatch.staticBatch();
    // Flush all glyphs from the glyph list.
    PVec4 baseColor = PVec4.obtain().set(1, 1, 1, 1);
    PVec4 borderColor = PVec4.obtain().set(.8f, .8f, .8f, 1);
    float threshold = .5f;
    float borderThresholdInwardOffset = .2f;
    for (int a = 0; a < glyphs.size(); a++) {
      Glyph glyph = glyphs.get(a);
      PSDFSheet.Symbol sdfSymbol = glyph.getSymbol();
      sdfSB.draw(sdfSymbol, threshold, borderThresholdInwardOffset, glyph.meshCorner00.x(), glyph.meshCorner00.y(),
                 baseColor, borderColor, glyph.meshCorner10.x(), glyph.meshCorner10.y(), baseColor, borderColor,
                 glyph.meshCorner11.x(), glyph.meshCorner11.y(), baseColor, borderColor, glyph.meshCorner01.x(),
                 glyph.meshCorner01.y(), baseColor, borderColor);
    }
    sdfSB.enableBlending(false);
    sdfSB.flush();
    glyphs.clearAndFreePooled();
    lastLaidOutGlyphIndex = -1;
    baseColor.free();
    borderColor.free();
    return this;
  }

  enum OverflowBehaviour {
    Ellipsis, Wrap, None
  }

  public interface PTextRendererDelegate {
    public void processGlyphBeforeTextRendererFlush(Glyph glyph);
  }

  public static class Glyph implements PPool.Poolable {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    private static final PPool<Glyph> staticPool = new PPool<Glyph>() {
      @Override protected Glyph newObject() {
        return new Glyph();
      }
    };
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec4 baseColor = PVec4.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec4 borderColor = PVec4.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec2 capCorner = PVec2.obtain();
    /** Includes padding. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec2 meshCorner00 = PVec2.obtain();
    /** Includes padding. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec2 meshCorner01 = PVec2.obtain();
    /** Includes padding. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec2 meshCorner10 = PVec2.obtain();
    /** Includes padding. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec2 meshCorner11 = PVec2.obtain();
    private final PVec2 xAxis = PVec2.obtain();
    private final PVec2 yAxis = PVec2.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    PTextRenderer textRenderer;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private float borderSize = 0;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The index in the textRenderer string this glyph corresponds to. */ private int charIndex;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The index in the textRenderer line this glyph corresponds to. */ private int charIndexInVisualLine;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private float fontSize;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PFont.GlyphData glyphData = null;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** 0 for vertical, 1 for 45 degrees. */ private float italicsAmount;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PSDFSheet sdfSheet = null;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private boolean valid;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The charOffset that this glyph was laid out with. */
    private final PVec2 layoutOffset = PVec2.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** The line that this glyph appears to be in. */
    private int visualLineNo;

    /** Recalculates the cap corner using the provided xaxis, yaxis, layoutoffset, and top corner. */
    private void setCapCornerFromLayout(PVec2 topCorner) {

      capCorner.set(topCorner).add(xAxis, layoutOffset.x()).add(yAxis, layoutOffset.y());

    }

    @Override public void reset() {
      charIndex = 0;
      visualLineNo = 0;
      charIndexInVisualLine = 0;
      glyphData = null;
      sdfSheet = null;
      layoutOffset.setZero();
      capCorner.setZero();
      meshCorner00.setZero();
      meshCorner01.setZero();
      meshCorner10.setZero();
      meshCorner11.setZero();
      xAxis.set(1, 0);
      yAxis.set(0, 1);
      fontSize = 32;
      borderSize = 0;
      italicsAmount = 0;
      baseColor.set(1, 1, 1, 1);
      borderColor.set(1, 1, 1, 1);
      textRenderer = null;
      valid = false;
    }

    public Glyph setMeshCornersFromSettings() {
      PAssert.isNotNull(sdfSheet);
      if (glyphData == null) {
        valid = false;
        return this;
      }
      PSDFSheet.Symbol sdfSymbol = getSymbol();
      if (sdfSymbol == null) {
        valid = false;
        return this;
      }
      final float fromFontScale = fromFontScale();
      final float paddingOriginalScale = sdfSymbol.paddingOriginalScale();
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        // Get the baseline corners, as they are unchanged regardless of slant. Includes horizontal padding.
        PVec2 unSlantedBaselineCorner = pool.vec2(capCorner).add(xAxis, -fromFontScale * paddingOriginalScale)
                                            .add(yAxis, -glyphData.font().capHeight() * fromFontScale);
        PVec2 unSlantedBaselineEndCorner = pool.vec2(unSlantedBaselineCorner).add(xAxis, (glyphData.width() +
                                                                                          2 * paddingOriginalScale) *
                                                                                         fromFontScale);
        // Set the mesh corners for a vertically-sloped glyph.
        float baselineToPaddedGlyphTop = fromFontScale * (glyphData.baselineToTop() + paddingOriginalScale);
        float baselineToPaddedGlyphBottom = fromFontScale * (glyphData.baselineToBottom() - paddingOriginalScale);
        meshCorner00.set(unSlantedBaselineCorner).add(yAxis, baselineToPaddedGlyphBottom);
        meshCorner10.set(unSlantedBaselineEndCorner).add(yAxis, baselineToPaddedGlyphBottom);
        meshCorner01.set(unSlantedBaselineCorner).add(yAxis, baselineToPaddedGlyphTop);
        meshCorner11.set(unSlantedBaselineEndCorner).add(yAxis, baselineToPaddedGlyphTop);
        // Apply italics by shifting the corners horizontally.
        meshCorner00.add(xAxis, baselineToPaddedGlyphBottom * italicsAmount);
        meshCorner10.add(xAxis, baselineToPaddedGlyphBottom * italicsAmount);
        meshCorner01.add(xAxis, baselineToPaddedGlyphTop * italicsAmount);
        meshCorner11.add(xAxis, baselineToPaddedGlyphTop * italicsAmount);
      }
      valid = true;
      return this;
    }

    public PSDFSheet.Symbol getSymbol() {
      PAssert.isNotNull(glyphData);
      return sdfSheet.get(glyphData.sdfKey());
    }

    public float fromFontScale() {
      PAssert.isNotNull(glyphData);
      PAssert.isNotNull(glyphData.font());
      return fontSize / glyphData.font().fontSize;
    }
  }
}
