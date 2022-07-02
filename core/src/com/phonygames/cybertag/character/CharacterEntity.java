package com.phonygames.cybertag.character;

import com.phonygames.pengine.graphics.PRenderContext;

public abstract class CharacterEntity {
  public CharacterEntity() {

  }

  public abstract void preLogicUpdate();

  public abstract void logicUpdate();

  public abstract void frameUpdate();

  public abstract void render(PRenderContext renderContext);
}
