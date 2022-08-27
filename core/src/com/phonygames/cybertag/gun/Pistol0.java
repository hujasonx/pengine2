package com.phonygames.cybertag.gun;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.particles.PBillboardParticle;
import com.phonygames.pengine.graphics.particles.PPipeParticle;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PSODynamics;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PPool;

public class Pistol0 extends Gun {
  public Pistol0(PlayerCharacterEntity playerCharacter) {
    super("model/gun/pistol0.glb", playerCharacter);
    firstPersonStandardOffsetFromCamera.set(-.17f, -.1f, .34f);
    walkCycleShakeTEdgeInset = .1f;
    walkCycleXOffsetScale = .01f;
    walkCycleYOffsetScale = .014f;
    walkCycleYOffsetPower = .75f;
    reloadAnimation = "Reload";
    triggerAnimation = "PullTrigger";
    defaultAnimation = "Default";
    cameraOffsetSpring.setDynamicsParams(5, 1, 0);
    recoilCameraEulRotSpring.setDynamicsParams(3, 1, 0);
    recoilCameraEulRotImpulse.set(0, 3, 0);
    recoilEulRotSpring.setDynamicsParams(5, PSODynamics.zetaFromMagnitudeRemainingPerPeriod(5, .4f), 0);
    recoilEulRotImpulse.set(0, -24f, 0);
    recoilOffsetSpring.setDynamicsParams(1f / .3f, PSODynamics.zetaFromMagnitudeRemainingPerPeriod(1f / .3f, .2f), 0);
    recoilOffsetImpulse.set(0, 0, -1.3f);
    fovSpring.setDynamicsParams(3, 1, 0);
    adsFOV = 40;
    weaponIdleSwayLeftSettings.set(.001f, 8);
    weaponIdleSwayUpSettings.set(.003f, 3.28f);
    weaponIdleSwayDirSettings.set(.001f, 9.2f);
    firePointNodeName = "Firepoint";
    triggerTSpring.setDynamicsParams(5, 1, 0);
    setMuzzleParticleSourceDelegate();
    setPipeParticleSourceDelegate();
  }

  private void setMuzzleParticleSourceDelegate() {
    muzzleParticleSource.delegate(particle -> {
      float a = .3f - PNumberUtils.pow(particle.lifeT(), 3) * .9f;
      if (a < 0) {
        particle.kill();
        return;
      }
      particle.col0().a(a);
      particle.setColFrom0();
    });
  }

  private void setPipeParticleSourceDelegate() {
    pipeParticleSource.delegate(particle -> {
      float a = .6f - PNumberUtils.pow(particle.lifeT(), 3) * .03f;
      // Apply gravity.
      //      particle.vel().y(particle.vel().y() - PEngine.dt * 9.81f);
      if (a < 0) {
        particle.kill();
        return;
      }
    });
  }

  @Override public String name() {
    return "Pistol0";
  }

  @Override public void onShoot() {
    super.onShoot();
    spawnShootParticles();
  }

  @Override public void render(PRenderContext renderContext) {super.render(renderContext);}

  private void spawnShootParticles() {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 firePointPos = modelInstance.getNode(firePointNodeName).worldTransform().getTranslation(pool.vec3());
      PVec3 fireDir = modelInstance.getNode(firePointNodeName).worldTransform().getYAxis(pool.vec3());
      for (int a = 0; a < 5; a++) {
        PBillboardParticle particle = muzzleParticleSource.spawnParticle();
        particle.accelVelocityDir(-6);
        particle.faceCamera(true);
        particle.faceCameraXScale(.1f);
        particle.faceCameraYScale(.1f);
        particle.col0().setHSVA(MathUtils.random(.0f, .02f), 1, 1, .6f);
        particle.setColFrom0();
        particle.texture().set(PAssetManager.textureRegion("textureAtlas/particles.atlas", "Glow", true));
        particle.angVel(MathUtils.random(-3f, 3f));
        particle.angVelDecel(3f);
        particle.vel().set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor()
                .scl(MathUtils.random(2f));
        particle.vel().add(fireDir, 5);
        particle.pos().set(firePointPos);
      }
      for (int a = 0; a < 1; a++) {
        PPipeParticle particle = pipeParticleSource.spawnParticle();
        particle.updateTopology(4, 4);
        particle.headCol().setHSVA(.15f, 1, 3, 1);
        //        particle.tailCol().setHSVA(MathUtils.random(), 1, 10, 1);
        particle.tailCol().set(particle.headCol());
        particle.vel().set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor()
                .scl(MathUtils.random(.5f));
        particle.vel().add(fireDir, 250);
        particle.pos().set(firePointPos);
        particle.intermediatePointNormalizedTimes().add(.05f).add(.22f).add(.5f).add(.7f);
        particle.intermediateRingRadii().add(.02f).add(.04f).add(.02f).add(.01f);
        particle.beginTracking(.02f, 10);
        particle.applyColorToVertices();
      }
    }
  }
}
