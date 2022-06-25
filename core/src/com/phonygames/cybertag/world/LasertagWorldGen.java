package com.phonygames.cybertag.world;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.Duple;
import com.phonygames.pengine.util.PArrayUtils;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PSet;

import lombok.val;

public class LasertagWorldGen {
  private static final PMap<String, PMap<String, Float>> styleCompatibilities =
      new PMap<String, PMap<String, Float>>() {
        @Override public PMap<String, Float> newUnpooled(String key) {
          return new PMap<>();
        }
      };
  private final Context context = new Context();
  private boolean wasGenned = false;

  public LasertagWorldGen() {
  }

  public void gen(@NonNull final OnFinishedCallback onFinishedCallback) {
    PAssert.isFalse(wasGenned);
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PModelGen.Part basePart;
      PModelGen.StaticPhysicsPart basePhysicsPart;

      @Override protected void modelIntro() {
        basePart = addPart("base", new PVertexAttributes(
            new VertexAttribute[]{PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos),
                                  PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.nor)}));
        basePhysicsPart = addStaticPhysicsPart("base");
      }

      @Override protected void modelMiddle() {
        //        // Get the first glNode's mesh and physics status from the model.
        //        PGlNode basicWallNode = PAssetManager.model("model/template/wall/basic.glb", true).getFirstNode();
        //        PMesh basicWallMesh = basicWallNode.drawCall().mesh();
        //        boolean basicWallStaticBody = basicWallNode.drawCall().material().id().contains(".alsoStaticBody");
        //        PGlNode basicFloorNode = PAssetManager.model("model/template/floor/basic.glb", true).getFirstNode();
        //        PMesh basicFloorMesh = basicFloorNode.drawCall().mesh();
        //        boolean basicFloorStaticBody = basicFloorNode.drawCall().material().id().contains(".alsoStaticBody");
        //        Part.VertexProcessor vertexProcessor = Part.VertexProcessor.staticPool().obtain();
        //        PMat4 emitTransform = PMat4.obtain();
        //        vertexProcessor.setTransform(emitTransform);
        //        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, vertexProcessor,
        //                      basePart.vertexAttributes());
        //        basePart.emit(basicFloorMesh, basicFloorStaticBody ? basePhysicsPart : null, vertexProcessor,
        //                      basePart.vertexAttributes());
        //        //        vertexProcessor.setTransform(emitTransform.translate(1, 0, 0));
        //        vertexProcessor.setWall(0, 0, 1, 1, 0, -.5f, 1.4f, 1f);
        //        basePart.emit(basicWallMesh, basicWallStaticBody ? basePhysicsPart : null, vertexProcessor,
        //                      basePart.vertexAttributes());
        //        //        vertexProcessor.setFlatQuad(0, 0, 1, 1, 0, 1, 1, -.4f, 2, 0, -.6f, 2);
        //        vertexProcessor.setFlatQuad(0, -.6f, 2, 0, 0, 1, 1, 0, 1, 1, -.4f, 2);
        //        basePart.emit(basicFloorMesh, basicFloorStaticBody ? basePhysicsPart : null, vertexProcessor,
        //                      basePart.vertexAttributes());
        //        emitTransform.free();
        //        Part.VertexProcessor.staticPool().free(vertexProcessor);
        Building b = new Building(20, 3, 20, 3, 4, 3, 300);
        b.emit(this, context);
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null), null, PGltf.Layer.PBR);
        for (val e : context.modelgenParts) {
          PVec3 partCenter = e.getKey();
          PModelGen.Part part = e.getValue();
          // TODO: use a different layer for alphablend parts.
          chainGlNode(glNodes, part, new PMaterial(part.name(), null), null, PGltf.Layer.PBR);
        }
        PModel.Builder builder = new PModel.Builder();
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode("lasertagworld", null, glNodes, PMat4.IDT);
        onFinishedCallback.onFinished(builder.build());
        wasGenned = true;
      }
    });
  }

  private static class Building {
    private final Room[][][] roomTiles;
    private final PList<Room> rooms = new PList<>();
    private final PVec3 scale = PVec3.obtain();
    private final int sizeX, sizeY, sizeZ;
    private final PMat4 worldTransform = PMat4.obtain();

    private Building(int sizeX, int sizeY, int sizeZ, float scaleX, float scaleY, float scaleZ, int numRooms) {
      roomTiles = new Room[this.sizeX = sizeX][this.sizeY = sizeY][this.sizeZ = sizeZ];
      scale.set(scaleX, scaleY, scaleZ);
      for (int a = 0; a < numRooms; a++) {
        Room potentialRoom = genRoom(rooms.size);
        // Check for overlaps with other rooms.
        final int minimumRoomSegmentSizeAfterEncroachment = 3; // This room will not replace existing room tiles, so
        // make sure there arent any parts of this room that will be smaller than this value in dimension.
        final float minimumKeptTilesRatio = .5f;
        if (potentialRoom.isValidWithEncroachment(roomTiles, minimumRoomSegmentSizeAfterEncroachment, minimumKeptTilesRatio)) {
          potentialRoom.addToBuilding();
          System.out.println("!!!JASON adding room");
        } else {
          System.out.println("!!!JASON not adding room");
        }
      }
    }

    /**
     * Attempts to generate a room.
     * @return the room.
     */
    private Room genRoom(int index) {
      final int minRoomSize = 5, maxRoomSize = 10;
      // The edgeShiftBoundary is added to the range edges of the location random() call. The location is then clamped
      // back down to the original range. This allows for the rooms to be more clustered towards the edge of the
      // building.
      final int edgeShiftBoundaryX = 3, edgeShiftBoundaryZ = 3, edgeShiftLowerY = 1;
      final float[] roomHeightWeights = new float[]{.5f, .2f};
      int roomSizeX = MathUtils.random(minRoomSize, maxRoomSize);
      int roomSizeY = PArrayUtils.randomIndexWithWeights(roomHeightWeights) + 1;
      int roomSizeZ = MathUtils.random(minRoomSize, maxRoomSize);
      int roomX = PNumberUtils.clamp(MathUtils.random(-edgeShiftBoundaryX, sizeX + edgeShiftBoundaryX - roomSizeX), 0,
                                     sizeX - roomSizeX - 1);
      int roomY = PNumberUtils.clamp(MathUtils.random(-edgeShiftLowerY, sizeY - roomSizeY), 0, sizeY - roomSizeY);
      int roomZ = PNumberUtils.clamp(MathUtils.random(-edgeShiftBoundaryZ, sizeZ + edgeShiftBoundaryZ - roomSizeZ), 0,
                                     sizeZ - roomSizeZ);
      Room ret = new Room(this, index, roomX, roomY, roomZ, roomSizeX, roomSizeY, roomSizeZ);
      return ret;
    }

    private void emit(PModelGen modelGen, Context context) {
      for (Room room : rooms) {
        room.emit(modelGen, context);
      }
    }

    private void getRoomTilePhysicalBounds(int x, int y, int z, PVec3 out000, PVec3 out100, PVec3 out010, PVec3 out001,
                                           PVec3 out110, PVec3 out101, PVec3 out011, PVec3 out111) {
      if (out000 != null) {
        out000.set((x + 0) * scale.x(), (y + 0) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
      }
      if (out100 != null) {
        out100.set((x + 1) * scale.x(), (y + 0) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
      }
      if (out010 != null) {
        out010.set((x + 0) * scale.x(), (y + 1) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
      }
      if (out001 != null) {
        out001.set((x + 0) * scale.x(), (y + 0) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
      }
      if (out110 != null) {
        out110.set((x + 1) * scale.x(), (y + 1) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
      }
      if (out101 != null) {
        out101.set((x + 1) * scale.x(), (y + 0) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
      }
      if (out011 != null) {
        out011.set((x + 0) * scale.x(), (y + 1) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
      }
      if (out111 != null) {
        out111.set((x + 1) * scale.x(), (y + 1) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
      }
    }

    private Room roomAtTile(int x, int y, int z) {
      if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
        return null;
      }
      return roomTiles[x][y][z];
    }

    private static class Room {
      private final Building building;
      private final transient PList<Duple<PMesh, Boolean>> doorframeTemplateData = new PList<>();
      // Should be a wall with a window, with multiple parts.
      // private String windowModel = "model/template/window/basic.glb";
      private final transient PList<Duple<PMesh, Boolean>> floorTemplateData = new PList<>();
      private final int index, roomX, roomY, roomZ, roomSizeX, roomSizeY, roomSizeZ;
      private final transient PList<Duple<PMesh, Boolean>> wallTemplateData = new PList<>();
      private String doorframeModel = "model/template/doorframe/basic.glb";
      private String floorModel = "model/template/floor/basic.glb";
      private String wallModel = "model/template/wall/basic.glb";

      private Room(@NonNull Building building, int index, int roomX, int roomY, int roomZ, int roomSizeX, int roomSizeY,
                   int roomSizeZ) {
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
              Room roomAtTile = building.roomTiles[x][y][z];
              if (roomAtTile == null) { // Don't replace existing room tiles.
                building.roomTiles[x][y][z] = this;
              }
            }
          }
        }
      }

      private void emit(PModelGen modelGen, Context context) {
        PModelGen.Part basePart = modelGen.addPart("room" + index + "Part", PVertexAttributes.getGLTF_UNSKINNED());
        PModelGen.StaticPhysicsPart basePhysicsPart =
            modelGen.addStaticPhysicsPart("room" + index + "StaticPhysicsPart");
        context.modelgenParts.add(
            new Duple<>(PVec3.obtain().set(roomX + roomSizeX / 2, roomY + roomSizeY / 2, roomZ + roomSizeZ / 2),
                        basePart));
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
              Room roomAtTile = building.roomTiles[x][y][z];
              if (roomAtTile != this) {continue;}
              Room roomXl = building.roomAtTile(x - 1, y, z);
              Room roomXh = building.roomAtTile(x + 1, y, z);
              Room roomZl = building.roomAtTile(x, y, z + 1);
              Room roomZh = building.roomAtTile(x, y, z - 1);
              Room roomYl = building.roomAtTile(x, y - 1, z);
              Room roomYh = building.roomAtTile(x, y + 1, z);
              building.getRoomTilePhysicalBounds(x, y, z, tile000, tile100, tile010, tile001, tile110, tile101, tile011,
                                                 tile111);
              if (roomYl != this) {
                vertexProcessor.setFlatQuad(tile000, tile100, tile101, tile001);
                // We need to create a floor.
                emitTemplate(basePart, "room" + index + "AlphaBlendPartYl", basePhysicsPart, context,
                             floorTemplateData, vertexProcessor);
              }
              if (roomXl != this) {
                vertexProcessor.setWall(tile000.x(), tile000.y(), tile000.z(), tile010.y() - tile000.y(), tile001.x(), tile001.y(), tile001.z(), tile011.y() - tile001.y());
                // We need to create a floor.
                emitTemplate(basePart, "room" + index + "AlphaBlendPartZl", basePhysicsPart, context,
                             wallTemplateData, vertexProcessor);
              }
            }
          }
        }
        pool.finish();
        PModelGen.Part.VertexProcessor.staticPool().free(vertexProcessor);
      }

      private void emitTemplate(@NonNull PModelGen.Part basePart, @Nullable String alphaBlendPartName,
                                @Nullable PModelGen.StaticPhysicsPart staticPhysicsPart, @NonNull Context context,
                                @NonNull PList<Duple<PMesh, Boolean>> template,
                                @NonNull PModelGen.Part.VertexProcessor vertexProcessor) {
        for (val e : template) {
          PMesh mesh = e.getKey();
          boolean alsoStaticBody = e.getValue();
          basePart.emit(mesh, (alsoStaticBody && staticPhysicsPart != null) ? staticPhysicsPart : null, vertexProcessor,
                        basePart.vertexAttributes());
        }
      }

      /**
       * In this sense, "encroachment" refers to the tiles of this room after
       * @param roomTiles
       * @param minimumRoomSizeAfterEnchroachment
       * @return
       */
      public boolean isValidWithEncroachment(Room[][][] roomTiles, int minimumRoomSizeAfterEnchroachment, float minimumKeptTilesRatio) {
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
        if (((float)keptTiles)/((float)(roomSizeX * roomSizeY * roomSizeZ)) < minimumKeptTilesRatio) {
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
    }
  }

  private static class Context {
    // The key here is the origin of the part, mainly used for alpha blend part sorting.
    private PSet<Duple<PVec3, PModelGen.Part>> modelgenParts = new PSet<>();
    private PSet<PModelGen.StaticPhysicsPart> modelgenStaticPhysicsParts = new PSet<>();
  }

  static abstract class OnFinishedCallback {
    public abstract void onFinished(PModel model);
  }
}
