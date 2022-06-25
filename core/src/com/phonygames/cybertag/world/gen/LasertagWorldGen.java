package com.phonygames.cybertag.world.gen;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.cybertag.world.LasertagWorldBuilding;
import com.phonygames.cybertag.world.LasertagWorldRoom;
import com.phonygames.cybertag.world.World;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PGltf;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;

import lombok.val;

public class LasertagWorldGen {
  private static final PMap<String, PMap<String, Float>> styleCompatibilities =
      new PMap<String, PMap<String, Float>>() {
        @Override public PMap<String, Float> newUnpooled(String key) {
          return new PMap<>();
        }
      };
  private final Context context = new Context();
  private final World world;
  private boolean wasGenned = false;

  public LasertagWorldGen(World world) {
    this.world = world;
  }

  public void gen(@NonNull final OnFinishedCallback onFinishedCallback) {
    PAssert.isFalse(wasGenned);
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
      PModelGen.Part basePart;
      PModelGen.StaticPhysicsPart basePhysicsPart;

      @Override protected void modelIntro() {
        basePart = addPart("base", new PVertexAttributes(
            new VertexAttribute[]{PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos),
                                  PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.nor)}));
        basePhysicsPart = addStaticPhysicsPart("base");
      }

      @Override protected void modelMiddle() {
        LasertagWorldGenBuilding b = new LasertagWorldGenBuilding(0, 20, 3, 20, 3, 4, 3, 300);
        b.generateRoomData();
        b.emit(this, context);
      }

      @Override protected void modelEnd() {
        PList<PGlNode> glNodes = new PList<>();
        PModel.Builder builder = new PModel.Builder();
        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null), null, PGltf.Layer.PBR);
        // Go through the roomPartData and generate buildings and rooms.
        for (val e : context.roomPartData) {
          int buildingIndex = e.k();
          LasertagWorldBuilding worldBuilding = new LasertagWorldBuilding(buildingIndex);
          world.buildings().add(worldBuilding);
          for (val e2 : e.v()) {
            int roomIndex = e2.k();
            LasertagWorldRoom worldRoom =
                new LasertagWorldRoom(worldBuilding, roomIndex, context.curVColIndexLength);
            worldBuilding.rooms().add(worldRoom);
            RoomPartData data = e2.v();
            for (RoomPartData.Part e3 : data.modelgenParts) {
              PVec3 partCenter = e3.origin;
              PModelGen.Part part = e3.part;
              // TODO: use a different layer for alphablend parts.
              chainGlNode(glNodes, part, e3.material, null, e3.layer);
            }
          }
        }
        emitStaticPhysicsPartIntoModelBuilder(builder);
        builder.addNode("lasertagworld", null, glNodes, PMat4.IDT);
        onFinishedCallback.onFinished(builder.build());
        wasGenned = true;
      }
    });
  }

  public static class Context {
    private final PMap<Integer, PMap<Integer, RoomPartData>> roomPartData =
        new PMap<Integer, PMap<Integer, RoomPartData>>() {
          @Override public PMap<Integer, RoomPartData> newUnpooled(Integer k) {
            return new PMap<>();
          }
        };
    private int curVColIndex = 0;
    private int curVColIndexLength = 16;

    public RoomPartData addRoomPartData(int buildingIndex, int roomIndex) {
      RoomPartData ret = new RoomPartData(buildingIndex, roomIndex, curVColIndex, curVColIndexLength);
      curVColIndex += curVColIndexLength;
      roomPartData.genUnpooled(buildingIndex).put(roomIndex, ret);
      return ret;
    }
  }

  public static abstract class OnFinishedCallback {
    public abstract void onFinished(PModel model);
  }

  /**
   * Helper class that keeps track of room data.
   */
  public static class RoomPartData {
    public final int buildingIndex;
    // Center location (for sorting alphablend), modelgenPart, layer.
    public final PSet<Part> modelgenParts = new PSet<>();
    public final PSet<PModelGen.StaticPhysicsPart> modelgenStaticPhysicsParts = new PSet<>();
    public final int roomIndex, vColIndex, vColIndexLength;

    private RoomPartData(int buildingIndex, int roomIndex, int vColIndex, int vColIndexLength) {
      this.buildingIndex = buildingIndex;
      this.roomIndex = roomIndex;
      this.vColIndex = vColIndex;
      this.vColIndexLength = vColIndexLength;
    }

    public static class Part {
      final String layer;
      final PMaterial material;
      final PVec3 origin;
      final PModelGen.Part part;

      public Part(@NonNull PVec3 origin, @NonNull PModelGen.Part part, @NonNull String layer,
                  @NonNull PMaterial material) {
        this.origin = origin;
        this.part = part;
        this.layer = layer;
        this.material = material;
      }
    }
  }
}
