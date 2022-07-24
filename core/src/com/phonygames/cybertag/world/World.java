package com.phonygames.cybertag.world;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.character.NpcHumanoidEntity;
import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.cybertag.world.lasertag.LasertagWorld;
import com.phonygames.cybertag.world.lasertag.WorldGen;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.particles.PBillboardParticle;
import com.phonygames.pengine.graphics.particles.PBillboardParticleSource;
import com.phonygames.pengine.input.PKeyboard;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.navmesh.PTileCache;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;

import org.recast4j.detour.tilecache.TileCache;

public class World {
  public final LasertagWorld lasertagWorld;
  private PBillboardParticleSource billboardParticleSource;
  private NpcHumanoidEntity npcHumanoidEntity;
  public PlayerCharacterEntity playerCharacter;
  public PTileCache tileCache;
  public final PList<Integer> physicsVertexIndices = new PList<>();
  public final PList<Float> physicsVertexPositions = new PList<>();

  public World() {
    lasertagWorld = WorldGen.gen(this);
    playerCharacter = new PlayerCharacterEntity();
    npcHumanoidEntity = new NpcHumanoidEntity(this);
    billboardParticleSource = new PBillboardParticleSource();
  }

  public void previewNavMeshData() {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      for (int a = 0; a < physicsVertexIndices.size(); a += 3) {
        int i0 = physicsVertexIndices.get(a + 0);
        int i1 = physicsVertexIndices.get(a + 1);
        int i2 = physicsVertexIndices.get(a + 2);
        PVec3 v0 = pool.vec3(physicsVertexPositions.get(i0 * 3 + 0), physicsVertexPositions.get(i0 * 3 + 1),
                             physicsVertexPositions.get(i0 * 3 + 2));
        PVec3 v1 = pool.vec3(physicsVertexPositions.get(i1 * 3 + 0), physicsVertexPositions.get(i1 * 3 + 1),
                             physicsVertexPositions.get(i1 * 3 + 2));
        PVec3 v2 = pool.vec3(physicsVertexPositions.get(i2 * 3 + 0), physicsVertexPositions.get(i2 * 3 + 1),
                             physicsVertexPositions.get(i2 * 3 + 2));
        PDebugRenderer.line(v0, v1, PColor.GREEN, PColor.GREEN, 1, 1);
        PDebugRenderer.line(v1, v2, PColor.GREEN, PColor.GREEN, 1, 1);
        PDebugRenderer.line(v2, v0, PColor.GREEN, PColor.GREEN, 1, 1);
      }
    }
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
    if (tileCache != null) {
      tileCache.previewNavmesh();
      //      previewNavMeshData();
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
