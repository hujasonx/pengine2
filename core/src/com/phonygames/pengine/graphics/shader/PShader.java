package com.phonygames.pengine.graphics.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.exception.PRuntimeException;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;

public class PShader {
@Getter
  private final ShaderProgram shaderProgram;

  @Getter
  private static PShader activeShader;

  @Getter
  private final String vertexShaderSource, fragmentShaderSource;

  public PShader(FileHandle vert, FileHandle frag) {
    StringBuilder vertexStringBuilder = new StringBuilder();
    StringBuilder fragmentStringBuilder = new StringBuilder();

    loadRaw(vert, vertexStringBuilder, true);
    loadRaw(frag, fragmentStringBuilder, false);

    vertexShaderSource = vertexStringBuilder.toString();
    fragmentShaderSource = fragmentStringBuilder.toString();

    shaderProgram = new ShaderProgram(vertexShaderSource, fragmentShaderSource);
    if (!shaderProgram.isCompiled()) {
      throw new PRuntimeException("Shader was not compiled");
    }
  }

  private static void loadRaw(FileHandle fileHandle, StringBuilder out, boolean isVertexShader) {
    boolean isFragmentShader = !isVertexShader;
    String shader = fileHandle.readString();
    String[] split = PStringUtils.splitByLine(shader);
    for (int a = 0; a < split.length; a++) {
      String line = split[a];
      if (line.startsWith("#include ")) {
        String fname = line.split("\"")[1];
        if (fileHandle.parent() != null) {
          fname = fileHandle.parent().path() + "/" + fname;
        }
        loadRaw(Gdx.files.local(fname), out, isVertexShader);
      } else {
        out.append(line);
      }
      out.append('\n');
    }
  }

  public boolean isActive() {
    return activeShader == this;
  }

  public void start() {
    PAssert.isFalse(isActive());
    activeShader = this;
    shaderProgram.bind();
  }

  public void end() {
    PAssert.isTrue(isActive());
    activeShader = null;
  }

  public PShader set(String uniform, PVec1 v) {
    return set(uniform, v.x());
  }

  public PShader set(String uniform, float x) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x);
    } catch (IllegalArgumentException e) {
      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec2 v) {
    return set(uniform, v.x(), v.y());
  }

  public PShader set(String uniform, float x, float y) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y);
    } catch (IllegalArgumentException e) {
      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec3 v) {
    return set(uniform, v.x(), v.y(), v.z());
  }

  public PShader set(String uniform, float x, float y, float z) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y, z);
    } catch (IllegalArgumentException e) {
      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec4 v) {
    return set(uniform, v.x(), v.y(), v.z(), v.w());
  }

  public PShader set(String uniform, float x, float y, float z, float w) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y, z, w);
    } catch (IllegalArgumentException e) {
      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }
}
