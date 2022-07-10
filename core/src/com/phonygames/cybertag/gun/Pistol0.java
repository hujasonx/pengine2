package com.phonygames.cybertag.gun;

import com.phonygames.cybertag.character.PlayerCharacterEntity;
import com.phonygames.pengine.util.PPool;

public class Pistol0 extends Gun{
  public Pistol0(PlayerCharacterEntity playerCharacter) {
    super("model/gun/pistol0.glb", playerCharacter);
    standardOffsetFromPlayer.set(-.1f, 1.34f, .4f);
    reloadAnimation = "Reload";
  }

  @Override public String name() {
    return "Pistol0";
  }
}
