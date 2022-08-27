package com.phonygames.pengine.util.collection;

import com.phonygames.pengine.util.PPool;

import lombok.Getter;
import lombok.Setter;

public class PStringMap<V> extends PMap<String, V> implements PPool.Poolable {
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;

  public PStringMap(PPool pool) {
    super(pool);
  }

  public PStringMap() {
    super();
  }
}
