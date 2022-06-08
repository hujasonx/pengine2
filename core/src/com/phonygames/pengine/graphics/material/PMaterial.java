package com.phonygames.pengine.graphics.material;

import com.badlogic.gdx.graphics.Texture;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.Duple;
import com.phonygames.pengine.util.PMap;

import lombok.Getter;
import lombok.val;

public class PMaterial {
  @Getter
  private final String id;

  private final PMap<String, Float> floats = new PMap<>();
  private final PMap<String, PVec1> vec1s = new PMap<String, PVec1>() {
    @Override
    public PVec1 tryDeepCopyValue(PVec1 o) {
      return o.cpy();
    }
  };
  private final PMap<String, PVec2> vec2s = new PMap<String, PVec2>() {
    @Override
    public PVec2 tryDeepCopyValue(PVec2 o) {
      return o.cpy();
    }
  };
  private final PMap<String, PVec3> vec3s = new PMap<String, PVec3>() {
    @Override
    public PVec3 tryDeepCopyValue(PVec3 o) {
      return o.cpy();
    }
  };
  private final PMap<String, PVec4> vec4s = new PMap<String, PVec4>() {
    @Override
    public PVec4 tryDeepCopyValue(PVec4 o) {
      return o.cpy();
    }
  };
  private final PMap<String, Texture> textures = new PMap<>();
  private final PMap<String, Duple<PRenderBuffer, String>> renderBufferTextures = new PMap<>();

  public PMaterial(String id) {
    this.id = id;
  }

  public float getFloat(String id) {
    return floats.get(id);
  }

  public PMaterial set(String id, float f) {
    floats.put(id, f);
    return this;
  }

  public PVec3 getVec3(PVec3 out, String id) {
    return out.set(vec3s.get(id));
  }

  public PMaterial set(String id, PVec3 vec3) {
    PVec3 applyTo = vec3s.get(id);
    if (applyTo == null) {
      vec3s.put(id, new PVec3());
    }

    applyTo.set(vec3);
    return this;
  }

  public PVec4 getVec4(PVec4 out, String id) {
    return out.set(vec4s.get(id));
  }

  public PMaterial set(String id, PVec4 vec4) {
    PVec4 applyTo = vec4s.get(id);
    if (applyTo == null) {
      vec4s.put(id, new PVec4());
    }

    applyTo.set(vec4);
    return this;
  }

  public PMaterial copy(String newName) {
    PMaterial ret = new PMaterial(newName);
    ret.vec1s.tryDeepCopyFrom(vec1s);
    ret.vec2s.tryDeepCopyFrom(vec2s);
    ret.vec3s.tryDeepCopyFrom(vec3s);
    ret.vec4s.tryDeepCopyFrom(vec4s);
    return ret;
  }

  public void applyUniforms(PShader shader) {
    for (val e : vec1s) {
      shader.set(e.getKey(), e.getValue());
    }
    for (val e : vec2s) {
      shader.set(e.getKey(), e.getValue());
    }
    for (val e : vec3s) {
      shader.set(e.getKey(), e.getValue());
    }
    for (val e : vec4s) {
      shader.set(e.getKey(), e.getValue());
    }
  }
}
