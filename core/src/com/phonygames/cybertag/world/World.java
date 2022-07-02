package com.phonygames.cybertag.world;

import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.cybertag.world.lasertag.LasertagBuildGenDoorProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGen;
import com.phonygames.cybertag.world.lasertag.LasertagBuildingGenAABBPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGen;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenRoomPlacer;
import com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileProcessor;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.cybertag.world.lasertag.LasertagWorldGen;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.aabb.PIntAABB;

public class World {
  public final LasertagWorld lasertagWorld;
  private PlayerCharacterEntity playerCharacter;

  public World() {
    LasertagWorldGen worldGen = new LasertagWorldGen(this);
    LasertagBuildingGen buildingGen = new LasertagBuildingGen(worldGen);
    buildingGen.setTileTranslation(0, 0, 0).setTileRotation(.2f).setTileScale(3, 3, 3);
    LasertagBuildingGenAABBPlacer.addAABBs(buildingGen);
    LasertagRoomGenRoomPlacer.reset();
    for (int a = 0; a < 10; a++) {
      PIntAABB roomAABB = LasertagRoomGenRoomPlacer.getValidAABBForRoomPlacement(buildingGen);
      if (roomAABB != null) {
        LasertagRoomGen roomGen = new LasertagRoomGen(buildingGen, roomAABB);
        LasertagRoomGenTileProcessor.processRoomWalls(roomGen);
        LasertagRoomGenTileProcessor.processRoomFloors(roomGen);
        LasertagRoomGenTileProcessor.processRoomCeilings(roomGen);
      }
    }
    LasertagBuildGenDoorProcessor.processPossibleDoorsIntoAcutal(buildingGen);
    LasertagBuildingGen.finalPassWalls(buildingGen);
    this.lasertagWorld = worldGen.build();
    playerCharacter = new PlayerCharacterEntity();
  }

  public void logicUpdate() {
    lasertagWorld.logicUpdate();
    playerCharacter.logicUpdate();
  }

  public void preLogicUpdate() {
    playerCharacter.preLogicUpdate();
  }

  public void frameUpdate() {
    lasertagWorld.frameUpdate();
    playerCharacter.logicUpdate();
  }

  public void render(PRenderContext renderContext) {
    lasertagWorld.render(renderContext);
    playerCharacter.render(renderContext);
  }
}
