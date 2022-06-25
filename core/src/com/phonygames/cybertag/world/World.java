package com.phonygames.cybertag.world;

import com.phonygames.cybertag.world.gen.LasertagWorldGen;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.util.PList;
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
  private PModelInstance testWorldModelInstance;
  private PModel worldModel;

  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PList<LasertagWorldBuilding> buildings = new PList<>();

  public World() {
    LasertagWorldGen worldGen = new LasertagWorldGen(this);
    worldGen.gen(new LasertagWorldGen.OnFinishedCallback() {
      @Override public void onFinished(PModel model) {
        worldModel = model;
        testWorldModelInstance = new PModelInstance(worldModel);
        testWorldModelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
        testWorldModelInstance.setDataBufferEmitter(new PRenderContext.DataBufferEmitter() {
          @Override public void emitDataBuffersInto(PRenderContext renderContext) {
            PFloat4Texture vColIndexBuffer = renderContext.genDataBuffer("vColIndex");
            for (LasertagWorldBuilding building :  buildings()) {
              building.outputColorData(vColIndexBuffer);
            }
          }
        });
      }
    });
  }

  public void frameUpdate() {
    for (LasertagWorldBuilding building :  buildings()) {
      building.frameUpdate();
    }
  }

  public void render(PRenderContext renderContext) {
    if (testWorldModelInstance != null) {
      testWorldModelInstance.enqueue(renderContext, PGltf.DEFAULT_SHADER_PROVIDER);
    }
  }
}
