package com.phonygames.cybertag.world.gen;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.Duple;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import lombok.val;

public class LasertagWorldGenRoom {
  private final LasertagWorldGenBuilding building;
  private final transient PList<Duple<PMesh, Boolean>> doorframeTemplateData = new PList<>();
  // Should be a wall with a window, with multiple parts.
  // private String windowModel = "model/template/window/basic.glb";
  private final transient PList<Duple<PMesh, Boolean>> floorTemplateData = new PList<>();
  private final int index, roomX, roomY, roomZ, roomSizeX, roomSizeY, roomSizeZ;
  private final PList<WallData> wallData = new PList<>();
  private final transient PList<Duple<PMesh, Boolean>> wallTemplateData = new PList<>();
  private String doorframeModel = "model/template/doorframe/basic.glb";
  private String floorModel = "model/template/floor/basic.glb";
  private String wallModel = "model/template/wall/basic.glb";

  protected LasertagWorldGenRoom(@NonNull LasertagWorldGenBuilding building, int index, int roomX, int roomY, int roomZ,
                                 int roomSizeX, int roomSizeY, int roomSizeZ) {
    this.building = building;
    this.index = index;
    this.roomX = roomX;
    this.roomY = roomY;
    this.roomZ = roomZ;
    this.roomSizeX = roomSizeX;
    this.roomSizeY = roomSizeY;
    this.roomSizeZ = roomSizeZ;
  }

  public void addToBuilding() {
    building.rooms.add(this);
    for (int x = roomX; x < roomX + roomSizeX; x++) {
      for (int y = roomX; y < roomY + roomSizeY; y++) {
        for (int z = roomZ; z < roomZ + roomSizeZ; z++) {
          LasertagWorldGenRoom roomAtTile = building.roomTiles[x][y][z];
          if (roomAtTile == null) { // Don't replace existing room tiles.
            building.roomTiles[x][y][z] = this;
          }
        }
      }
    }
  }

  protected void emit(PModelGen modelGen, LasertagWorldGen.Context context) {
    // Init parts and templates.
    PModelGen.Part basePart = modelGen.addPart("room" + index + "Part", PVertexAttributes.getGLTF_UNSKINNED());
    PModelGen.StaticPhysicsPart basePhysicsPart = modelGen.addStaticPhysicsPart("room" + index + "StaticPhysicsPart");
    context.modelgenParts.add(
        new Duple<>(PVec3.obtain().set(roomX + roomSizeX / 2, roomY + roomSizeY / 2, roomZ + roomSizeZ / 2), basePart));
    context.modelgenStaticPhysicsParts.add(basePhysicsPart);
    initTemplateData();
    // Set up the vertex processor and temp variables.
    PModelGen.Part.VertexProcessor vertexProcessor = PModelGen.Part.VertexProcessor.staticPool().obtain();
    PPool.PoolBuffer pool = PPool.getBuffer();
    PMat4 emitTransform = pool.mat4();
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile001 = pool.vec3();
    PVec3 tile110 = pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    vertexProcessor.setTransform(emitTransform);
    // Loop through walldata and emit the walls.
    for (WallData data : wallData) {
      for (int a = 0; a < data.tileTypes.size; a++) {
        int x = data.tileXs.get(a);
        int y = data.baseY;
        int z = data.tileZs.get(a);
        WallData.TileType tileType = data.tileTypes.get(a);
        float heightL = data.cornerHeightsL.get(a);
        float heightH = data.cornerHeightsH.get(a);
        building.getRoomTilePhysicalBounds(x, y, z, tile000, tile100, tile010, tile001, tile110, tile101, tile011,
                                           tile111);
        PVec3 first = null, second = null;
        float firstHeight = 0, secondHeight = 0;
        switch (data.side) {
          case Xl:
            first = tile000;
            second = tile001;
            firstHeight = tile010.y() - first.y();
            secondHeight = tile011.y() - second.y();
            break;
          case Zh:
            first = tile001;
            second = tile101;
            firstHeight = tile011.y() - first.y();
            secondHeight = tile111.y() - second.y();
            break;
          case Xh:
            first = tile101;
            second = tile100;
            firstHeight = tile111.y() - first.y();
            secondHeight = tile110.y() - second.y();
            break;
          case Zl:
            first = tile100;
            second = tile000;
            firstHeight = tile110.y() - first.y();
            secondHeight = tile010.y() - second.y();
            break;
        }
        vertexProcessor.setWall(first.x(), first.y(), first.z(), firstHeight * heightL, second.x(), second.y(),
                                second.z(), secondHeight * heightH);
        emitTemplate(basePart, "room" + index + "AlphaBlendPartZl", basePhysicsPart, context, wallTemplateData,
                     vertexProcessor);
      }
    }
    // TODO: remove this.
    for (int x = roomX; x < roomX + roomSizeX; x++) {
      for (int y = roomX; y < roomY + roomSizeY; y++) {
        for (int z = roomZ; z < roomZ + roomSizeZ; z++) {
          LasertagWorldGenRoom roomAtTile = building.roomTiles[x][y][z];
          if (roomAtTile != this) {continue;}
          LasertagWorldGenRoom roomXl = building.roomAtTile(x - 1, y, z);
          LasertagWorldGenRoom roomXh = building.roomAtTile(x + 1, y, z);
          LasertagWorldGenRoom roomZl = building.roomAtTile(x, y, z + 1);
          LasertagWorldGenRoom roomZh = building.roomAtTile(x, y, z - 1);
          LasertagWorldGenRoom roomYl = building.roomAtTile(x, y - 1, z);
          LasertagWorldGenRoom roomYh = building.roomAtTile(x, y + 1, z);
          building.getRoomTilePhysicalBounds(x, y, z, tile000, tile100, tile010, tile001, tile110, tile101, tile011,
                                             tile111);
          if (roomYl != this) {
            vertexProcessor.setFlatQuad(tile000, tile100, tile101, tile001);
            // We need to create a floor.
            emitTemplate(basePart, "room" + index + "AlphaBlendPartYl", basePhysicsPart, context, floorTemplateData,
                         vertexProcessor);
          }
          if (roomYh != this) {
            vertexProcessor.setFlatQuad(tile010, tile110, tile111, tile011);
            // We need to create a ceiling.
            emitTemplate(basePart, "room" + index + "AlphaBlendPartYh", basePhysicsPart, context, floorTemplateData,
                         vertexProcessor);
          }
        }
      }
    }
    pool.finish();
    PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
  }

