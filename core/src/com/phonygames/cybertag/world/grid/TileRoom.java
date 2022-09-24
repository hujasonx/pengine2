package com.phonygames.cybertag.world.grid;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PVColIndexBuffer;
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
  /** The tiles in this room. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final TileGrid tileGrid = new TileGrid();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** Whether or not the room should have an open ceiling. */
  private final boolean hasOpenCeiling;
  /** The vcol index buffer. */
  private final PVColIndexBuffer vColors = new PVColIndexBuffer();


  /** The model instance. */
  private PModelInstance modelInstance;

  // TODO: This is temporary, figure out whre to put this.
  private void initColors() {

    vColors.registerName("skin", 0);
    vColors.setDiffuse("skin", 1, 1, 1);
  }

  @Override public void emitDataBuffersInto(PRenderContext renderContext) {
    vColors.emitColorData(renderContext);
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
