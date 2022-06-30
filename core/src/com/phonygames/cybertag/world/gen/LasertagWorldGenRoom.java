package com.phonygames.cybertag.world.gen;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PArrayUtils;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PSet;

public class LasertagWorldGenRoom {
  protected final LasertagWorldGenBuilding building;
  // Should be a wall with a window, with multiple parts.
  // private String windowModel = "model/template/window/basic.glb";
  protected final int index, roomX, roomY, roomZ, roomSizeX, roomSizeY, roomSizeZ;
  protected final transient PSet<Integer> roomIndicesConnectedByDoorFrames = new PSet<>();
  protected final PList<LasertagWorldGenWallData> wallData = new PList<>();
  protected String doorframeModel = "model/template/doorframe/basic.glb";
  protected String floorModel = "model/template/floor/basic.glb";
  protected String wallModel = "model/template/wall/basic.glb";

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
      for (int y = roomY; y < roomY + roomSizeY; y++) {
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
    //
    // Init parts and templates.
    final String partNamePrefix = partNamePrefix();
    PModelGen.Part basePart = modelGen.addPart(partNamePrefix + "Part", PVertexAttributes.getGLTF_UNSKINNED());
    PModelGen.StaticPhysicsPart basePhysicsPart = modelGen.addStaticPhysicsPart(partNamePrefix + "StaticPhysicsPart");
    LasertagWorldGen.RoomPartData roomPartData = context.addRoomPartData(building.index, index);
    roomPartData.modelgenParts.add(new LasertagWorldGen.RoomPartData.Part(
        PVec3.obtain().set(roomX + roomSizeX / 2, roomY + roomSizeY / 2, roomZ + roomSizeZ / 2), basePart,
        PGltf.Layer.PBR, new PMaterial(basePart.name(), null).useVColIndex(true)));
    roomPartData.modelgenStaticPhysicsParts.add(basePhysicsPart);
    int curVColIndex = roomPartData.vColIndex;
    int maxVColIndex = roomPartData.vColIndex + roomPartData.vColIndexLength - 1;
    //
    // Set up the vertex processor and temp variables.
    PModelGen.Part.VertexProcessor vertexProcessor = PModelGen.Part.VertexProcessor.staticPool().obtain();
    PPool.PoolBuffer pool = PPool.getBuffer();
    PMat4 emitTransform = pool.mat4();
    PVec3 tile000 = pool.vec3(), tile100 = pool.vec3(), tile010 = pool.vec3(), tile001 = pool.vec3();
    PVec3 tile110 = pool.vec3(), tile101 = pool.vec3(), tile011 = pool.vec3(), tile111 = pool.vec3();
    vertexProcessor.setTransform(emitTransform);
    //
    // Loop through walldata and emit the walls.
    for (LasertagWorldGenWallData data : wallData) {
      for (int a = 0; a < data.tileTypes.size; a++) {
        int x = data.tileXs.get(a);
        int y = data.baseY;
        int z = data.tileZs.get(a);
        LasertagWorldGenWallData.TileType tileType = data.tileTypes.get(a);
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
        LasertagWorldGen.MeshTemplate template = null;
        switch (tileType) {
          case NORMAL:
          default:
            template = context.template(wallModel);
            break;
          case WINDOW:
            PAssert.failNotImplemented("WINDOW");
            break;
          case DOORFRAME:
            template = context.template(doorframeModel);
            break;
        }
        emitTemplate(basePart, partNamePrefix + "AlphaBlendPart" + data.side.name() + ":" + x + "," + y + "," + z,
                     basePhysicsPart, context, template, vertexProcessor, curVColIndex);
      }
    }
    // TODO: remove this.
    //
    // Add floors and ceilings.
    for (int x = roomX; x < roomX + roomSizeX; x++) {
      for (int y = roomY; y < roomY + roomSizeY; y++) {
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
            emitTemplate(basePart, partNamePrefix + "AlphaBlendPartYl" + ":" + x + "," + y + "," + z, basePhysicsPart,
                         context, context.template(floorModel), vertexProcessor, curVColIndex);
          }
          if (roomYh != this) {
            vertexProcessor.setFlatQuad(tile010.add(0, -.01f, 0), tile110.add(0, -.01f, 0), tile111.add(0, -.01f, 0),
                                        tile011.add(0, -.01f, 0));
            // We need to create a ceiling.
            emitTemplate(basePart, partNamePrefix + "AlphaBlendPartYh" + ":" + x + "," + y + "," + z, basePhysicsPart,
                         context, context.template(floorModel), vertexProcessor, curVColIndex);
          }
        }
      }
    }
    pool.finish();
    PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
  }

  // Keep in sync with the equivalent function in LasertagWorldRoom.
  protected String partNamePrefix() {
    return new StringBuilder().append("building").append(building.index).append("_room").append(index).toString();
  }

  protected void emitTemplate(@NonNull PModelGen.Part basePart, @Nullable String alphaBlendPartName,
                              @Nullable PModelGen.StaticPhysicsPart staticPhysicsPart,
                              @NonNull LasertagWorldGen.Context context,
                              @NonNull LasertagWorldGen.MeshTemplate template,
                              @NonNull PModelGen.Part.VertexProcessor vertexProcessor, int vColIndexStart) {
    for (int a = 0; a < template.meshes.size; a++) {
      PMesh mesh = template.meshes.get(a);
      boolean emitMesh = template.emitMesh.get(a);
      boolean emitPhysics = template.emitPhysics.get(a);
      if (emitMesh) {
        basePart.set(PVertexAttributes.Attribute.Keys.col[0],
                     PMesh.vColForIndex(PVec4.obtain(), vColIndexStart + template.vColIndexOffsets.get(a)));
        basePart.emit(mesh, (emitPhysics && staticPhysicsPart != null) ? staticPhysicsPart : null, vertexProcessor,
                      basePart.vertexAttributes());
      } else if (staticPhysicsPart != null && emitPhysics) {
        staticPhysicsPart.emit(mesh, vertexProcessor);
      }
    }
  }

  protected void generateRoomData() {
    generateWallData();
    generateWalkwayData();
    generatePropsData();
    generateFloorData();
  }

  /**
   * Generates the data regarding what sort of walls to emit.
   */
  private void generateWallData() {
    for (int x = roomX; x < roomX + roomSizeX; x++) {
      for (int y = roomY; y < roomY + roomSizeY; y++) {
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
              wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Xl, x, y, z));
              wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Zl, x, y, z));
            } else if (roomXlZl == this) {
              // Create convex wall - only in the +z direction.
              wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Xl, x, y, z));
            }
          } else if (roomZl != this && roomXlZl == this) {
            // Create convex wall - only in the +x direction.
            wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Zl, x, y, z));
          }
          if (roomXh != this) {
            if (roomZl != this || roomXhZl == this) {
              // Create a wall in the +z direction.
              wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Xh, x, y, z));
            }
          }
          if (roomZh != this) {
            if (roomXl != this || roomXlZh == this) {
              // Create a wall in the +x direction.
              wallData.add(new LasertagWorldGenWallData(this, LasertagWorldGenWallData.Side.Zh, x, y, z));
            }
          }
        }
      }
    }
    for (LasertagWorldGenWallData data : wallData) {
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
    for (int y = roomY; y < roomY + roomSizeY; y++) {
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
    for (int y = roomY; y < roomY + roomSizeY; y++) {
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
}
