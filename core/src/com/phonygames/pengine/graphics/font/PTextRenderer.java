package com.phonygames.pengine.graphics.font;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.sdf.PSDFSheet;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PTextRenderer implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PTextRenderer> staticPool = new PPool<PTextRenderer>() {
    @Override protected PTextRenderer newObject() {
      return new PTextRenderer();
    }
  };
  /** The size, which can be modified by the input text. */
  private float curSize;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private PTextRendererDelegate delegate;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private float leftX, rightX, topY, bottomY, size;
  private boolean started = false;

  private PTextRenderer() {
    reset();
  }

  @Override public void reset() {
    leftX = 0;
    rightX = 0;
    topY = 0;
    bottomY = 0;
    size = 32;
    curSize = size;
    delegate = null;
    PAssert.failNotImplemented("reset"); // TODO: FIXME
  }

  public static PTextRenderer obtain() {
    return staticPool.obtain();
  }

  public void begin() {
    PAssert.isFalse(started);
    started = true;
  }

  public void end() {
    PAssert.isTrue(started);
    started = false;
  }

  public interface PTextRendererDelegate {}

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
    private final PVec2 baselineCorner = PVec2.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec4 borderColor = PVec4.obtain();
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
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private float borderSize = 0;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PFont.GlyphData glyphData = null;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private PSDFSheet sdfSheet = null;
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    /** 0 for vertical, 1 for 45 degrees. */ private float italicsAmount;

    private static Glyph obtain(PFont.GlyphData glyphData, PSDFSheet sheet) {
      Glyph glyph = staticPool.obtain();
      glyph.glyphData = glyphData;
      glyph.sdfSheet = sheet;
      return glyph;
    }

    @Override public void reset() {
      glyphData = null;
      sdfSheet = null;
      baselineCorner.setZero();
      meshCorner00.setZero();
      meshCorner01.setZero();
      meshCorner10.setZero();
      meshCorner11.setZero();
      borderSize = 0;
      italicsAmount = 0;
      baseColor.set(1, 1, 1, 1);
      borderColor.set(1, 1, 1, 1);
    }

    public Glyph setMeshCornersFromSettings() {
      PAssert.isNotNull(glyphData);
      PAssert.isNotNull(sdfSheet);
      // Drawing is done at the top of the cap.
//      final int cap = drawY;
//      final int baseline = cap - (int) font.getCapHeight();
//      final int glyphLeft = drawX;
//      final int glyphRight = glyphLeft + glyph.width;
//      final int glyphBottom = cap + glyph.yoffset + (int) font.getAscent();
//      final int glyphTop = glyphBottom + glyph.height;
//      final int descent = baseline + (int) font.getDescent();
//      final int advancedX = glyphLeft + glyph.xadvance;
      float baseline = baselineCorner.y();
      meshCorner00.set(baselineCorner);
      return this;
    }
  }
}