  private void initTemplateData() {
    PModel floorPModel = PAssetManager.model(floorModel, true);
    floorTemplateData.clear();
    for (val e : floorPModel.glNodes()) {
      floorTemplateData.add(
          new Duple<>(e.v().drawCall().mesh(), e.v().drawCall().material().id().contains(".alsoStaticBody")));
    }
    PModel doorframePModel = PAssetManager.model(doorframeModel, true);
    doorframeTemplateData.clear();
    for (val e : doorframePModel.glNodes()) {
      doorframeTemplateData.add(
          new Duple<>(e.v().drawCall().mesh(), e.v().drawCall().material().id().contains(".alsoStaticBody")));
    }
    PModel wallPModel = PAssetManager.model(wallModel, true);
    wallTemplateData.clear();
    for (val e : wallPModel.glNodes()) {
      wallTemplateData.add(
          new Duple<>(e.v().drawCall().mesh(), e.v().drawCall().material().id().contains(".alsoStaticBody")));
    }
  }

  protected void emitTemplate(@NonNull PModelGen.Part basePart, @Nullable String alphaBlendPartName,
                              @Nullable PModelGen.StaticPhysicsPart staticPhysicsPart,
                              @NonNull LasertagWorldGen.Context context, @NonNull PList<Duple<PMesh, Boolean>> template,
                              @NonNull PModelGen.Part.VertexProcessor vertexProcessor) {
    for (val e : template) {
      PMesh mesh = e.getKey();
      boolean alsoStaticBody = e.getValue();
      basePart.emit(mesh, (alsoStaticBody && staticPhysicsPart != null) ? staticPhysicsPart : null, vertexProcessor,
                    basePart.vertexAttributes());
    }
  }

  protected void generateRoomData() {
    generateWallData();
    generateWalkwayData();
    generatePropsData();
    generateFloorData();
  }

  private void generateWallData() {
    for (int x = roomX; x < roomX + roomSizeX; x++) {
      for (int y = roomX; y < roomY + roomSizeY; y++) {
        for (int z = roomZ; z < roomZ + roomSizeZ; z++) {
          if (building.roomAtTile(x, y, z) != this) {continue;}
          LasertagWorldGenRoom roomXl = building.roomAtTile(x - 1, y, z);
          LasertagWorldGenRoom roomXh = building.roomAtTile(x + 1, y, z);
          LasertagWorldGenRoom roomZl = building.roomAtTile(x, y, z - 1);
          LasertagWorldGenRoom roomZh = building.roomAtTile(x, y, z + 1);
          LasertagWorldGenRoom roomXlZl = building.roomAtTile(x - 1, y, z - 1);
          LasertagWorldGenRoom roomXhZl = building.roomAtTile(x + 1, y, z - 1);
          LasertagWorldGenRoom roomXlZh = building.roomAtTile(x - 1, y, z + 1);
          if (roomXl != this) {
            if (roomZl != this) {
              // Create concave walls - both +x and +z directions.
              wallData.add(new WallData(this, WallData.Side.Xl, x, y, z));
              wallData.add(new WallData(this, WallData.Side.Zl, x, y, z));
            } else if (roomXlZl == this) {
              // Create convex wall - only in the +z direction.
              wallData.add(new WallData(this, WallData.Side.Xl, x, y, z));
            }
          } else if (roomZl != this && roomXlZl == this) {
            // Create convex wall - only in the +x direction.
            wallData.add(new WallData(this, WallData.Side.Zl, x, y, z));
          }
          if (roomXh != this) {
            if (roomZl != this || roomXhZl == this) {
              // Create a wall in the +z direction.
              wallData.add(new WallData(this, WallData.Side.Xh, x, y, z));
            }
          }
          if (roomZh != this) {
            if (roomXl != this || roomXlZh == this) {
              // Create a wall in the +x direction.
              wallData.add(new WallData(this, WallData.Side.Zh, x, y, z));
            }
          }
        }
      }
    }
    for (WallData data : wallData) {
      data.processFromTiles();
    }
  }

