package com.phonygames.cybertag.world;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.character.NpcHumanoidEntity;
import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.cybertag.world.lasertag.LasertagBuildGenDoorProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGen;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGenAABBPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGenHallwayProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGen;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenRoomPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenWalkwayProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.cybertag.world.lasertag.LasertagWorldGen;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.aabb.PIntAABB;

public class World {
  public final LasertagWorld lasertagWorld;
  private PlayerCharacterEntity playerCharacter;
  private NpcHumanoidEntity npcHumanoidEntity;

  public World() {
    LasertagWorldGen worldGen = new LasertagWorldGen(this);
    LasertagBuildingGen buildingGen = new LasertagBuildingGen(worldGen);
    buildingGen.setTileTranslation(0, 0, 0).setTileRotation(.2f).setTileScale(4, 4, 4);
    LasertagBuildingGenAABBPlacer.addAABBs(buildingGen);
    LasertagRoomGenRoomPlacer.reset();
    LasertagRoomGenRoomPlacer.roomHeightWeights = new float[]{.5f, .4f, .3f, .2f, .1f};
    for (int a = 0; a < 50; a++) {
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
    this.lasertagWorld = worldGen.build();
    playerCharacter = new PlayerCharacterEntity();
    npcHumanoidEntity = new NpcHumanoidEntity();
  }

  public void logicUpdate() {
    lasertagWorld.logicUpdate();
    playerCharacter.logicUpdate();
    npcHumanoidEntity.logicUpdate();
  }

  public void preLogicUpdate() {
    playerCharacter.preLogicUpdate();
    npcHumanoidEntity.preLogicUpdate();
  }

  public void frameUpdate() {
    lasertagWorld.frameUpdate();
    playerCharacter.frameUpdate();
    npcHumanoidEntity.frameUpdate();
  }

  public void render(PRenderContext renderContext) {
    lasertagWorld.render(renderContext);
    playerCharacter.render(renderContext);
    npcHumanoidEntity.render(renderContext);
  }
}
