package com.phonygames.pengine.graphics.material;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PMap;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

import lombok.Getter;
import lombok.val;

public class PMaterial {
  @Getter
  private final String id;

  private final PMap<String, PVec1> vec1s = new PMap<String, PVec1>() {
    @Override
    public PVec1 tryDeepCopyValue(PVec1 o) {
      return o.cpy();
    }

    @Override
    protected Object makeNew(String s) {
      return new PVec1();
    }
  };

  private final PMap<String, PVec2> vec2s = new PMap<String, PVec2>() {
    @Override
    public PVec2 tryDeepCopyValue(PVec2 o) {
      return o.cpy();
    }

    @Override
    protected Object makeNew(String s) {
      return new PVec2();
    }
  };

  private final PMap<String, PVec3> vec3s = new PMap<String, PVec3>() {
    @Override
    public PVec3 tryDeepCopyValue(PVec3 o) {
      return o.cpy();
    }

    @Override
    protected Object makeNew(String s) {
      return new PVec3();
    }
  };

  private final PMap<String, PVec4> vec4s = new PMap<String, PVec4>() {
    @Override
    public PVec4 tryDeepCopyValue(PVec4 o) {
      return o.cpy();
    }

    @Override
    protected Object makeNew(String s) {
      return new PVec4();
    }
  };

  private final PMap<String, PTexture> textures = new PMap<String, PTexture>() {
    @Override
    public PTexture tryDeepCopyValue(PTexture o) {
      return o.tryDeepCopy();
    }

    @Override
    protected Object makeNew(String s) {
      return new PTexture();
    }
  };

  public static class UniformConstants {

    public static class Vec2 {
      public static final String u_metallicRoughness = "u_metallicRoughness";
    }

    public static class Vec3 {
    }

    public static class Vec4 {
      public static final String u_diffuseCol = "u_diffuseCol";
      public static final String u_emissiveCol = "u_emissiveCol";
      public static final String u_specularCol = "u_specularCol";
    }

    public static class Sampler2D {
      public static final String u_brdflutTex = "u_brdflutTex";
      public static final String u_diffuseTex = "u_diffuseTex";
      public static final String u_emissiveTex = "u_emissiveTex";
      public static final String u_metallicRoughnessTex = "u_metallicRoughnessTex";
      public static final String u_normalTex = "u_normalTex";
      public static final String u_occlusionTex = "u_occlusionTex";
      public static final String u_reflectionTex = "u_reflectionTex";
      public static final String u_specularTex = "u_specularTex";
    }

    public static class Float {
      public static final String u_alphaTest = "u_alphaTest";
      public static final String u_normalScale = "u_normalScale";
      public static final String u_occlusionStrength = "u_occlusionStrength";
      public static final String u_shadowBias = "u_shadowBias";
      public static final String u_shininess = "u_shininess";
    }
  }

  public PMaterial(String id) {
    this.id = id;

    // PBR defaults.
    set(UniformConstants.Vec4.u_diffuseCol, 1, 1, 1, 1);
    set(UniformConstants.Vec4.u_emissiveCol, 0, 0, 0, 1);
    set(UniformConstants.Vec2.u_metallicRoughness, 0, 0);
    set(UniformConstants.Sampler2D.u_diffuseTex, new PTexture());
    set(UniformConstants.Sampler2D.u_emissiveTex, new PTexture());
    set(UniformConstants.Sampler2D.u_metallicRoughnessTex, new PTexture());
  }

  public float getFloat(String uniform) {
    return vec1s.get(uniform).x();
  }

  public PMaterial set(String uniform, float f) {
    vec1s.getOrMake(uniform).x(f);
    return this;
  }

  public PMaterial set(String uniform, PTexture texture) {
    textures.getOrMake(uniform).set(texture);
    return this;
  }

  public PVec3 getVec2(PVec3 out, String uniform) {
    return out.set(vec3s.get(uniform));
  }

  public PMaterial set(String uniform, PVec2 vec2) {
    vec2s.getOrMake(uniform).set(vec2);
    return this;
  }

  public PMaterial set(String uniform, float x, float y) {
    vec2s.getOrMake(uniform).set(x, y);
    return this;
  }

  public PVec3 getVec3(PVec3 out, String uniform) {
    return out.set(vec3s.get(uniform));
  }

