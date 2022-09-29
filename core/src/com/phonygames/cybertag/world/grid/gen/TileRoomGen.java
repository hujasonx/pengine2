package com.phonygames.cybertag.world.grid.gen;

import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.grid.GridTile;
import com.phonygames.cybertag.world.grid.TileRoom;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMeshGen;
import com.phonygames.pengine.graphics.model.PMeshGenVertexProcessor;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PModelGenTemplate;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PFacing;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.collection.PIntMap3d;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;
import com.phonygames.pengine.util.collection.PStringMap;

/** Helper class for generating tile rooms. */
public class TileRoomGen {
  /** Vertex processor for flatQuads. */
  private static PMeshGenVertexProcessor.FlatQuad __flatQuadVP = new PMeshGenVertexProcessor.FlatQuad();
  /** Vertex processor for transforms. */
  private static PMeshGenVertexProcessor.Transform __transformVP = new PMeshGenVertexProcessor.Transform();
  /** The offset of the floors and walkways, used to prevent walls and ceilings from clipping through. World space. */
  private static float floorAndWalkwayVerticalOffset = .025f;
  /** The size of a tile on a horizontal axis. */
  private static float tileScaleXZ = 3f;
  /** The size of a tile on a vertical axis. */
  private static float tileScaleY = 3f;

  /**
   * Run this when finished adding rooms and doors to the building. It finalizes the room model.
   */
  public static void onNeighborsAndDoorsReady(TileRoom room, @Nullable Runnable runOnModelGenned) {
    // Process the room.
    __runTileProcessorForRoom(room);
    // Emit models for the room.
    __genModelFor(room, runOnModelGenned);
  }

  /** Processes the room after doors have been finalized. GridTile emitOptions should be set by this call. */
  private static void __runTileProcessorForRoom(TileRoom room) {
    room.parameters().processRoomAfterDoorsGenned(room);
    try (PPooledIterable.PPoolableIterator<PIntMap3d.Entry<GridTile>> it = room.tileGrid().obtainIterator()) {
      while (it.hasNext()) {
        PIntMap3d.Entry<GridTile> e = it.next();
        GridTile tile = e.val();
        room.parameters().processTileOptionsAfterDoorsGenned(room, tile);
      }
    }
  }

