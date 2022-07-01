package com.phonygames.cybertag.world.lasertag;

import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.PIntMap3d;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagRoom implements PRenderContext.DataBufferEmitter {
  public final String id;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PIntMap3d<LasertagTile> tiles = new PIntMap3d<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected LasertagBuilding building;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected boolean initialized = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected ColorDataEmitter colorDataEmitter;

  protected LasertagRoom(String id) {
    this.id = id;
  }

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    if (colorDataEmitter != null) {
      colorDataEmitter.outputColorData(renderContext.genDataBuffer("vColIndex"));
    }
  }

  public void frameUpdate() {
    if (!initialized) {return;}
    if (colorDataEmitter != null) {
      colorDataEmitter.frameUpdateColorData();
    }
    for (val tile : tiles().iterator3d()) {
      tile.val().frameUpdate();
    }
  }

  public void logicUpdate() {
    if (!initialized) {return;}
    for (val tile : tiles().iterator3d()) {
      tile.val().logicUpdate();
    }
  }

  public void render(PRenderContext renderContext) {
    if (!initialized) {return;}
    for (val tile : tiles().iterator3d()) {
      tile.val().render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
