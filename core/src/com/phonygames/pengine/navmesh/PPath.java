package com.phonygames.pengine.navmesh;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PParametricCurve;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PPath implements PPool.Poolable {
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;

  private static final PPool<PPath> staticPool = new PPool<PPath>() {
    @Override protected PPath newObject() {
      return new PPath();
    }
  };

  protected static PPath obtain() {
    return staticPool.obtain();
  }

  private PPath() {}

  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PParametricCurve.PParametricCurve3 backingCurve = PParametricCurve.obtain3();
  @Override public void reset() {
    backingCurve.reset();
  }
}
