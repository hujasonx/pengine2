package com.phonygames.cybertag.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.character.NpcHumanoidEntity;
import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.cybertag.world.lasertag.WorldGen;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.particles.PBillboardParticle;
import com.phonygames.pengine.graphics.particles.PBillboardParticleSource;
import com.phonygames.pengine.input.PKeyboard;

public class World {
  public final LasertagWorld lasertagWorld;
  private PBillboardParticleSource billboardParticleSource;
  private NpcHumanoidEntity npcHumanoidEntity;
  private PlayerCharacterEntity playerCharacter;

  public World() {
    lasertagWorld = WorldGen.gen(this);
    playerCharacter = new PlayerCharacterEntity();
    npcHumanoidEntity = new NpcHumanoidEntity();
    billboardParticleSource = new PBillboardParticleSource();
  }

  public void frameUpdate() {
    lasertagWorld.frameUpdate();
    playerCharacter.frameUpdate();
    npcHumanoidEntity.frameUpdate();
    billboardParticleSource.frameUpdate();
    if (PKeyboard.isFrameJustDown(Input.Keys.ALT_RIGHT)) {
      PBillboardParticle particle = billboardParticleSource.spawnParticle();
      particle.texture().set(PAssetManager.textureRegion("textureAtlas/particles.atlas", "Glow", true));
      particle.faceCamera(true);
      particle.faceCameraXScale(4);
      particle.faceCameraYScale(4);
      particle.angVel(MathUtils.random(-1f, 1f));
      particle.angVelDecel(.1f);
      particle.vel().set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f));
      particle.accelVelocityDir(-.1f);
      particle.pos().set(MathUtils.random(10f), MathUtils.random(10f), MathUtils.random(10f));
    }
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

  public void render(PRenderContext renderContext) {
    lasertagWorld.render(renderContext);
    playerCharacter.render(renderContext);
    npcHumanoidEntity.render(renderContext);
    billboardParticleSource.setOrigin(renderContext.cameraPos());
    billboardParticleSource.render(renderContext);
  }
}
