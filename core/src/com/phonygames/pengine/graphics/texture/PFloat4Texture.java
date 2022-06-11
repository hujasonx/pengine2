package com.phonygames.pengine.graphics.texture;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PMap;

import java.nio.FloatBuffer;

import lombok.Getter;

public class PFloat4Texture extends Texture implements Pool.Poolable {
  private static PMap<Integer, Pool<PFloat4Texture>> staticTexturePools = new PMap<Integer, Pool<PFloat4Texture>>() {
    @Override
    protected Object makeNew(final Integer integer) {
      return new Pool<PFloat4Texture>() {
        @Override
        public PFloat4Texture newObject() {
          return PFloat4Texture.get(integer, true);
        }
      };
    }
  };

  public static PFloat4Texture getTemp(int vec4Capacity) {
    return staticTexturePools.getOrMake(vec4Capacity).obtain();
  }

  public void freeTemp() {
    staticTexturePools.getOrMake(vec4Capacity).free(this);
  }

  private int sideLength = 0;
  private FloatBuffer floatBuffer;
  // Floats are packed into vec4s.

  @Getter
  private int vec4Capacity;

  public static PFloat4Texture get(int vec4Capacity, boolean use32bits) {
    int sideLength = (int) Math.pow(2, (int) Math.ceil(Math.log(Math.sqrt(vec4Capacity)) / Math.log(2)));
    PFloat4TextureTextureData textureData = new PFloat4TextureTextureData(sideLength, sideLength, use32bits);
    PFloat4Texture b = new PFloat4Texture(textureData, sideLength);
    b.vec4Capacity = vec4Capacity;
    textureData.owner = b;
    return b;
  }

  private PFloat4Texture(PFloat4TextureTextureData data, int sideLength) {
    super(data);
    this.sideLength = sideLength;

    this.floatBuffer = BufferUtils.newFloatBuffer(sideLength * sideLength * 4);
  }

  public void setData(float[] floats) {
    floatBuffer.rewind();
    floatBuffer.limit(floatBuffer.capacity());
    floatBuffer.put(floats);
    floatBuffer.flip();

    load(this.getTextureData());
  }

  @Override
  public void reset() {
    floatBuffer.rewind();
    floatBuffer.limit(floatBuffer.capacity());
  }

  public PFloat4Texture addData(PVec3 vec3, float w) {
    floatBuffer.put(vec3.x());
    floatBuffer.put(vec3.y());
    floatBuffer.put(vec3.z());
    floatBuffer.put(w);
    return this;
  }

  public PFloat4Texture addData(PVec4 vec4) {
    floatBuffer.put(vec4.x());
    floatBuffer.put(vec4.y());
    floatBuffer.put(vec4.z());
    floatBuffer.put(vec4.w());
    return this;
  }

  public PFloat4Texture addData(PMat4 mat4) {
    floatBuffer.put(mat4.getBackingMatrix4().val);
    return this;
  }

  public PFloat4Texture addData(float[] floats) {
    floatBuffer.put(floats);
    return this;
  }


  public PFloat4Texture addData(float r, float g, float b, float a) {
    floatBuffer.put(r);
    floatBuffer.put(g);
    floatBuffer.put(b);
    floatBuffer.put(a);
    return this;
  }

  public void dataTransferFinished() {
    floatBuffer.flip();
    load(this.getTextureData());
  }

  public int floatCapacity() {
    return floatBuffer.capacity();
  }

  public void loadRegion(int x, int y, int w, int h) {
    floatBuffer.flip();
    ((PFloat4TextureTextureData) this.getTextureData()).setSubArea(x, y, w, h);
    load(this.getTextureData());
    ((PFloat4TextureTextureData) this.getTextureData()).clearSubArea();
  }

  private static class PFloat4TextureTextureData extends FloatTextureData {
    PFloat4Texture owner;
    private boolean genedTexture = false;
    private boolean use32bits;

    // The location to sub pixels.
    private int subX, subY, subW, subH;
    private boolean shouldUseSubArea = false;

    public PFloat4TextureTextureData(int w, int h, boolean use32bits) {
      super(w, h, use32bits ? GL30.GL_RGBA32F : GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
      this.use32bits = use32bits;
    }

    public void setSubArea(int x, int y, int w, int h) {
      shouldUseSubArea = true;
      subX = x;
      subY = y;
      subW = w;
      subH = h;
    }

    public void clearSubArea() {
      shouldUseSubArea = false;
    }

    @Override
    public void consumeCustomData(int target) {
      if (owner != null) {
        if (genedTexture) {
          if (shouldUseSubArea) {
            Gdx.gl.glTexSubImage2D(target, 0, subX, subY, subW, subH, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          } else {
            Gdx.gl.glTexSubImage2D(target, 0, 0, 0, owner.sideLength, owner.sideLength, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          }
        } else {
          PAssert.isTrue(!shouldUseSubArea, "Can't use subArea unless the texture has already been created!");
          if (Gdx.app.getType() == Application.ApplicationType.Android || Gdx.app.getType() == Application.ApplicationType.iOS
              || Gdx.app.getType() == Application.ApplicationType.WebGL) {

            if (!Gdx.graphics.supportsExtension("OES_texture_float")) { throw new GdxRuntimeException("Extension OES_texture_float not supported!"); }

            // GLES and WebGL defines texture format by 3rd and 8th argument,
            // so to get a float texture one needs to supply GL_RGBA and GL_FLOAT there.
            Gdx.gl.glTexImage2D(target, 0, GL30.GL_RGBA, owner.sideLength, owner.sideLength, 0, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          } else {
            Gdx.gl.glTexImage2D(target, 0, use32bits ? GL30.GL_RGBA32F : GL30.GL_RGBA16F, owner.sideLength, owner.sideLength, 0, GL30.GL_RGBA, GL20.GL_FLOAT, owner.floatBuffer);
          }
          genedTexture = true;
        }
      }
    }
  }

  public void setUniforms(PShader shader, String name, int vecsPerInstance) {
    shader.set(name, this);
    shader.setI(name + "VecsPerI", vecsPerInstance);
  }
}
