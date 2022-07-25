package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PInt;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;

import java.nio.FloatBuffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PFloat4Texture extends Texture implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PMap<PInt, PPool<PFloat4Texture>> staticTexturePools =
      new PMap<PInt, PPool<PFloat4Texture>>() {
        @Override protected PPool<PFloat4Texture> newUnpooled(final PInt integer) {
          final int vec4capacity = integer.valueOf();
          return new PPool<PFloat4Texture>() {
            @Override public PFloat4Texture newObject() {
              PFloat4Texture newO = PFloat4Texture.get(vec4capacity, true);
              return newO;
            }
          };
        }
      };
  private static PFloat4Texture WHITE_PIXEL;
  private boolean dataDirty = false;
  private FloatBuffer floatBuffer;
  private boolean locked = false;
  private int sideLength = 0;
  // Floats are packed into vec4s.
  @Getter
  private int vec4Capacity;
  private static final PInt tempPInt = PInt.obtain();

  private PFloat4Texture(PFloat4TextureTextureData data, int sideLength) {
    super(data);
    this.sideLength = sideLength;
    this.floatBuffer = BufferUtils.newFloatBuffer(sideLength * sideLength * 4);
    reset();
  }

  @Override public void reset() {
    markDirty();
    floatBuffer.clear();
  }

  private void markDirty() {
    PAssert.isFalse(locked);
    dataDirty = true;
  }

  public static PFloat4Texture getTemp(int vec4Capacity) {
    return staticTexturePools().genUnpooled(tempPInt.set(vec4Capacity)).obtain();
  }

  public PFloat4Texture addData(PVec3 vec3, float w) {
    markDirty();
    floatBuffer.put(vec3.x());
    floatBuffer.put(vec3.y());
    floatBuffer.put(vec3.z());
    floatBuffer.put(w);
    return this;
  }

  public PFloat4Texture addData(PVec4 vec4) {
    markDirty();
    floatBuffer.put(vec4.x());
    floatBuffer.put(vec4.y());
    floatBuffer.put(vec4.z());
    floatBuffer.put(vec4.w());
    return this;
  }

  public PFloat4Texture addData(PMat4 mat4) {
    markDirty();
    floatBuffer.put(mat4.getBackingMatrix4().val);
    return this;
  }

  public PFloat4Texture addData(float[] floats) {
    markDirty();
    floatBuffer.put(floats);
    return this;
  }

  public void applyShader(PShader shader, String name, int lookupOffset, int vecsPerInstance) {
    String uniform = PStringUtils.concat("u_", PStringUtils.concat(name, "Tex"));
    load();
    shader.setWithUniform(uniform, this);
    shader.setI(PStringUtils.concat(uniform, "LookupOffset"), lookupOffset);
    shader.setI(PStringUtils.concat(uniform, "VecsPerI"), vecsPerInstance);
  }

  public void load() {
    if (!dataDirty) {
      return;
    }
    dataDirty = false;
    floatBuffer.flip();
    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0); // Bind the zero texture, as we have to bind the texture.
    load(this.getTextureData());
  }

  public void freeTemp() {
    PAssert.isFalse(this == getWHITE_PIXEL());
    staticTexturePools().genUnpooled(tempPInt.set(tempPInt)).free(this);
  }

  public static PFloat4Texture getWHITE_PIXEL() {
    if (WHITE_PIXEL == null) {
      WHITE_PIXEL = genWHITE_PIXEL();
    }
    return WHITE_PIXEL;
  }

  public static PFloat4Texture genWHITE_PIXEL() {
    PFloat4Texture f = PFloat4Texture.get(1, true).addData(1, 1, 1, 1).lock();
    f.load();
    return f;
  }

  public PFloat4Texture lock() {
    locked = true;
    return this;
  }

  public PFloat4Texture addData(float r, float g, float b, float a) {
    markDirty();
    floatBuffer.put(r);
    floatBuffer.put(g);
    floatBuffer.put(b);
    floatBuffer.put(a);
    return this;
  }

  public static PFloat4Texture get(int vec4Capacity, boolean use32bits) {
    int sideLength = (int) Math.pow(2, (int) Math.ceil(Math.log(Math.sqrt(vec4Capacity)) / Math.log(2)));
    PFloat4TextureTextureData textureData = new PFloat4TextureTextureData(sideLength, sideLength, use32bits);
    PFloat4Texture b = new PFloat4Texture(textureData, sideLength);
    b.vec4Capacity = vec4Capacity;
    textureData.owner = b;
    return b;
  }

  public void loadIntoRegion(int x, int y, int w, int h) {
    if (!dataDirty) {
      return;
    }
    dataDirty = false;
    floatBuffer.flip();
    ((PFloat4TextureTextureData) this.getTextureData()).setSubArea(x, y, w, h);
    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
    load(this.getTextureData());
    ((PFloat4TextureTextureData) this.getTextureData()).clearSubArea();
  }

  public PFloat4Texture unlock() {
    locked = false;
    return this;
  }

  public int vecsWritten() {
    return floatBuffer.limit() == floatBuffer.capacity() ? floatBuffer.position() / 4 : floatBuffer.limit() / 4;
  }

  private static class PFloat4TextureTextureData extends FloatTextureData {
    PFloat4Texture owner;
    private boolean genedTexture = false;
    private boolean shouldUseSubArea = false;
    // The location to sub pixels.
    private int subX, subY, subW, subH;
    private boolean use32bits;

    public PFloat4TextureTextureData(int w, int h, boolean use32bits) {
      super(w, h, use32bits ? GL30.GL_RGBA32F : GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
      this.use32bits = use32bits;
    }

    public void clearSubArea() {
      shouldUseSubArea = false;
    }

    @Override public void consumeCustomData(int target) {
      if (owner != null) {
        if (genedTexture) {
          if (shouldUseSubArea) {
            Gdx.gl.glTexSubImage2D(target, 0, subX, subY, subW, subH, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          } else {
            // Still subImage this shit, to speed things up.
            int rowsUsed = Math.min(owner.vecsWritten() / owner.sideLength + 1, owner.sideLength);
            int colsUsed = owner.sideLength;
            if (rowsUsed == 1) {
              colsUsed = owner.vecsWritten();
            }
            Gdx.gl.glTexSubImage2D(target, 0, 0, 0, colsUsed, rowsUsed, GL30.GL_RGBA, GL20.GL_FLOAT,
                                   owner.floatBuffer);
          }
        } else {
          PAssert.isTrue(!shouldUseSubArea, "Can't use subArea unless the texture has already been created!");
          if (Gdx.app.getType() == Application.ApplicationType.Android ||
              Gdx.app.getType() == Application.ApplicationType.iOS ||
              Gdx.app.getType() == Application.ApplicationType.WebGL) {
            if (!Gdx.graphics.supportsExtension("OES_texture_float")) {
              throw new GdxRuntimeException("Extension OES_texture_float not supported!");
            }
            // GLES and WebGL defines texture format by 3rd and 8th argument,
            // so to get a float texture one needs to supply GL_RGBA and GL_FLOAT there.
            Gdx.gl.glTexImage2D(target, 0, GL30.GL_RGBA, owner.sideLength, owner.sideLength, 0, GL30.GL_RGBA,
                                GL20.GL_FLOAT, owner.floatBuffer);
          } else {
            Gdx.gl.glTexImage2D(target, 0, use32bits ? GL30.GL_RGBA32F : GL30.GL_RGBA16F, owner.sideLength,
                                owner.sideLength, 0, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          }
          genedTexture = true;
        }
      }
    }

    public void setSubArea(int x, int y, int w, int h) {
      shouldUseSubArea = true;
      subX = x;
      subY = y;
      subW = w;
      subH = h;
    }
  }
}
