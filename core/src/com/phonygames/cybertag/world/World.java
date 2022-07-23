package com.phonygames.cybertag.world;

import com.badlogic.gdx.Input;
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
import com.phonygames.cybertag.world.lasertag.WorldGen;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.particles.PBillboardParticle;
import com.phonygames.pengine.graphics.particles.PBillboardParticleSource;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.aabb.PIntAABB;

public class World {
  public final LasertagWorld lasertagWorld;
  private PlayerCharacterEntity playerCharacter;
  private NpcHumanoidEntity npcHumanoidEntity;
  private PBillboardParticleSource billboardParticleSource;

  public World() {
    lasertagWorld = WorldGen.gen(this);
    playerCharacter = new PlayerCharacterEntity();
    npcHumanoidEntity = new NpcHumanoidEntity();
    billboardParticleSource = new PBillboardParticleSource();
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
    billboardParticleSource.frameUpdate();
    if (PKeyboard.isFrameJustDown(Input.Keys.ALT_RIGHT)) {
      PBillboardParticle particle = billboardParticleSource.spawnParticle();
      particle.texture().set(PAssetManager.textureRegion("textureAtlas/particles.atlas", "Mist2", true));
      particle.faceCamera(true);
      particle.faceCameraXScale(1);
      particle.faceCameraYScale(1);
      particle.pos().set(MathUtils.random(10f), MathUtils.random(10f), MathUtils.random(10f));
    }
  }

  public void render(PRenderContext renderContext) {
    lasertagWorld.render(renderContext);
    playerCharacter.render(renderContext);
    npcHumanoidEntity.render(renderContext);
    billboardParticleSource.setOrigin(renderContext.cameraPos());
    billboardParticleSource.render(renderContext);
  }
}
