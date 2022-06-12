package com.phonygames.pengine.graphics.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.exception.PRuntimeException;
import com.phonygames.pengine.file.PFileHandleUtils;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PMesh;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.lighting.PLight;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;
import com.phonygames.pengine.util.PStringUtils;

import java.util.HashMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

public class PShader implements Disposable {
  private static final PSet<PShader> staticShaders = new PSet<>();
  private final String prefix;
  private final String fragmentLayout;

  @Getter
  private ShaderProgram shaderProgram;

  @Getter
  private static PShader activeShader;

  @Getter
  private String vertexShaderSource, fragmentShaderSource;
  private final FileHandle vsSourceFH, fsSourceFH;

  private static final String PSHADER_COMMENT_START = "/** PSHADER ";
  private static final String PSHADER_COMMENT_END = " */ ";

  private String toStringResult;

  private static final PFileHandleUtils.RecursiveLoadProcessor RECURSIVE_LOAD_PROCESSOR =
      new PFileHandleUtils.RecursiveLoadProcessor() {
        @Override
        public String processLine(int totalLineNo, int rawLineNo, String prefix, String rawLine, FileHandle rawFile) {
          String shaderPrefix = PSHADER_COMMENT_START + rawFile.path() + ":" + rawLineNo + PSHADER_COMMENT_END;
          return shaderPrefix + prefix + rawLine;
        }
      };

  public PShader(@NonNull String prefix,
                 @NonNull String fragmentLayout,
                 @NonNull PVertexAttributes vertexAttributes,
                 FileHandle vert,
                 FileHandle frag) {
    staticShaders.add(this);
    this.prefix = prefix + vertexAttributes.getPrefix() + "\n// PREFIX END\n\n";
    this.fragmentLayout = fragmentLayout;
    vsSourceFH = vert;
    fsSourceFH = frag;

    reloadFromSources();
  }

  public void reloadFromSources() {
    StringBuilder vertexStringBuilder = new StringBuilder("#version 330\n// VERTEX SHADER\n").append(this.prefix);
    StringBuilder fragmentStringBuilder =
        new StringBuilder("#version 330\n// FRAGMENT SHADER\n").append(this.prefix).append(fragmentLayout).append("\n");

    vertexStringBuilder.append(PFileHandleUtils.loadRecursive(vsSourceFH, RECURSIVE_LOAD_PROCESSOR));
    fragmentStringBuilder.append(PFileHandleUtils.loadRecursive(fsSourceFH, RECURSIVE_LOAD_PROCESSOR));

    vertexShaderSource = vertexStringBuilder.toString();
    fragmentShaderSource = fragmentStringBuilder.toString();

    if (shaderProgram != null) {
      shaderProgram.dispose();
    }
    shaderProgram = new ShaderProgram(vertexShaderSource, fragmentShaderSource);

    // Print the shader logs output if the shader was changed.
    String newStringResult = getCompileFailureString();
    if (!newStringResult.equals(toStringResult)) {
      if (!shaderProgram.isCompiled()) {
        PLog.w(getCompileFailureString());
      } else {
        PLog.i(getCompileFailureString());
      }
    }
    toStringResult = newStringResult;
  }

  private String getCompileFailureString() {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[PShader]\n\n");
      stringBuilder.append("V: ");
      stringBuilder.append(vsSourceFH.path()).append("\n");
      printSource(stringBuilder, PStringUtils.splitByLine(vertexShaderSource));
      stringBuilder.append("F: ");
      stringBuilder.append(fsSourceFH.path()).append("\n");
      printSource(stringBuilder, PStringUtils.splitByLine(fragmentShaderSource));

      if (shaderProgram.isCompiled()) {
        stringBuilder.append("\nNo errors!\n");
      } else {
        stringBuilder.append("\nErrors:\n");

        String[] logLines = PStringUtils.splitByLine(shaderProgram.getLog());

        int phase = 0;
        String[] rawShaderLines = null;
        int previousOffendingLineNo = -1;
        for (int a = 0; a < logLines.length; a++) {
          String logLine = logLines[a];
          if (logLine.startsWith("Vertex shader")) {
            phase = 1;
            rawShaderLines = PStringUtils.splitByLine(vertexShaderSource);
          } else if (logLine.startsWith("Fragment shader:")) {
            phase = 2;
            rawShaderLines = PStringUtils.splitByLine(fragmentShaderSource);
          } else {

            if (phase == 1 || phase == 2) {
              int offendingLineNo = Integer.parseInt(PStringUtils.extract(logLine, "(", ")")) - 1;
              String errorString = logLine.substring(PStringUtils.indexAfter(logLine, " : "));
              String offendingLine = rawShaderLines[offendingLineNo];

              String[] fileNameAndLine =
                  PStringUtils.extract(offendingLine, PSHADER_COMMENT_START, PSHADER_COMMENT_END).split(":");

              if (previousOffendingLineNo != offendingLineNo) {
                // Print the shader source around the error.
                int linesToShowBeforeAndAfter = 2;
                stringBuilder.append(phase == 2 ? "\nF: " : "\nV: ").append(fileNameAndLine[0]).append("\n");
                printSource(stringBuilder,
                            rawShaderLines,
                            offendingLineNo - linesToShowBeforeAndAfter,
                            offendingLineNo + linesToShowBeforeAndAfter,
                            offendingLineNo);
              }
              stringBuilder.append("[").append(PStringUtils.prependSpacesToLength(offendingLineNo + "", 4)).append("] ")
                  .append(errorString).append("\n");

              previousOffendingLineNo = offendingLineNo;
            }
          }
        }
      }

      stringBuilder.append("\n[/PShader compile error]\n");
      return stringBuilder.toString();
    } catch (Exception e) {
      PLog.e(e);
    }

