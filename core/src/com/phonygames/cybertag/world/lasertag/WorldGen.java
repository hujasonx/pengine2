package com.phonygames.cybertag.world.lasertag;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.math.aabb.PIntAABB;

public class WorldGen {
  public static LasertagWorld gen(World world) {
    LasertagWorldGen worldGen = new LasertagWorldGen(world);
    LasertagBuildingGen buildingGen = new LasertagBuildingGen(worldGen);
    buildingGen.setTileTranslation(0, 0, 0).setTileRotation(0).setTileScale(4, 4, 4);
    LasertagBuildingGenAABBPlacer.addAABBs(buildingGen);
    LasertagRoomGenRoomPlacer.reset();
    LasertagRoomGenRoomPlacer.roomHeightWeights = new float[]{.5f, .4f, .3f, .2f, .1f};
    for (int a = 0; a < 20; a++) {
      PIntAABB roomAABB = LasertagRoomGenRoomPlacer.getValidAABBForRoomPlacement(buildingGen);
      if (roomAABB != null) {
        LasertagRoomGen roomGen = new LasertagRoomGen(buildingGen, roomAABB);
      }
    }
    LasertagBuildingGenHallwayProcessor.processHallways(buildingGen);
    buildingGen.processTiles();
    LasertagBuildingGen buildingGen2 = new LasertagBuildingGen(worldGen);
    buildingGen2.setTileTranslation(MathUtils.random(100f, 150f), MathUtils.random(-10f, 10f), MathUtils.random(100f, 150f)).setTileRotation(MathUtils.random(MathUtils.PI2)).setTileScale(4, 4, 4);
    LasertagBuildingGenAABBPlacer.addAABBs(buildingGen2);
    LasertagRoomGenRoomPlacer.reset();
    for (int a = 0; a < 10; a++) {
      PIntAABB roomAABB = LasertagRoomGenRoomPlacer.getValidAABBForRoomPlacement(buildingGen2);
      if (roomAABB != null) {
        LasertagRoomGen roomGen = new LasertagRoomGen(buildingGen2, roomAABB);
      }
    }
    LasertagBuildingGenHallwayProcessor.processHallways(buildingGen2);
    buildingGen2.processTiles();
    // Process
    LasertagBuildGenDoorProcessor.processPossibleDoorsIntoAcutal(buildingGen);
    LasertagBuildGenDoorProcessor.processPossibleDoorsIntoAcutal(buildingGen2);
    // Hallway Pass.
    // Final wall pass.
    LasertagBuildingGen.finalPassWalls(buildingGen);
    LasertagBuildingGen.finalPassWalls(buildingGen2);
    // Walkway Pass.

    LasertagRoomGenWalkwayProcessor.processRoomWalkways(buildingGen);
    LasertagRoomGenWalkwayProcessor.processRoomWalkways(buildingGen2);
    return worldGen.build();
  }

}
