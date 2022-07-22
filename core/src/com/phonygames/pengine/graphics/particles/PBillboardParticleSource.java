//package com.phonygames.pengine.graphics.particles;
//
//import com.badlogic.gdx.graphics.GL20;
//import com.badlogic.gdx.graphics.Mesh;
//import com.badlogic.gdx.graphics.VertexAttribute;
//import com.badlogic.gdx.graphics.VertexAttributes;
//import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
//import com.badlogic.gdx.math.Vector3;
//import com.phonygames.cybertag.world.ColorDataEmitter;
//import com.phonygames.cybertag.world.lasertag.LasertagRoomGenTileEmitter;
//import com.phonygames.cybertag.world.lasertag.LasertagTile;
//import com.phonygames.pengine.graphics.material.PMaterial;
//import com.phonygames.pengine.graphics.model.PGlNode;
//import com.phonygames.pengine.graphics.model.PGltf;
//import com.phonygames.pengine.graphics.model.PMesh;
//import com.phonygames.pengine.graphics.model.PModel;
//import com.phonygames.pengine.graphics.model.PModelGen;
//import com.phonygames.pengine.graphics.model.PModelInstance;
//import com.phonygames.pengine.graphics.model.PVertexAttributes;
//import com.phonygames.pengine.math.PMat4;
//import com.phonygames.pengine.util.PList;
//
//import java.nio.Buffer;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Set;
//
//import lombok.val;
//
//public class PBillboardParticleSource {
//  private PModel model;
//  private PModelInstance modelInstance;
//  private static Boolean modelGenStarted = false;
//  private boolean useAlphaBlend = true;
//  private static int maxCapacity = 1000;
//  // 4 x 3 Position, 4 x 2 UV, 4 x 4 color. Only one color per billboard.
//  public static final int FLOATS_PER_PARTICLE = 12 + 8 + 16;
//  private final float[] vertices;
//  private PMesh mesh;
//  public final String texture;
//
//  // The index to insert the next particle into the vertices buffer at.
//  private int currentBufferIndex = 0;
//
//
//  public PBillboardParticleSource(int capacity, String textureName) {
//    vertices = new float[FLOATS_PER_PARTICLE * capacity];
//    this.texture = textureName;
//  }
//
//  private void makeModelIfNeeded() {
//    if (modelInstance != null) {
//      return;
//    }
//
//    mesh = new PMesh(false, maxCapacity * 4, maxCapacity * 6, PVertexAttributes.getBILLBOARD_PARTICLE());
//
//    int len = maxCapacity * 6;
//    short[] indices = new short[len];
//    short j = 0;
//    for (int i = 0; i < len; i += 6, j += 4) {
//      indices[i + 0] = (short) (j + 0);
//      indices[i + 1] = (short) (j + 2);
//      indices[i + 2] = (short) (j + 1);
//      indices[i + 3] = (short) (j + 2);
//      indices[i + 4] = (short) (j + 0);
//      indices[i + 5] = (short) (j + 3);
//    }
//    mesh.setIndices(indices);
//
//    ModelBuilder modelBuilder = new ModelBuilder();
//    modelBuilder.begin();
//
//
//    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
//      PList<Part> alphaBlendParts = new PList<>();
//      Part basePart;
//
//      @Override protected void modelIntro() {
//        basePart = addPart("base", PVertexAttributes.getBILLBOARD_PARTICLE());
//      }
//
//      @Override protected void modelMiddle() {
//      }
//
//      @Override protected void modelEnd() {
//        PList<PGlNode> glNodes = new PList<>();
//        PModel.Builder builder = new PModel.Builder();
//        chainGlNode(glNodes, basePart, new PMaterial(basePart.name(), null).useVColIndex(true), null, PGltf.Layer.PBR,
//                    true, false);
//        emitStaticPhysicsPartIntoModelBuilder(builder);
//        builder.addNode(basePart.name(), null, glNodes, PMat4.IDT);
//        for (int a = 0; a < alphaBlendParts.size(); a++) {
//          Part part = alphaBlendParts.get(a);
//          glNodes.clear();
//          chainGlNode(glNodes, part, new PMaterial(part.name(), null).useVColIndex(true), null, PGltf.Layer.AlphaBlend,
//                      true, true);
//          builder.addNode(part.name(), null, glNodes, PMat4.IDT);
//        }
//        lasertagRoom.modelInstance = new PModelInstance(builder.build());
//        lasertagRoom.initialized = true;
//        // Create the color data emitter buffer.
//        int numVCols = lasertagRoom.numBaseVCols;
//        try (val it = lasertagRoom.tiles().obtainIterator()) {
//          while (it.hasNext()) {
//            val e = it.next();
//            numVCols += LasertagTile.PER_TILE_VCOL_INDICES;
//          }
//        }
//        lasertagRoom.colorDataEmitter = new ColorDataEmitter(numVCols);
//        lasertagRoom.modelInstance.createAndAddStaticBodiesFromModelWithCurrentWorldTransform();
//      }
//    });
//  }
//
//  public void clear() {
//    ((Buffer) mesh.getIndicesBuffer()).position(0);
//    ((Buffer) mesh.getIndicesBuffer()).limit(0);
//  }
//
//  @Override
//  public void logicUpdate() {
//
//  }
//
//  @Override
//  protected void ensureModelInit() {
//    makeModelIfNeeded();
//  }
//
//  public boolean useAlphaBlend() {
//    return useAlphaBlend;
//  }
//
//  public void setUseAlphaBlend(boolean useAlphaBlend) {
//    this.useAlphaBlend = useAlphaBlend;
//    updateMaterialInternal();
//  }
//
//  private void updateMaterialInternal() {
//  }
//
//  // This should spawn new particles.
//  @Override
//  public void spawnParticles() {
//  }
//
//
//  private final Vector3 tempNormal = new Vector3();
//  private final Vector3 tempTangent = new Vector3();
//  private final Vector3 tempBitangent = new Vector3();
//  private final Vector3 tempXChange = new Vector3();
//  private final Vector3 tempYChange = new Vector3();
//  private final Vector3 tempPosOv = new Vector3();
//
//  @Override
//  public void addParticleDataToBuffer(PBillboardParticle particle, float[] bufferData) {
//  }
//
//  private void setVerticesForParticle(PBillboardParticle particle) {
//    if (currentBufferIndex >= vertices.length) {
//      return;
//    }
//
//    if (particle.useVertexPositionOverrides()) {
//      particle.getManualPositionOverride(tempPosOv, 0);
//      vertices[currentBufferIndex + 0] = tempPosOv.x; // x0;
//      vertices[currentBufferIndex + 1] = tempPosOv.y; // y0;
//      vertices[currentBufferIndex + 2] = tempPosOv.z; // z0;
//
//      particle.getManualPositionOverride(tempPosOv, 1);
//      vertices[currentBufferIndex + 9] = tempPosOv.x; // x1;
//      vertices[currentBufferIndex + 10] = tempPosOv.y; // y1;
//      vertices[currentBufferIndex + 11] = tempPosOv.z; // z1;
//
//      particle.getManualPositionOverride(tempPosOv, 2);
//      vertices[currentBufferIndex + 18] = tempPosOv.x; // x2;
//      vertices[currentBufferIndex + 19] = tempPosOv.y; // y2;
//      vertices[currentBufferIndex + 20] = tempPosOv.z; // z2;
//
//      particle.getManualPositionOverride(tempPosOv, 3);
//      vertices[currentBufferIndex + 27] = tempPosOv.x; // x3;
//      vertices[currentBufferIndex + 28] = tempPosOv.y; // y3;
//      vertices[currentBufferIndex + 29] = tempPosOv.z; // z3;
//
//    } else {
//      particle.getNormal(tempNormal);
//      particle.getTangent(tempTangent);
//      if (particle.alwaysFaceCamera()) {
//        tempNormal.set(PRenderer3d.getCamera().direction).scl(-1).nor();
//        tempTangent.set(PRenderer3d.getCamera().up).nor();
//      }
//
//      tempBitangent.set(tempTangent).crs(tempNormal);
//      float rotation = particle.getRotation();
//      PMx.rotate(tempBitangent, tempNormal, rotation);
//      PMx.rotate(tempTangent, tempNormal, rotation);
//
//      tempXChange.set(tempBitangent).scl(particle.getScaleX() * .5f);
//      tempYChange.set(tempTangent).scl(particle.getScaleY() * .5f);
//
//      tempPosOv.set(particle.position).sub(tempXChange).sub(tempYChange);
//      vertices[currentBufferIndex + 0] = tempPosOv.x; // x0;
//      vertices[currentBufferIndex + 1] = tempPosOv.y; // y0;
//      vertices[currentBufferIndex + 2] = tempPosOv.z; // z0;
//
//      tempPosOv.set(particle.position).sub(tempXChange).add(tempYChange);
//      vertices[currentBufferIndex + 9] = tempPosOv.x; // x1;
//      vertices[currentBufferIndex + 10] = tempPosOv.y; // y1;
//      vertices[currentBufferIndex + 11] = tempPosOv.z; // z1;
//
//      tempPosOv.set(particle.position).add(tempXChange).add(tempYChange);
//      vertices[currentBufferIndex + 18] = tempPosOv.x; // x2;
//      vertices[currentBufferIndex + 19] = tempPosOv.y; // y2;
//      vertices[currentBufferIndex + 20] = tempPosOv.z; // z2;
//
//      tempPosOv.set(particle.position).add(tempXChange).sub(tempYChange);
//      vertices[currentBufferIndex + 27] = tempPosOv.x; // x3;
//      vertices[currentBufferIndex + 28] = tempPosOv.y; // y3;
//      vertices[currentBufferIndex + 29] = tempPosOv.z; // z3;
//    }
//
//    vertices[currentBufferIndex + 3] = particle.getTint().r; // c0;
//    vertices[currentBufferIndex + 4] = particle.getTint().g; // c0;
//    vertices[currentBufferIndex + 5] = particle.getTint().b; // c0;
//    vertices[currentBufferIndex + 6] = particle.getTint().a; // c0;
//    vertices[currentBufferIndex + 7] = particle.getTextureRegion().getU(); // u0;
//    vertices[currentBufferIndex + 8] = 1 - particle.getTextureRegion().getV(); // v0;
//
//
//    vertices[currentBufferIndex + 12] = particle.getTint().r; // c0;
//    vertices[currentBufferIndex + 13] = particle.getTint().g; // c0;
//    vertices[currentBufferIndex + 14] = particle.getTint().b; // c0;
//    vertices[currentBufferIndex + 15] = particle.getTint().a; // c0;
//    vertices[currentBufferIndex + 16] = particle.getTextureRegion().getU(); // u0;
//    vertices[currentBufferIndex + 17] = 1 - particle.getTextureRegion().getV2(); // v1;
//
//
//    vertices[currentBufferIndex + 21] = particle.getTint().r; // c0;
//    vertices[currentBufferIndex + 22] = particle.getTint().g; // c0;
//    vertices[currentBufferIndex + 23] = particle.getTint().b; // c0;
//    vertices[currentBufferIndex + 24] = particle.getTint().a; // c0;
//    vertices[currentBufferIndex + 25] = particle.getTextureRegion().getU2(); // u1;
//    vertices[currentBufferIndex + 26] = 1 - particle.getTextureRegion().getV2(); // v1;
//
//
//    vertices[currentBufferIndex + 30] = particle.getTint().r; // c0;
//    vertices[currentBufferIndex + 31] = particle.getTint().g; // c0;
//    vertices[currentBufferIndex + 32] = particle.getTint().b; // c0;
//    vertices[currentBufferIndex + 33] = particle.getTint().a; // c0;
//    vertices[currentBufferIndex + 34] = particle.getTextureRegion().getU2(); // u1;
//    vertices[currentBufferIndex + 35] = 1 - particle.getTextureRegion().getV(); // v0;
//
//    //    bufferData[8] = particle.getTextureRegion().getU();
//    //    bufferData[9] = particle.getTextureRegion().getU2();
//    //    bufferData[10] = 1 - particle.getTextureRegion().getV2();
//    //    bufferData[11] = 1 - particle.getTextureRegion().getV();
//    currentBufferIndex += 36;
//  }
//
//
//  @Override
//  public void render3d() {
//    ensureModelInit();
//    if (modelInstance == null) {
//      return;
//    }
//
//    if (particles.size() == 0) {
//      return;
//    }
//
//    if (shouldSortParticles) {
//      Collections.sort(particles);
//    }
//
//    currentBufferIndex = 0;
//
//    for (int a = 0; a < particles.size(); a++) {
//      PBillboardParticle p = particles.get(a);
//      if (p.isLive()) {
//        setVerticesForParticle(p);
//      }
//    }
//
//    if (currentBufferIndex == 0) {
//      return;
//    }
//    mesh.setVertices(vertices, 0, currentBufferIndex);
//    ((Buffer) mesh.getIndicesBuffer()).position(0);
//    ((Buffer) mesh.getIndicesBuffer()).limit(particles.size() * 6);
//
//    PRenderer3d.currentModelBatch().enqueue(modelInstance);
//  }
//}
