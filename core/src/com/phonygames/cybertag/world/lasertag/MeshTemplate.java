package com.phonygames.cybertag.world.lasertag;

import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.graphics.model.PModelGen;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringUtils;

import lombok.val;

public class MeshTemplate {
  private static final PMap<String, MeshTemplate> meshTemplates = new PMap<>();
  public final PList<Boolean> emitMesh = new PList<>();
  public final PList<Boolean> emitPhysics = new PList<>();
  public final PList<Boolean> isAlphaBlend = new PList<>();
  public final PList<PMesh> meshes = new PList<>();
  public final PList<Integer> vColIndexBaseOffsets = new PList<>();
  public final PList<Integer> vColIndexOffsets = new PList<>();

  private MeshTemplate(String name) {
    PModel model = PAssetManager.model(name, true);
    for (val e : model.glNodes()) {
      meshes.add(e.v().drawCall().mesh());
      String materialId = e.v().drawCall().material().id();
      // Determine the vCol index from the material.
      String vColIBaseString = PStringUtils.extract(materialId, ".vColBase", ".", true);
      vColIndexBaseOffsets.add(vColIBaseString == null ? -1 : Integer.parseInt(vColIBaseString));
      if (vColIBaseString == null) {
        String vColIString = PStringUtils.extract(materialId, ".vCol", ".", true);
        vColIndexOffsets.add(vColIString == null ? -1 : Integer.parseInt(vColIString));
      } else {
        vColIndexOffsets.add(-1);
      }
      isAlphaBlend.add(materialId.contains(".alphaBlend"));
      if (materialId.contains(".alsoStaticBody")) {
        emitPhysics.add(true);
        emitMesh.add(true);
      } else if (materialId.contains(".onlyStaticBody")) {
        emitPhysics.add(true);
        emitMesh.add(false);
      } else {
        emitPhysics.add(false);
        emitMesh.add(true);
      }
    }
  }

  public static MeshTemplate get(String name) {
    if (meshTemplates.has(name)) {
      return meshTemplates.get(name);
    }
    MeshTemplate ret = new MeshTemplate(name);
    meshTemplates.put(name, ret);
    return ret;
  }

  /**
   * @param modelGen
   * @param vertexProcessor
   * @param basePart
   * @param staticPhysicsPart
   * @param vColIndexOffset
   */
  public void emit(PModelGen modelGen, PModelGen.Part.VertexProcessor vertexProcessor, PModelGen.Part basePart,
                  PModelGen.StaticPhysicsPart staticPhysicsPart, int vColIndexOffset,
                  PList<PModelGen.Part> alphaBlendParts) {
    PVec4 temp = PVec4.obtain();
    for (int a = 0; a < this.meshes.size; a++) {
      PMesh mesh = this.meshes.get(a);
      boolean emitMesh = this.emitMesh.get(a);
      boolean emitPhysics = this.emitPhysics.get(a);
      boolean isAlphaBlend = this.isAlphaBlend.get(a);
      if (emitMesh) {
        int vColOffset = this.vColIndexOffsets.get(a);
        int vColBaseOffset = this.vColIndexBaseOffsets.get(a);
        int vColIndex = vColOffset == -1 ? (vColBaseOffset == -1 ? 0 : vColBaseOffset) : (vColIndexOffset + vColOffset);
        PModelGen.Part part;
        if (isAlphaBlend) {
          part = modelGen.addPart(basePart.name() + ".alphaBlend" + ".id" + a + "_" + vColIndexOffset + "",
                                  basePart.vertexAttributes());
          alphaBlendParts.add(part);
        } else {
          part = basePart;
        }
        part.set(PVertexAttributes.Attribute.Keys.col[0], PMesh.vColForIndex(temp, vColIndex));
        part.emit(mesh, emitPhysics ? staticPhysicsPart : null, vertexProcessor,
                  PVertexAttributes.getGLTF_UNSKINNED_NOCOLOR());
      } else if (emitPhysics && staticPhysicsPart != null) {
        staticPhysicsPart.emit(mesh, vertexProcessor);
      }
    }
    temp.free();
  }
}
