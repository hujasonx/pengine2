package com.phonygames.pengine.graphics.font;

import com.phonygames.pengine.exception.PAssert;
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

  public static class PTextRendererGlyphSettings {}
}
