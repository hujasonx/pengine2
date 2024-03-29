//package com.phonygames.cybertag.world.gen;
//
//import com.badlogic.gdx.math.MathUtils;
//import com.phonygames.pengine.graphics.model.PModelGenOld;
//import com.phonygames.pengine.math.PMat4;
//import com.phonygames.pengine.math.PNumberUtils;
//import com.phonygames.pengine.math.PVec3;
//import com.phonygames.pengine.util.collection.PArrayUtils;
//import com.phonygames.pengine.util.collection.PList;
//
//public class LasertagWorldGenBuilding {
//  protected final int index;
//  protected final LasertagWorldGenRoom[][][] roomTiles;
//  protected final PList<LasertagWorldGenRoom> rooms = new PList<>();
//  protected final PVec3 scale = PVec3.obtain();
//  protected final int sizeX, sizeY, sizeZ;
//  protected final PMat4 worldTransform = PMat4.obtain();
//
//  protected LasertagWorldGenBuilding(int index, int sizeX, int sizeY, int sizeZ, float scaleX, float scaleY,
//                                     float scaleZ, int numRooms) {
//    this.index = index;
//    roomTiles = new LasertagWorldGenRoom[this.sizeX = sizeX][this.sizeY = sizeY][this.sizeZ = sizeZ];
//    scale.set(scaleX, scaleY, scaleZ);
//    for (int a = 0; a < numRooms; a++) {
//      LasertagWorldGenRoom potentialRoom = genRoom(rooms.size());
//      // Check for overlaps with other rooms.
//      final int minimumRoomSegmentSizeAfterEncroachment = 2; // This room will not replace existing room tiles, so
//      // make sure there arent any parts of this room that will be smaller than this value in dimension.
//      final float minimumKeptTilesRatio = .5f;
//      if (potentialRoom.isValidWithEncroachment(roomTiles, minimumRoomSegmentSizeAfterEncroachment,
//                                                minimumKeptTilesRatio)) {
//        potentialRoom.addToBuilding();
//      }
//    }
//  }
//
//  /**
//   * Attempts to generate a room.
//   * @return the room.
//   */
//  protected LasertagWorldGenRoom genRoom(int index) {
//    final int minRoomSize = 4, maxRoomSize = 8;
//    // The edgeShiftBoundary is added to the range edges of the location random() call. The location is then clamped
//    // back down to the original range. This allows for the rooms to be more clustered towards the edge of the
//    // building.
//    final int edgeShiftBoundaryX = 3, edgeShiftBoundaryZ = 3, edgeShiftLowerY = 1;
//    final float[] roomHeightWeights = new float[]{.5f, .2f};
//    int roomSizeX = MathUtils.random(minRoomSize, maxRoomSize);
//    int roomSizeY = PArrayUtils.randomIndexWithWeights(roomHeightWeights) + 1;
//    int roomSizeZ = MathUtils.random(minRoomSize, maxRoomSize);
//    int roomX = PNumberUtils.clamp(MathUtils.random(-edgeShiftBoundaryX, sizeX + edgeShiftBoundaryX - roomSizeX), 0,
//                                   sizeX - roomSizeX - 1);
//    int roomY = PNumberUtils.clamp(MathUtils.random(-edgeShiftLowerY, sizeY - roomSizeY), 0, sizeY - roomSizeY);
//    int roomZ = PNumberUtils.clamp(MathUtils.random(-edgeShiftBoundaryZ, sizeZ + edgeShiftBoundaryZ - roomSizeZ), 0,
//                                   sizeZ - roomSizeZ);
//    LasertagWorldGenRoom ret =
//        new LasertagWorldGenRoom(this, index, roomX, roomY, roomZ, roomSizeX, roomSizeY, roomSizeZ);
//    return ret;
//  }
//
//  protected void emit(PModelGenOld modelGen, LasertagWorldGenOld.Context context) {
//    for (int a = 0; a < rooms.size(); a++) {
//      LasertagWorldGenRoom room = rooms.get(a);
//      room.emit(modelGen, context);
//    }
//  }
//
//  protected void generateRoomData() {
//    for (int a = 0; a < rooms.size(); a++) {
//      LasertagWorldGenRoom room = rooms.get(a);
//      room.generateRoomData();
//    }
//  }
//
//  protected void getRoomTilePhysicalBounds(int x, int y, int z, PVec3 out000, PVec3 out100, PVec3 out010, PVec3 out001,
//                                           PVec3 out110, PVec3 out101, PVec3 out011, PVec3 out111) {
//    if (out000 != null) {
//      out000.set((x + 0) * scale.x(), (y + 0) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out100 != null) {
//      out100.set((x + 1) * scale.x(), (y + 0) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out010 != null) {
//      out010.set((x + 0) * scale.x(), (y + 1) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out001 != null) {
//      out001.set((x + 0) * scale.x(), (y + 0) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out110 != null) {
//      out110.set((x + 1) * scale.x(), (y + 1) * scale.y(), (z + 0) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out101 != null) {
//      out101.set((x + 1) * scale.x(), (y + 0) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out011 != null) {
//      out011.set((x + 0) * scale.x(), (y + 1) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
//    }
//    if (out111 != null) {
//      out111.set((x + 1) * scale.x(), (y + 1) * scale.y(), (z + 1) * scale.z()).mul(worldTransform, 1);
//    }
//  }
//
//  protected LasertagWorldGenRoom roomAtTile(int x, int y, int z) {
//    if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
//      return null;
//    }
//    return roomTiles[x][y][z];
//  }
//}
