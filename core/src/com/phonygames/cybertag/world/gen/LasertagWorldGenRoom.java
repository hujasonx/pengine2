package com.phonygames.cybertag.world.gen;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.PAssetManager;
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
  private final transient PList<Duple<PMesh, Boolean>> wallTemplateData = new PList<>();
  private String doorframeModel = "model/template/doorframe/basic.glb";
  private String floorModel = "model/template/floor/basic.glb";
  private String wallModel = "model/template/wall/basic.glb";

  protected LasertagWorldGenRoom(@NonNull LasertagWorldGenBuilding building, int index, int roomX, int roomY, int roomZ, int roomSizeX,
               int roomSizeY, int roomSizeZ) {
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
    PModelGen.Part basePart = modelGen.addPart("room" + index + "Part", PVertexAttributes.getGLTF_UNSKINNED());
    PModelGen.StaticPhysicsPart basePhysicsPart = modelGen.addStaticPhysicsPart("room" + index + "StaticPhysicsPart");
    context.modelgenParts.add(
        new Duple<>(PVec3.obtain().set(roomX + roomSizeX / 2, roomY + roomSizeY / 2, roomZ + roomSizeZ / 2), basePart));
    context.modelgenStaticPhysicsParts.add(basePhysicsPart);
    // Now that we've obtained the parts to work with, get the template data we want.
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
    // Set up the vertex processor and temp variables.
    PModelGen.Part.VertexProcessor vertexProcessor = PModelGen.Part.VertexProcessor.staticPool().obtain();
    PPool.PoolBuffer pool = PPool.getBuffer();
    PMat4 emitTransform = pool.mat4();
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile001 = pool.vec3();
    PVec3 tile110 = pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    vertexProcessor.setTransform(emitTransform);
    // Finally, loop through the room tiles.
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
          if (roomXl != this) {
            vertexProcessor.setWall(tile000.x(), tile000.y(), tile000.z(), tile010.y() - tile000.y(), tile001.x(),
                                    tile001.y(), tile001.z(), tile011.y() - tile001.y());
            // We need to create a floor.
            emitTemplate(basePart, "room" + index + "AlphaBlendPartZl", basePhysicsPart, context, wallTemplateData,
                         vertexProcessor);
          }
        }
      }
    }
    pool.finish();
    PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
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

  protected void generateFloorData() {
  }

  protected void generatePropsData() {
  }

  protected void generateWalkwayData() {
  }

  protected void generateWallData() {
  }

  /**
   * In this sense, "encroachment" refers to the tiles of this room after
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
    private final LasertagWorldGenRoom room;
    private final Side side;

    private WallData(LasertagWorldGenRoom room, Side side) {
      this.room = room;
      this.side = side;
    }

    enum Side {
      Xl, Xh, Zl, Zh
    }
  }
}
