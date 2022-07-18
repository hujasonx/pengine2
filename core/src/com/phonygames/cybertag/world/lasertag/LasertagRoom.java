package com.phonygames.cybertag.world.lasertag;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagRoom implements PRenderContext.DataBufferEmitter {
  public final String id;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PList<LasertagDoor> doors = new PList<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PIntMap3d<LasertagTile> tiles = new PIntMap3d<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected LasertagBuilding building;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected ColorDataEmitter colorDataEmitter;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected boolean initialized = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
  /** Hallway rooms won't have walkways attached; instead, their floor might sloped. */
  protected boolean isHallway = false;
  /** The number of vCol indices dedicated to shared base colors, as opposed to per-tile colors. */
  protected int numBaseVCols = 16;
  private transient boolean roomColorsInitialized = false;

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
    if (colorDataEmitter != null && !roomColorsInitialized) {
      for (int a = 0; a < numBaseVCols; a++) {
        // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
        // the normal or the index with this buffer.
        colorDataEmitter.colorData[a * 2 + 0].setHSVA(MathUtils.random(), MathUtils.random(.1f, .3f),
                                                      MathUtils.random(.2f, .5f), 1); // DiffuseM;
        colorDataEmitter.colorData[a * 2 + 1].set(0, 0, 0, 1); // EmissiveR;
      }
      roomColorsInitialized = true;
    }
    try (val it = tiles().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        LasertagTile tile = e.val();
        tile.frameUpdate();
        if (colorDataEmitter != null) {
          colorDataEmitter.colorData[tile.tileVColIndexStart * 2 + 0].set(0, 0, 0, 1);
          colorDataEmitter.colorData[tile.tileVColIndexStart * 2 + 1].set((tile.x * .2f) % 1, (tile.y * .2f) % 1,
                                                                          (tile.z * .2f) % 1, 1);
        }
      }
    }
  }

  public void logicUpdate() {
    if (!initialized) {return;}
    try (val it = tiles().obtainIterator()) {
      while (it.hasNext()) {
        val tile = it.next();
        tile.val().logicUpdate();
      }
    }
  }

  public void render(PRenderContext renderContext) {
    if (!initialized) {return;}
    try (val it = tiles().obtainIterator()) {
      while (it.hasNext()) {
        val tile = it.next();
        tile.val().render(renderContext);
      }
    }
    if (modelInstance != null) {
      modelInstance.setDataBufferEmitter(this);
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
