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
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PShader {
  private final String prefix;

  @Getter
  private final ShaderProgram shaderProgram;

  @Getter
  private static PShader activeShader;

  @Getter
  private final String vertexShaderSource, fragmentShaderSource;

  public PShader(String prefix, PVertexAttributes vertexAttributes, FileHandle vert, FileHandle frag) {
    this.prefix = prefix + vertexAttributes.getPrefix() + "\n// PREFIX END\n\n";
    StringBuilder vertexStringBuilder = new StringBuilder("#version 330\n// VERTEX SHADER\n").append(this.prefix);
    StringBuilder fragmentStringBuilder = new StringBuilder("#version 330\n// FRAGMENT SHADER\n").append(this.prefix);

    loadRaw(vert, vertexStringBuilder, "");
    loadRaw(frag, fragmentStringBuilder, "");

    vertexShaderSource = vertexStringBuilder.toString();
    fragmentShaderSource = fragmentStringBuilder.toString();

    shaderProgram = new ShaderProgram(vertexShaderSource, fragmentShaderSource);
    if (!shaderProgram.isCompiled()) {
      PLog.w(shaderProgram.getVertexShaderSource());
      PLog.w(shaderProgram.getFragmentShaderSource());
      throw new PRuntimeException("Shader was not compiled:\n" + shaderProgram.getLog());
    }
  }

  private static void loadRaw(FileHandle fileHandle, StringBuilder out, String linePrefix) {
    String shader = fileHandle.readString();
    String[] split = PStringUtils.splitByLine(shader);
    for (int a = 0; a < split.length; a++) {
      String line = split[a];
      int fileLineSpaceCount = 0;
      for (int b = 0; b < line.length(); b++) {
        if (line.charAt(b) == ' ') {
          fileLineSpaceCount++;
        } else {
          break;
        }
      }

      String fileLineTabString = line.substring(0, fileLineSpaceCount);
      String lineWithoutTab = line.substring(fileLineSpaceCount);
      if (lineWithoutTab.startsWith("#include ")) {
        char delim = lineWithoutTab.charAt("#include ".length());
        String fname = "";
        switch (delim) {
          case '"':
            fname = line.split("\"")[1];
            if (fileHandle.parent() != null) {
              fname = fileHandle.parent().path() + "/" + fname;
            }
            break;
          case '<':
            // Absolute path.
            fname = line.split("<")[1].split(">")[0];
            break;
        }
        loadRaw(Gdx.files.local(fname), out, linePrefix + fileLineTabString);
      } else {
        out.append(linePrefix);
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

  public PShader set(String uniform, PMat4 mat4) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformMatrix(uniform, mat4.getBackingMatrix4());
    } catch (IllegalArgumentException e) {
      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
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

  public PShader setI(String uniform, int i) {
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformi(uniform, i);
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
