package com.phonygames.pengine.graphics.model;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.util.collection.PList;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PPooledIterable;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.Builder;

/**
 * A special form of PModel that is designed to be used by a model gen to emit copies.
 * <p>
 * Materials should be named in blender as follows: [sp=name] [op:name] [ab:name] At least one of the above must be
 * included, but multiple can be included as well. staticphysics parts will be combined if they share name. opaque parts
 * will be combined if they share name. If they do, they MUST have the same vertex attributes! alphablend parts will
 * never be combined.
 * <p>
 * [matid=*] The material id. Optional.
 * <p>
 * [vcio=*] The name used to lookup the vColIndexOffset. Optional.
 * <p>
 * [dup] If this is specified, each time this template is emitted, create a new part with a new material and mesh.
 */
public class PModelGenTemplate {
  private final PModel model;
  private final PList<Part> parts = new PList<>();
  /** The vColIndexOffsets map to use. Should only be used temporarily to allow parts to access it during emitting. */
  private PStringMap<PInt> __tmpVColIndexOffsets;

  public PModelGenTemplate(PModel model) {
    this.model = model;
    __genFromModel();
  }

  /** Sets up this template from the model. */
  private void __genFromModel() {
    try (PPooledIterable.PPoolableIterator<PMap.Entry<String, PGlNode>> it = model.glNodes().obtainIterator()) {
      while (it.hasNext()) {
        PMap.Entry<String, PGlNode> e = it.next();
        String originalMaterialId = e.v().drawCall().material().id();
        // Split the material id into parts separated by square brackets.
        String[] matIdSplit = originalMaterialId.split("[\\[\\]]");
        String opaqueName = dataForMaterialType(matIdSplit, "op");
        String physicsName = dataForMaterialType(matIdSplit, "sp");
        String alphaBlendName = dataForMaterialType(matIdSplit, "ab");
        String matId = dataForMaterialType(matIdSplit, "matid");
        String vcio = dataForMaterialType(matIdSplit, "vcio");
        boolean dup = hasParamInMaterial(matIdSplit, "dup");
        if (physicsName != null) {
          Part.PartBuilder partBuilder = Part.builder().ownerTemplate(this);
          partBuilder.baseName(physicsName);
          partBuilder.mesh(e.v().drawCall().mesh());
          partBuilder.type(Part.Type.STATICPHYSICS);
          parts.add(partBuilder.build());
        }
        if (opaqueName != null) {
          Part.PartBuilder partBuilder = Part.builder().ownerTemplate(this);
          partBuilder.baseName(opaqueName);
          partBuilder.matid(matId);
          partBuilder.mesh(e.v().drawCall().mesh());
          partBuilder.type(Part.Type.OPAQUE);
          partBuilder.vColIndexOffsetName(vcio);
          partBuilder.dup(dup);
          parts.add(partBuilder.build());
        }
        if (alphaBlendName != null) {
          Part.PartBuilder partBuilder = Part.builder().ownerTemplate(this);
          partBuilder.baseName(alphaBlendName);
          partBuilder.matid(matId);
          partBuilder.mesh(e.v().drawCall().mesh());
          partBuilder.type(Part.Type.ALPHABLEND);
          partBuilder.vColIndexOffsetName(vcio);
          partBuilder.dup(dup);
          parts.add(partBuilder.build());
        }
        System.out.println("Genfrommodel " + e.k());//TODO: actually implement this shit
      }
    }
  }

  /** If the materialIdParts contains the data with type, returns the value. E.g. [a=b] if type == a, then return b. */
  private static @Nullable String dataForMaterialType(String[] materialIdParts, String type) {
    for (int a = 0; a < materialIdParts.length; a++) {
      String s = materialIdParts[a];
      String[] splitByEquals = s.split("=");
      if (splitByEquals.length != 2) {continue;}
      if (splitByEquals[0].equals(type)) {
        return splitByEquals[1];
      }
    }
    return null;
  }

  /** Return true if the material id has the param. */
  private static boolean hasParamInMaterial(String[] materialIdParts, String param) {
    for (int a = 0; a < materialIdParts.length; a++) {
      if (materialIdParts[a].equals(param)) {
        return true;
      }
    }
    return false;
  }

