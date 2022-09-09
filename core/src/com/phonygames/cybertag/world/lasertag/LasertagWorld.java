package com.phonygames.cybertag.world.lasertag;

import com.phonygames.cybertag.world.World;
import com.phonygames.cybertag.world.grid.TileBuilding;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class LasertagWorld {
  public final World world;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected final PList<TileBuilding> buildings = new PList<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  protected PModelInstance modelInstance;

  protected LasertagWorld(World world) {
    this.world =world;
  }

  public void frameUpdate() {
    for (int a = 0; a < buildings.size(); a++) {
      TileBuilding building = buildings.get(a);
      building.frameUpdate();
    }
  }

  public void logicUpdate() {
    for (int a = 0; a < buildings.size(); a++) {
      TileBuilding building = buildings.get(a);
      building.logicUpdate();
    }
  }

  public void render(PRenderContext renderContext) {
    for (int a = 0; a < buildings.size(); a++) {
      TileBuilding building = buildings.get(a);
      building.render(renderContext);
    }
    if (modelInstance != null) {
      modelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