    return shaderProgram.getLog();
  }

  private static void printSource(StringBuilder stringBuilder, String[] rawLines) {
    printSource(stringBuilder, rawLines, 0, rawLines.length, -1);
  }

  /**
   * @param stringBuilder
   * @param rawLines
   * @param lineStart
   * @param lineEnd
   * @param lineToFlag    if -1, then the actual, post-processed line numbers will be used.
   */
  private static void printSource(StringBuilder stringBuilder,
                                  String[] rawLines,
                                  int lineStart,
                                  int lineEnd,
                                  int lineToFlag) {
    for (int lineNo = lineStart; lineNo <= lineEnd; lineNo++) {
      if (lineNo < 0 || lineNo >= rawLines.length) {
        continue;
      }

      int rawLineNo = -1;
      String line = rawLines[lineNo];
      String rawLine = line;

      if (line.startsWith(PSHADER_COMMENT_START)) {
        String[] fileNameAndLine = PStringUtils.extract(line, PSHADER_COMMENT_START, PSHADER_COMMENT_END).split(":");
        rawLineNo = Integer.parseInt(fileNameAndLine[1]);
        rawLine = PStringUtils.partAfter(line, PSHADER_COMMENT_END);
        if (lineToFlag != -1) {
          stringBuilder.append(PStringUtils.prependSpacesToLength("" + rawLineNo, 4)).append(": ");
        }
      } else {
        // Anything not loaded from a file.
        if (lineToFlag != -1) {
          stringBuilder.append("  PS: ");
        }
      }

      if (lineToFlag == -1) {
        // Use the actual, postprocessed value.
        stringBuilder.append(PStringUtils.prependSpacesToLength("" + lineNo, 4)).append(": ");
      }

      stringBuilder.append(rawLine);
      if (lineNo == lineToFlag) {
        stringBuilder.append(" <=== HERE");
      }
      stringBuilder.append("\n");
    }
  }

  public boolean isActive() {
    return activeShader == this;
  }

  public void start(PRenderContext renderContext) {
    if (!checkValid()) {
      return;
    }
    PAssert.isFalse(isActive());
    activeShader = this;
    shaderProgram.bind();
    set(PRenderContext.UniformConstants.Vec4.u_tdtuituidt, PEngine.t, PEngine.dt, PEngine.uit, PEngine.uidt);
    set(PRenderContext.UniformConstants.Vec4.u_renderBufferSize,
        PRenderBuffer.getActiveBuffer().width(),
        PRenderBuffer.getActiveBuffer().height(),
        1f / PRenderBuffer.getActiveBuffer().width(),
        1f / PRenderBuffer.getActiveBuffer().height());
    set(PRenderContext.UniformConstants.Mat4.u_viewProjTransform, renderContext.getViewProjTransform());
    set(PRenderContext.UniformConstants.Mat4.u_viewProjTransformInvTra, renderContext.getViewProjInvTraTransform());
    set(PRenderContext.UniformConstants.Vec3.u_cameraPos, renderContext.getCameraPos());
    set(PRenderContext.UniformConstants.Vec3.u_cameraDir, renderContext.getCameraDir());
    set(PRenderContext.UniformConstants.Vec3.u_cameraUp, renderContext.getCameraUp());
  }

  public void end() {
    if (!checkValid()) {
      return;
    }
    PAssert.isTrue(isActive());
    activeShader = null;
  }

  public PShader set(String uniform, PMat4 mat4) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformMatrix(uniform, mat4.getBackingMatrix4());
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec1 v) {
    return set(uniform, v.x());
  }

  public PShader set(String uniform, float x) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader setI(String uniform, int i) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformi(uniform, i);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader setWithUniform(String uniform, Texture texture) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      int t = PRenderContext.getActiveContext().getTextureBinder().bind(texture);
      shaderProgram.setUniformi(uniform, t);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    set(uniform + "Size", texture.getWidth(), texture.getHeight(), 1f / texture.getWidth(), 1f / texture.getHeight());
    return this;
  }

  public PShader set(String uniform, PVec2 v) {
    return set(uniform, v.x(), v.y());
  }

  public PShader set(String uniform, float x, float y) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec3 v) {
    return set(uniform, v.x(), v.y(), v.z());
  }

  public PShader set(String uniform, float x, float y, float z) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y, z);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  public PShader set(String uniform, PVec4 v) {
    return set(uniform, v.x(), v.y(), v.z(), v.w());
  }

  public PShader set(String uniform, float x, float y, float z, float w) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    try {
      shaderProgram.setUniformf(uniform, x, y, z, w);
    } catch (IllegalArgumentException e) {
//      PLog.w("Illegal argument: " + uniform, e);
    }
    return this;
  }

  @Override
  public String toString() {
    if (toStringResult != null) {
      return toStringResult;
    }
    return super.toString();
  }

  @Override
  public void dispose() {
    if (shaderProgram != null) {
      shaderProgram.dispose();
      shaderProgram = null;
    }

    staticShaders.remove(this);
  }

  public static void reloadAllFromSources() {
    for (val shader : staticShaders) {
      shader.reloadFromSources();
    }
  }

  private float checkValidLoggingThrottleNextCheckTime = 0;

  public boolean checkValid() {
    if (!shaderProgram.isCompiled()) {
      if (PEngine.uit > checkValidLoggingThrottleNextCheckTime) {
        PLog.w("Shader invalid: " + getCompileFailureString());
        checkValidLoggingThrottleNextCheckTime = PEngine.uit + 5;
      }
      return false;
    }

    return true;
  }
}
