package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PIntMap3d;
import com.phonygames.pengine.util.PList;

import lombok.val;

public class LasertagBuildingGen extends PBuilder {
  protected final PIntAABB aabb = new PIntAABB();
  protected final PList<PIntAABB> outsideAabbs = new PList<>();
  protected final LasertagBuilding building;
  protected final PList<LasertagDoorGen> doorGens = new PList<>();
  protected final PList<LasertagDoorGen.PossibleWallDoor> possibleDoors = new PList<>();
  protected final PList<LasertagRoomGen> roomGens = new PList<>();
  protected final LasertagWorldGen worldGen;
  protected final PIntMap3d<LasertagTileGen> tileGens = new PIntMap3d<LasertagTileGen>() {
    @Override protected LasertagTileGen newUnpooled(int x, int y, int z) {
      return newUnpooledTileGen(x, y, z);}
  };
  protected LasertagTileGen newUnpooledTileGen(int x, int y, int z) {
    return new LasertagTileGen(this, LasertagBuildingGen.this.building.id + "(" + x + "," + y + "," + z + ")", x, y, z);
  }

  public LasertagBuildingGen(LasertagWorldGen worldGen) {
    this.worldGen = worldGen;
    worldGen.addBlockingTask(this);
    building = new LasertagBuilding("building" + worldGen.buildingGens.size(), worldGen.lasertagWorld);
    worldGen.buildingGens.add(this);
  }

  /**
   * After doors and windows are placed on tilewalls, this method can copy those settings to the opposing walls.
   * @param buildingGen
   */
  public static void finalPassWalls(LasertagBuildingGen buildingGen) {
    try (val it = buildingGen.tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        LasertagTileGen otherX = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.X);
        LasertagTileGen otherZ = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.Z);
        LasertagTileGen otherMX = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.mX);
        LasertagTileGen otherMZ = LasertagRoomGenTileProcessor.otherTileForWall(buildingGen, e.val(), LasertagTileWall.Facing.mZ);
        LasertagTileWallGen wallX = e.val().wallX;
        LasertagTileWallGen wallZ = e.val().wallZ;
        LasertagTileWallGen wallMX = e.val().wallMX;
        LasertagTileWallGen wallMZ = e.val().wallMZ;
        wallX.copySettingsFromOrToOtherWall(true);
        wallZ.copySettingsFromOrToOtherWall(true);
        wallMX.copySettingsFromOrToOtherWall(true);
        wallMZ.copySettingsFromOrToOtherWall(true);
      }
    }
  }

  protected void addOutsideAABB(int x0, int y0, int z0, int x1, int y1, int z1) {
    checkLock();
    PIntAABB aabb = new PIntAABB().set(x0, y0, z0, x1, y1, z1);
    outsideAabbs.add(aabb);
  }

  public LasertagBuilding build() {
    lockBuilder();
    building.outsideAabbs = new PIntAABB[outsideAabbs.size()];
    for (int a = 0; a < outsideAabbs.size(); a++) {
      building.outsideAabbs[a] = outsideAabbs.get(a);
    }
    building.rooms = new LasertagRoom[roomGens.size()];
    for (int a = 0; a < roomGens.size(); a++) {
      building.rooms[a] = roomGens.get(a).build();
    }
    try (val it = tileGens.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        e.val().build();
      }
    }
    for (int a = 0; a < doorGens.size(); a++) {
      val e = doorGens.get(a);
      e.build();
    }
    buildModelInstance();
    return building;
  }

  private void buildModelInstance() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      @Override protected void modelEnd() {

        worldGen.clearBlockingTask(LasertagBuildingGen.this);
      }
    });
  }

  public void processTiles() {
    for (int a = 0; a < roomGens.size(); a++) {
      val roomGen = roomGens.get(a);

      LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
      LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
      LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
    }
//    try (val it = roomGens.obtainIterator()) {
//      while (it.hasNext()) {
//        val roomGen = it.next();
//        LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
//        LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
//        LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
//        LasertagRoomGenWalkwayProcessor.processRoomWalkways(roomGen);
//      }
//    }
  }

  public LasertagBuildingGen setTileRotation(float rotation) {
    this.building.tileRotation().setToRotation(0, 1, 0, rotation);
    return this;
  }

  public LasertagBuildingGen setTileScale(float x, float y, float z) {
    this.building.tileScale().set(x, y, z);
    return this;
  }

  public LasertagBuildingGen setTileTranslation(float x, float y, float z) {
    this.building.tileTranslation().set(x, y, z);
    return this;
  }
}