  /** Emits this template to the modelGen using the given vertex processor. */
  public void emit(PModelGen modelGen, @Nullable PMeshGenVertexProcessor vertexProcessor,
                   @Nullable PStringMap<PInt> vColIndexOffsets) {
    __tmpVColIndexOffsets = vColIndexOffsets;
    for (int a = 0; a < parts.size(); a++) {
      parts.get(a).emit(modelGen, vertexProcessor);
    }
    __tmpVColIndexOffsets = null;
  }

  @Builder private static class Part {
    /** The base name of the part. */
    private final String baseName;
    /** The material id. */
    private final @Nullable
    String matid;
    /** The backing mesh of this part. */
    private final PMesh mesh;
    /** The template that owns this part. */
    private final PModelGenTemplate ownerTemplate;
    /** Whether or not to produce duplicate parts each time this is emitted. Parts will be named with dedup suffixes. */
    @Builder.Default
    private final boolean dup = false;
    private final Type type;
    /** The name used to retrieve vColIndex offsets from the offsetMap. */
    private final @Nullable
    String vColIndexOffsetName;
    // Used to modify the vColIndices based on the offsets.
    private final PMeshGen.FinalPassVertexProcessor finalPassVertexProcessor = new PMeshGen.FinalPassVertexProcessor() {
      @Override public void process(float[] vertexFloats) {
        PVertexAttribute vColIPVA = mesh.vertexAttributes().pva(PVertexAttribute.Definitions.vColI.alias);
        if (vColIPVA != null && ownerTemplate.__tmpVColIndexOffsets != null) {
          // Shift vColIndex based on the offsets stored in __tmpVColIndexOffsets.
          vertexFloats[vColIPVA.offsetInOwnerFloats()] +=
              ownerTemplate.__tmpVColIndexOffsets.genPooled(vColIndexOffsetName).valueOf();
        }
      }
    };

    /** Emits this part to the modelGen. */
    private void emit(PModelGen modelGen, @Nullable PMeshGenVertexProcessor vertexProcessor) {
      switch (type) {
        case STATICPHYSICS:
          emitStaticPhysics(modelGen, vertexProcessor);
          break;
        case OPAQUE:
          emitOpaque(modelGen, vertexProcessor);
          break;
        case ALPHABLEND:
          emitAlphaBlend(modelGen, vertexProcessor);
          break;
        default:
          PAssert.fail("Should not reach!");
          break;
      }
    }

    /** Emits this part as a static physics part to the modelGen. */
    private void emitStaticPhysics(PModelGen modelGen, @Nullable PMeshGenVertexProcessor vertexProcessor) {
      PMeshGen meshGen = modelGen.getOrAddStaticPhysicsMesh(baseName);
      meshGen.vertexProcessor(vertexProcessor);
      meshGen.addMeshCopy(mesh);
      meshGen.vertexProcessor(null);
    }

    /** Emits this part as an opaque part to the modelGen. */
    private void emitOpaque(PModelGen modelGen, @Nullable PMeshGenVertexProcessor vertexProcessor) {
      PMeshGen meshGen;
      if (dup) {
        meshGen = modelGen.addDupMeshGen(baseName, mesh.vertexAttributes());
      } else {
        meshGen = modelGen.getOrAddMeshGen(baseName, mesh.vertexAttributes());
      }
      meshGen.finalPassVertexProcessor(finalPassVertexProcessor);
      meshGen.vertexProcessor(vertexProcessor);
      meshGen.addMeshCopy(mesh);
      meshGen.vertexProcessor(null);
      meshGen.finalPassVertexProcessor(null);
    }

    /** Emits this part as an opaque part to the modelGen. */
    private void emitAlphaBlend(PModelGen modelGen, @Nullable PMeshGenVertexProcessor vertexProcessor) {
      PMeshGen meshGen;
      if (dup) {
        meshGen = modelGen.addDupMeshGen(baseName, mesh.vertexAttributes());
      } else {
        meshGen = modelGen.getOrAddMeshGen(baseName, mesh.vertexAttributes());
      }
      meshGen.alphaBlend(true);
      meshGen.finalPassVertexProcessor(finalPassVertexProcessor);
      meshGen.vertexProcessor(vertexProcessor);
      meshGen.addMeshCopy(mesh);
      meshGen.vertexProcessor(null);
      meshGen.finalPassVertexProcessor(null);
    }

    enum Type {
      STATICPHYSICS, OPAQUE, ALPHABLEND;
    }
  }
}
