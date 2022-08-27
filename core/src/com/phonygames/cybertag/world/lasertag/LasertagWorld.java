package com.phonygames.cybertag.world.lasertag;

import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagWorld {
  public final World world;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected LasertagBuilding[] buildings;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;

  protected LasertagWorld(World world) {
    this.world =world;
  }

  public void frameUpdate() {
    for (val building : buildings) {
      building.frameUpdate();
    }
  }

  public void logicUpdate() {
    for (val building : buildings) {
      building.logicUpdate();
    }
  }

  public void render(PRenderContext renderContext) {
    for (val building : buildings) {
      building.render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
