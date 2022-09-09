package com.phonygames.cybertag.world.grid;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A room in a building that uses a tile grid system. */
@Builder
public class TileRoom  implements PRenderContext.DataBufferEmitter{
  /** The room's type. */
  private final String type;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The tiles in this room. */
  private static final TileGrid tileGrid = new TileGrid();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** Whether or not the room should have an open ceiling. */
  private final boolean hasOpenCeiling;


  /** The model instance. */
  private PModelInstance modelInstance;

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    PAssert.failNotImplemented("emitDataBuffersInto"); // TODO: FIXME
  }

  public void frameUpdate() {

  }

  public void logicUpdate() {

  }

  public void render(PRenderContext renderContext) {
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
