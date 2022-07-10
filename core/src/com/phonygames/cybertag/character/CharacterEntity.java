package com.phonygames.cybertag.character;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.math.PMat4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public abstract class CharacterEntity {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransform = PMat4.obtain();

  public CharacterEntity() {

  }

  public abstract void preLogicUpdate();

  public abstract void logicUpdate();

  public abstract void frameUpdate();

  public abstract void render(PRenderContext renderContext);
}
