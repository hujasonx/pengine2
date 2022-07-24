package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.pengine.math.PSODynamics;

public class Pistol0 extends Gun {
  public Pistol0(PlayerCharacterEntity playerCharacter) {
    super("model/gun/pistol0.glb", playerCharacter);
    firstPersonStandardOffsetFromCamera.set(-.17f, -.1f, .34f);
    walkCycleShakeTEdgeInset = .1f;
    walkCycleXOffsetScale = .01f;
    walkCycleYOffsetScale = .014f;
    walkCycleYOffsetPower = .75f;
    reloadAnimation = "Reload";
    cameraOffsetSpring.setDynamicsParams(5, 1, 0);
    recoilCameraEulRotSpring.setDynamicsParams(3, 1, 0);
    recoilCameraEulRotImpulse.set(0, 3, 0);
    recoilEulRotSpring.setDynamicsParams(5, PSODynamics.zetaFromMagnitudeRemainingPerPeriod(5, .4f), 0);
    recoilEulRotImpulse.set(0, -24f, 0);
    recoilOffsetSpring.setDynamicsParams(1f / .3f, PSODynamics.zetaFromMagnitudeRemainingPerPeriod(1f / .3f, .2f), 0);
    recoilOffsetImpulse.set(0, 0, -1.3f);
    fovSpring.setDynamicsParams(3,1,0);
    adsFOV = 40;
  }

  @Override public String name() {
    return "Pistol0";
  }
}