  private void generateWalkwayData() {
  }

  private void generatePropsData() {
  }

  private void generateFloorData() {
  }

  /**
   * In this sense, "encroachment" refers to the tiles of this room after we account the fact that it cannot overwrite
   * existing tiles in the roomTiles array.
   * @param roomTiles
   * @param minimumRoomSizeAfterEnchroachment
   * @return
   */
  protected boolean isValidWithEncroachment(LasertagWorldGenRoom[][][] roomTiles, int minimumRoomSizeAfterEnchroachment,
                                            float minimumKeptTilesRatio) {
    // Go through all the rows and columns of this room, ensuring that there are no blocks of tiles that this room
    // would own that are too small in combined length.
    // First, x-rows.
    int keptTiles = 0;
    for (int y = roomX; y < roomY + roomSizeY; y++) {
      for (int z = roomZ; z < roomZ + roomSizeZ; z++) {
        int consecutiveFreeSpots = 0;
        for (int x = roomX; x < roomX + roomSizeX; x++) {
          if (roomTiles[x][y][z] == null) {
            consecutiveFreeSpots++;
            keptTiles++;
          } else {
            if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minimumRoomSizeAfterEnchroachment) {
              return false;
            }
            consecutiveFreeSpots = 0;
          }
        }
        if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minimumRoomSizeAfterEnchroachment) {
          return false;
        }
      }
    }
    if (((float) keptTiles) / ((float) (roomSizeX * roomSizeY * roomSizeZ)) < minimumKeptTilesRatio) {
      return false;
    }
    // Then, z-rows.
    for (int y = roomX; y < roomY + roomSizeY; y++) {
      for (int x = roomX; x < roomX + roomSizeX; x++) {
        int consecutiveFreeSpots = 0;
        for (int z = roomZ; z < roomZ + roomSizeZ; z++) {
          if (roomTiles[x][y][z] == null) {
            consecutiveFreeSpots++;
          } else {
            if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minimumRoomSizeAfterEnchroachment) {
              return false;
            }
            consecutiveFreeSpots = 0;
          }
        }
        if (consecutiveFreeSpots > 0 && consecutiveFreeSpots < minimumRoomSizeAfterEnchroachment) {
          return false;
        }
      }
    }
    return true;
  }

  private static class WallData {
    private final int baseX, baseY, baseZ;
    private final PList<Float> cornerHeightsH = new PList<>();
    // Always traverse in the +x or +z direction.
    private final PList<Float> cornerHeightsL = new PList<>();
    private final LasertagWorldGenRoom room;
    private final Side side;
    private final PList<TileType> tileTypes = new PList<>();
    private final PList<Integer> tileXs = new PList<>();
    private final PList<Integer> tileZs = new PList<>();

    private WallData(LasertagWorldGenRoom room, Side side, int baseX, int baseY, int baseZ) {
      this.room = room;
      this.side = side;
      this.baseX = baseX;
      this.baseY = baseY;
      this.baseZ = baseZ;
    }

    private final boolean affectsTile(int x, int y, int z) {
      if (y != baseY) {
        return false;
      }
      switch (side) {
        case Xl:
        case Xh:
          // Moving in the +z direction.
          return x == baseX && z >= baseZ && z < baseZ + tileTypes.size;
        case Zl:
        case Zh:
          // Moving in the +x direction.
          return z == baseZ && x >= baseX && x < baseX + tileTypes.size;
        default:
          PAssert.fail("WTF");
          return false;
      }
    }

    /**
     * Fills the data buffers.
     */
    private void processFromTiles() {
      PAssert.isTrue(tileTypes.isEmpty());
      for (int a = 0; ; a++) {
        int x = baseX;
        int y = baseY;
        int z = baseZ;
        switch (side) {
          case Xl:
          case Xh:
            // Moving in the +z direction.
            z += a;
            break;
          case Zl:
          case Zh:
            // Moving in the +x direction.
            x += a;
            break;
        }
        if (room.building.roomAtTile(x, y, z) != room) {
          return;
        }
        tileTypes.add(TileType.NORMAL); // TODO: doorways and windows and parapets.
        cornerHeightsL.add(1f);
        cornerHeightsH.add(1f);
        tileXs.add(x);
        tileZs.add(z);
      }
    }

    enum Side {
      Xl, Xh, Zl, Zh
    }

    enum TileType {
      NORMAL, WINDOW, DOORFRAME
    }
  }
}
