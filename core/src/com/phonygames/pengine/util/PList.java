package com.phonygames.pengine.util;

import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.math.PNumberUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * List class whose iterator() does not allocate and will always be in order.
 */
public class PList<E> extends Array<E> implements PPool.Poolable {
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;

  public PList() {
  }

  public E removeLast() {
    if (size == 0) {
      return null;
    }
    return removeIndex(size - 1);
  }

  @Override public E get(int index) {
    return super.get(PNumberUtils.mod(index, size));
  }

  @Override public void reset() {
    clear();
  }
}
