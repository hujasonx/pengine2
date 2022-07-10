package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.PlayerCharacterEntity;

public class Pistol0 extends Gun{
  public Pistol0(PlayerCharacterEntity playerCharacter) {
    super("model/gun/pistol0.glb", playerCharacter);
    standardOffsetFromPlayer.set(0, 1, .4f);
  }

  @Override public String name() {
    return "Pistol0";
  }
}
