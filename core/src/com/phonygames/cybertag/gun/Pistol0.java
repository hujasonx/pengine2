package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.PlayerCharacterEntity;

public class Pistol0 extends Gun{
  public Pistol0(PlayerCharacterEntity playerCharacter) {
    super("model/gun/pistol0.glb", playerCharacter);
//    firstPersonStandardOffsetFromCamera.set(-.17f, -.1f, .29f);
    firstPersonStandardOffsetFromCamera.set(-.17f, -.1f, .39f);
    reloadAnimation = "Reload";
  }

  @Override public String name() {
    return "Pistol0";
  }
}