  public PMaterial set(String uniform, PVec3 vec3) {
    vec3s.getOrMake(uniform).set(vec3);
    return this;
  }

  public PMaterial set(String uniform, float x, float y, float z) {
    vec3s.getOrMake(uniform).set(x, y, z);
    return this;
  }

  public PVec4 getVec4(PVec4 out, String uniform) {
    return out.set(vec4s.get(uniform));
  }

  public PMaterial set(String uniform, PVec4 vec4) {
    vec4s.getOrMake(uniform).set(vec4);
    return this;
  }

  public PMaterial set(String uniform, float x, float y, float z, float w) {
    vec4s.getOrMake(uniform).set(x, y, z, w);
    return this;
  }

  public PMaterial set(String uniform, Color color) {
    vec4s.getOrMake(uniform).set(color.r, color.g, color.b, color.a);
    return this;
  }

  public PMaterial cpy(String newName) {
    PMaterial ret = new PMaterial(newName);
    ret.vec1s.tryDeepCopyFrom(vec1s);
    ret.vec2s.tryDeepCopyFrom(vec2s);
    ret.vec3s.tryDeepCopyFrom(vec3s);
    ret.vec4s.tryDeepCopyFrom(vec4s);
    ret.textures.tryDeepCopyFrom(textures);
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
    for (val e : textures) {
      e.getValue().applyShader(e.getKey(), shader);
    }
  }

  public PMaterial setMetallic(float metallic) {
    vec2s.getOrMake(UniformConstants.Vec2.u_metallicRoughness).x(metallic);
    return this;
  }

  public PMaterial setRoughness(float roughness) {
    vec2s.getOrMake(UniformConstants.Vec2.u_metallicRoughness).y(roughness);
    return this;
  }

  public PMaterial setMetallicRoughness(float metallic, float roughness) {
    set(UniformConstants.Vec2.u_metallicRoughness, metallic, roughness);
    return this;
  }

  public PMaterial set(Attribute attribute) {
    PLog.i("Setting attribute " + attribute);
    if (attribute.type == PBRColorAttribute.Diffuse) {
      set(UniformConstants.Vec4.u_diffuseCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRColorAttribute.Ambient) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRColorAttribute.AmbientLight) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRColorAttribute.Emissive) {
      set(UniformConstants.Vec4.u_emissiveCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRColorAttribute.Fog) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRColorAttribute.Reflection) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRColorAttribute.Specular) {
      set(UniformConstants.Vec4.u_specularCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRTextureAttribute.Ambient) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRTextureAttribute.BRDFLUTTexture) {
      set(UniformConstants.Sampler2D.u_brdflutTex, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRTextureAttribute.Bump) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRTextureAttribute.BaseColorTexture || attribute.type == PBRTextureAttribute.Diffuse) {
      set(UniformConstants.Sampler2D.u_diffuseTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Emissive) {
      set(UniformConstants.Sampler2D.u_emissiveTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.EmissiveTexture) {
      PAssert.warnNotImplemented();
    } else if (attribute.type == PBRTextureAttribute.MetallicRoughnessTexture) {
      set(UniformConstants.Sampler2D.u_metallicRoughnessTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Normal || attribute.type == PBRTextureAttribute.NormalTexture) {
      set(UniformConstants.Sampler2D.u_normalTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.OcclusionTexture) {
      set(UniformConstants.Sampler2D.u_occlusionTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Reflection) {
      set(UniformConstants.Sampler2D.u_reflectionTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Specular) {
      set(UniformConstants.Sampler2D.u_specularTex, new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRFloatAttribute.AlphaTest) {
      set(UniformConstants.Float.u_alphaTest, ((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.Metallic) {
      setMetallic(((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.NormalScale) {
      set(UniformConstants.Float.u_normalScale, ((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.OcclusionStrength) {
      set(UniformConstants.Float.u_occlusionStrength, ((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.Roughness) {
      setRoughness(((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.ShadowBias) {
      set(UniformConstants.Float.u_shadowBias, ((PBRFloatAttribute) attribute).value);
    } else if (attribute.type == PBRFloatAttribute.Shininess) {
      set(UniformConstants.Float.u_shininess, ((PBRFloatAttribute) attribute).value);
    } else {
      PAssert.warnNotImplemented();
    }

    return this;
  }
}
