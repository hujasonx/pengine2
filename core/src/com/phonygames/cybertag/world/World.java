package com.phonygames.cybertag.world;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class World {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PModelInstance> modelInstances = new PStringMap<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PModel> models = new PStringMap<>();
  private PModelInstance testBoxModelInstance;
  private PModel worldModel;

  public World() {
    LasertagWorldGen worldGen = new LasertagWorldGen();
    worldGen.gen(new LasertagWorldGen.OnFinishedCallback() {
      @Override public void onFinished(PModel model) {
        worldModel = model;
        testBoxModelInstance = new PModelInstance(worldModel);
        testBoxModelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
      }
    });
  }

  public void render(PRenderContext renderContext) {
    if (testBoxModelInstance != null) {
      testBoxModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