  /** Generates the model for the room and sets vColIndices for gridTiles. */
  private static void __genModelFor(TileRoom room, @Nullable final Runnable runOnModelGenned) {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      /** Map of vColIndex offsets used to shift vColIndices of modelGenTemplate part vertices. */
      private PStringMap<PInt> vColIndexOffsets = new PStringMap<>(PInt.getStaticPool());

      @Override protected void modelIntro() {
        vColIndexOffsets.genPooled("base").set(0);
        // Tile colors start at the end of the base colors.
        vColIndexOffsets.genPooled("tile").set(TileRoom.NUM_BASE_VCOLS);
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
        try (PPooledIterable.PPoolableIterator<PMap.Entry<String, PMeshGen>> it = meshGenMap.obtainIterator()) {
          while (it.hasNext()) {
            glNodes.clear();
            PMap.Entry<String, PMeshGen> e = it.next();
            boolean alphaBlend = e.v().alphaBlend();
            if (alphaBlend) {
              System.out.println();
            }
            builder.chainGlNode(glNodes, e.k(), e.v().getMesh(), new PMaterial(e.k(), null), null,
                                alphaBlend ? PGltf.Layer.AlphaBlend : PGltf.Layer.PBR, true,
                                alphaBlend);
            builder.addNode(e.k(), null, glNodes, PMat4.IDT);
          }
        }
        emitStaticPhysicsPartIntoModelBuilder(builder);
        emitStaticPhysicsPartIntoVerticesAndIndices(room.building().world().physicsVertexPositions,
                                                    room.building().world().physicsVertexIndices);
        //        builder.addNode("base", null, glNodes, PMat4.IDT);
        //        for (int a = 0; a < alphaBlendParts.size(); a++) {
        //          PMeshGen part = alphaBlendParts.get(a);
        //          glNodes.clear();
        //          builder.chainGlNode(glNodes, part.name(), part.getMesh(), new PMaterial(part.name(), null), null,
        //          PGltf.Layer.AlphaBlend,
        //                              true, true);
        //          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
        //        }
        /** Ensure the vColor array for the room is filled for all tiles. */
        room.vColors().ensureCapacity(vColIndexOffsets.genPooled("tile").valueOf());
        room.setModelInstance(PModelInstance.obtain(builder.build()));
        if (runOnModelGenned != null) {
          runOnModelGenned.run();
        }
        room.unblockTaskTracker();
      }
    });
  }
  /** Vertical offset for floors and walkways, to prevent visible clipping of the wall tops through the ground. */
  /** Emits the tile to the model gen using the gridTile options. */
  private static void __emitTileToModelGen(GridTile gridTile, PModelGen modelGen, PStringMap<PInt> vColIndexOffsets) {
    int tileVColOffset = vColIndexOffsets.genPooled("tile").valueOf();
    gridTile.vColIndexOffset(tileVColOffset);
    // Emit floor.
    if (gridTile.emitOptions.floorModelTemplateID != null) {
      PModelGenTemplate floorTemplate =
          PAssetManager.model(gridTile.emitOptions.floorModelTemplateID, true).modelGenTemplate();
      __transformVP.transform()
                   .setToTranslation(gridTile.x * tileScaleXZ, floorAndWalkwayVerticalOffset + gridTile.y * tileScaleY,
                                     gridTile.z * tileScaleXZ).scl(tileScaleXZ, tileScaleY, tileScaleXZ);
      floorTemplate.emit(modelGen, __transformVP, vColIndexOffsets);
    }
    // Emit walkway.
    if (gridTile.emitOptions.walkwayModelTemplateID != null) {
      PModelGenTemplate walkwayTemplate =
          PAssetManager.model(gridTile.emitOptions.walkwayModelTemplateID, true).modelGenTemplate();
      gridTile.worldSpaceCorners(tileScaleXZ, tileScaleY, tileScaleXZ, __flatQuadVP.flatQuad00(),
                                 __flatQuadVP.flatQuad10(), __flatQuadVP.flatQuad11(), __flatQuadVP.flatQuad01(), null,
                                 null, null, null);
      __flatQuadVP.flatQuad00().add(0, floorAndWalkwayVerticalOffset +
                                       tileScaleY * gridTile.emitOptions.walkwayCornerVerticalOffsets.x(), 0);
      __flatQuadVP.flatQuad10().add(0, floorAndWalkwayVerticalOffset +
                                       tileScaleY * gridTile.emitOptions.walkwayCornerVerticalOffsets.y(), 0);
      __flatQuadVP.flatQuad11().add(0, floorAndWalkwayVerticalOffset +
                                       tileScaleY * gridTile.emitOptions.walkwayCornerVerticalOffsets.z(), 0);
      __flatQuadVP.flatQuad01().add(0, floorAndWalkwayVerticalOffset +
                                       tileScaleY * gridTile.emitOptions.walkwayCornerVerticalOffsets.w(), 0);
      walkwayTemplate.emit(modelGen, __flatQuadVP, vColIndexOffsets);
    }
    // Emit walls.
    for (int a = 0; a < PFacing.count(); a++) {
      GridTile.EmitOptions.Wall wall = gridTile.emitOptions.walls[a];
      for (int b = 0; b < wall.wallModelTemplateIDs.size(); b++) {
        String wallModelTemplateID = wall.wallModelTemplateIDs.get(b);
        PModelGenTemplate wallTemplate = PAssetManager.model(wallModelTemplateID, true).modelGenTemplate();
        // Rotate wall if the facing is not -X (the default facing).
        int modelOriginX = gridTile.x + ((wall.facing == PFacing.mZ || wall.facing == PFacing.X) ? 1 : 0);
        int modelOriginZ = gridTile.z + ((wall.facing == PFacing.X || wall.facing == PFacing.Z) ? 1 : 0);
        float rotationRad = MathUtils.PI * (1f - .5f * wall.facing.intValue());
        __transformVP.transform()
                     .setToTranslation(modelOriginX * tileScaleXZ, gridTile.y * tileScaleY, modelOriginZ * tileScaleXZ)
                     .rotate(0, 1, 0, rotationRad).scl(tileScaleXZ, tileScaleY, tileScaleXZ);
        wallTemplate.emit(modelGen, __transformVP, vColIndexOffsets);
      }
    }
    // TODO: instead of hard coding 8, calculate how many colors are actually needed per tile.
    vColIndexOffsets.genPooled("tile").set(tileVColOffset + 8);
  }
}
