package com.phonygames.cybertag.world.grid.gen;

import com.phonygames.cybertag.world.ColorDataEmitter;
import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileRoom;
//import com.phonygames.cybertag.world.lasertag.LasertagRoomGen;
import com.phonygames.cybertag.world.lasertag.LasertagTile;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMeshGen;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelGenTemplate;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.val;

/** Helper class for generating tile rooms. */
public class TileRoomGen {
  /**
   * Run this when finished adding rooms and doors to the building. It finalizes the room model.
   */
  public static void onNeighborsAndDoorsReady(TileRoom room) {
    // Process the room.
    __runProcessorForRoom(room);
    // Emit models for the room.
    __genModelFor(room);
  }

  /** Processes the room after doors have been finalized. GridTile emitOptions should be set by this call. */
  private static void __runProcessorForRoom(TileRoom room) {
    room.parameters().processRoomAfterDoorsGenned(room);
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = room.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile = e.val();
        room.parameters().processTileOptionsAfterDoorsGenned(room,tile);
      }
    }
  }

  /** Generates the model for the room and sets vColIndices for gridTiles. */
  private static void __genModelFor(TileRoom room) {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      /** Map of vColIndex offsets used to shift vColIndices of modelGenTemplate part vertices. */
      private PStringMap<PInt> vColIndexOffsets = new PStringMap<>(PInt.getStaticPool());

      @Override protected void modelIntro() {
      }

      @Override protected void modelMiddle() {
        /** Loop through the tiles and emit them individually. */
        try (PPool.PoolBuffer pool = PPool.getBuffer()) {
          try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = room.tileGrid().obtainIterator()) {
            while (it.hasNext()) {
              PIntMap3d.Entry<GridTile> e = it.next();
              GridTile tile = e.val();
              __emitTileToModelGen(tile, this, vColIndexOffsets);
            }
          }
        }
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        PModel.Builder builder = new PModel.Builder();
        try ( PPooledIterable.PPoolableIterator<PMap.Entry<String, PMeshGen>> it = opaqueMeshGenMap.obtainIterator()){
          while (it.hasNext()) {
            PMap.Entry<String, PMeshGen> e = it.next();
            builder.chainGlNode(glNodes, e.k(), e.v().getMesh(),
                                new PMaterial(e.k(), null).useVColIndex(true), null, PGltf.Layer.PBR, true,
                                false);
          }
        }
        emitStaticPhysicsPartIntoModelBuilder(builder);
        emitStaticPhysicsPartIntoVerticesAndIndices(room.building().world().physicsVertexPositions, room.building().world().physicsVertexIndices);
        builder.addNode("base", null, glNodes, PMat4.IDT);
        for (int a = 0; a < alphaBlendParts.size(); a++) {
          PMeshGen part = alphaBlendParts.get(a);
          glNodes.clear();
          builder.chainGlNode(glNodes, part.name(), part.getMesh(), new PMaterial(part.name(), null).useVColIndex(true), null, PGltf.Layer.AlphaBlend,
                              true, true);
          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
        }
        room.setModelInstance(PModelInstance.obtain(builder.build()));
        room.unblockTaskTracker();
      }
    });
  }

  /** Emits the tile to the model gen using the gridTile options. */
  private static void __emitTileToModelGen(GridTile gridTile, PModelGen modelGen, PStringMap<PInt> vColIndexOffsets) {
    if (gridTile.emitOptions.floorModelTemplateID != null) {
      PModelGenTemplate floorTemplate = PAssetManager.model(gridTile.emitOptions.floorModelTemplateID, true).modelGenTemplate();
      floorTemplate.emit(modelGen, null, vColIndexOffsets );
    }
  }
}
