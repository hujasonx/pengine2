package com.phonygames.pengine.graphics.material;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.PModelInstance;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PStringMap;
import com.phonygames.pengine.util.PStringUtils;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PMaterial {
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final String id;
  @Getter
  private final PModelInstance owner;
  private final PStringMap<PTexture> textures = new PStringMap<PTexture>() {
    @Override protected PTexture newUnpooled(String s) {
      return new PTexture();
    }
  };
  private final PStringMap<PVec1> vec1s = new PStringMap<PVec1>(PVec1.getStaticPool());
  private final PStringMap<PVec2> vec2s = new PStringMap<PVec2>(PVec2.getStaticPool());
  private final PStringMap<PVec3> vec3s = new PStringMap<PVec3>(PVec3.getStaticPool());
  private final PStringMap<PVec4> vec4s = new PStringMap<PVec4>(PVec4.getStaticPool());
  @Getter
  private String shaderPrefix;

  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean useVColIndex = false, noModelTransform = false;

  public PMaterial(String id, PModelInstance owner) {
    this.id = id;
    this.owner = owner;
    // PBR defaults.
    set(UniformConstants.Vec4.u_diffuseCol, 1, 1, 1, 1);
    set(UniformConstants.Vec4.u_emissiveCol, 0, 0, 0, 1);
    set(UniformConstants.Vec2.u_metallicRoughness, 0, 0);
    setTexWithUniform(UniformConstants.Sampler2D.u_diffuseTex, new PTexture());
    setTexWithUniform(UniformConstants.Sampler2D.u_emissiveTex, new PTexture());
    setTexWithUniform(UniformConstants.Sampler2D.u_metallicRoughnessTex, new PTexture());
  }

  public PMaterial set(String uniform, float x, float y, float z, float w) {
    vec4s.genPooled(uniform).set(x, y, z, w);
    return this;
  }

  public PMaterial set(String uniform, float x, float y) {
    vec2s.genPooled(uniform).set(x, y);
    return this;
  }

  public PMaterial setTexWithUniform(String uniform, PTexture texture) {
    textures.genUnpooled(uniform).set(texture);
    return this;
  }

  public void applyUniforms(PShader shader) {
    try (val it = vec1s.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        shader.set(e.k(), e.v());
      }
    }
    try (val it = vec2s.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        shader.set(e.k(), e.v());
      }
    }
    try (val it = vec3s.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        shader.set(e.k(), e.v());
      }
    }
    try (val it = vec4s.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        shader.set(e.k(), e.v());
      }
    }
    try (val it = textures.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        e.v().applyShaderWithUniform(e.k(), shader);
      }
    }
  }

  public PMaterial cpy(String newName, PModelInstance newOwner) {
    PMaterial ret = new PMaterial(newName, newOwner);
    ret.vec1s.tryDeepCopyAllFrom(vec1s);
    ret.vec2s.tryDeepCopyAllFrom(vec2s);
    ret.vec3s.tryDeepCopyAllFrom(vec3s);
    ret.vec4s.tryDeepCopyAllFrom(vec4s);
    ret.textures.tryDeepCopyAllFrom(textures);
    ret.useVColIndex = useVColIndex;
    ret.noModelTransform = noModelTransform;
    return ret;
  }

  public String getShaderPrefix() {
    String prefix = "";
    if (useVColIndex) {
      prefix = PStringUtils.concat(prefix, "#define vColIndexFlag\n");
    }
    if (noModelTransform) {
      prefix = PStringUtils.concat(prefix, "#define noModelTransformFlag\n");
    }
    return prefix;
  }

  public float getFloat(String uniform) {
    return vec1s.get(uniform).x();
  }

  public PVec3 getVec2(PVec3 out, String uniform) {
    return out.set(vec3s.get(uniform));
  }

  public PVec3 getVec3(PVec3 out, String uniform) {
    return out.set(vec3s.get(uniform));
  }

  public PVec4 getVec4(PVec4 out, String uniform) {
    return out.set(vec4s.get(uniform));
  }

  public PMaterial set(String uniform, PVec2 vec2) {
    vec2s.genPooled(uniform).set(vec2);
    return this;
  }

  public PMaterial set(String uniform, PVec3 vec3) {
    vec3s.genPooled(uniform).set(vec3);
    return this;
  }

  public PMaterial set(String uniform, float x, float y, float z) {
    vec3s.genPooled(uniform).set(x, y, z);
    return this;
  }

  public PMaterial set(String uniform, PVec4 vec4) {
    vec4s.genPooled(uniform).set(vec4);
    return this;
  }

  public PMaterial set(Attribute attribute) {
    if (attribute.type == PBRColorAttribute.Diffuse) {
      set(UniformConstants.Vec4.u_diffuseCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRColorAttribute.Ambient) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRColorAttribute.AmbientLight) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRColorAttribute.Emissive) {
      set(UniformConstants.Vec4.u_emissiveCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRColorAttribute.Fog) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRColorAttribute.Reflection) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRColorAttribute.Specular) {
      set(UniformConstants.Vec4.u_specularCol, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRTextureAttribute.Ambient) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRTextureAttribute.BRDFLUTTexture) {
      set(UniformConstants.Sampler2D.u_brdflutTex, ((ColorAttribute) attribute).color);
    } else if (attribute.type == PBRTextureAttribute.Bump) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRTextureAttribute.BaseColorTexture ||
               attribute.type == PBRTextureAttribute.Diffuse) {
      setTexWithUniform(UniformConstants.Sampler2D.u_diffuseTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Emissive) {
      setTexWithUniform(UniformConstants.Sampler2D.u_emissiveTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.EmissiveTexture) {
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    } else if (attribute.type == PBRTextureAttribute.MetallicRoughnessTexture) {
      setTexWithUniform(UniformConstants.Sampler2D.u_metallicRoughnessTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Normal || attribute.type == PBRTextureAttribute.NormalTexture) {
      setTexWithUniform(UniformConstants.Sampler2D.u_normalTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.OcclusionTexture) {
      setTexWithUniform(UniformConstants.Sampler2D.u_occlusionTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Reflection) {
      setTexWithUniform(UniformConstants.Sampler2D.u_reflectionTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
    } else if (attribute.type == PBRTextureAttribute.Specular) {
      setTexWithUniform(UniformConstants.Sampler2D.u_specularTex,
                        new PTexture(((PBRTextureAttribute) attribute).textureDescription.texture));
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
      // PAssert.warnNotImplemented("Material attribute " + attribute.toString());
    }
    return this;
  }

  public PMaterial set(String uniform, Color color) {
    vec4s.genPooled(uniform).set(color.r, color.g, color.b, color.a);
    return this;
  }

  public PMaterial set(String uniform, float f) {
    vec1s.genPooled(uniform).x(f);
    return this;
  }

  public PMaterial setMetallic(float metallic) {
    vec2s.genPooled(UniformConstants.Vec2.u_metallicRoughness).x(metallic);
    return this;
  }

  public PMaterial setRoughness(float roughness) {
    vec2s.genPooled(UniformConstants.Vec2.u_metallicRoughness).y(roughness);
    return this;
  }

  public PMaterial setMetallicRoughness(float metallic, float roughness) {
    set(UniformConstants.Vec2.u_metallicRoughness, metallic, roughness);
    return this;
  }

  public PMaterial setTex(String name, PTexture texture) {
    textures.genPooled("u_" + name + "Tex").set(texture);
    return this;
  }

  public static class UniformConstants {
    public static class Float {
      public static final String u_alphaTest = "u_alphaTest";
      public static final String u_normalScale = "u_normalScale";
      public static final String u_occlusionStrength = "u_occlusionStrength";
      public static final String u_shadowBias = "u_shadowBias";
      public static final String u_shininess = "u_shininess";
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

    public static class Vec2 {
      public static final String u_metallicRoughness = "u_metallicRoughness";
    }

    public static class Vec3 {}

    public static class Vec4 {
      public static final String u_diffuseCol = "u_diffuseCol";
      public static final String u_emissiveCol = "u_emissiveCol";
      public static final String u_specularCol = "u_specularCol";
    }
  }
}
