package com.phonygames.cybertag.world.lasertag;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

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
  /** Hallway rooms won't have walkways attached; instead, their floor might sloped. */
  protected boolean isHallway = false;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;
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
        if (isHallway) {
          // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
          // the normal or the index with this buffer.
          colorDataEmitter.colorData[a * 2 + 0].setHSVA(.01f, .01f, .01f, 1); // DiffuseM;
        } else {
          // Note, we use emissiveR, but the shader will output emissiveI and normalR. But we don't want to edit
          // the normal or the index with this buffer.
          colorDataEmitter.colorData[a * 2 + 0].setHSVA(MathUtils.random(), MathUtils.random(.1f, .3f),
                                                        MathUtils.random(.2f, .5f), 1); // DiffuseM;
        }
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
          if (isHallway) {
            colorDataEmitter.colorData[tile.tileVColIndexStart * 2 + 1].setHSVA(id.hashCode() * .3732f, 1, .4f,
                                                                                1); // EmissiveR;
          } else {
            colorDataEmitter.colorData[tile.tileVColIndexStart * 2 + 1].set((tile.x * .2f) % 1, (tile.y * .2f) % 1,
                                                                            (tile.z * .2f) % 1, 1);
          }
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
    if (isHallway) {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        try (val it = tiles().obtainIterator()){
          while (it.hasNext()) {
            val e = it.next();
            if (!e.val().hasFloor) { continue; }
            PVec3 c000 = pool.vec3(), c001 = pool.vec3();
            PVec3 c010 = pool.vec3(), c011 = pool.vec3();
            PVec3 c110 = pool.vec3(), c111 = pool.vec3();
            PVec3 c100 = pool.vec3(), c101 = pool.vec3();
            e.val().getCornersFloorCeiling(c000,c001,c010,c011,c100,c101,c110,c111);
//            PDebugRenderer.line(c000, c001, PVec4.ONE, 2);
//            PDebugRenderer.line(c001, c101, PVec4.ONE, 2);
//            PDebugRenderer.line(c101, c100, PVec4.ONE, 2);
//            PDebugRenderer.line(c100, c000, PVec4.ONE, 2);
          }
        }
      }
    }
  }
}
