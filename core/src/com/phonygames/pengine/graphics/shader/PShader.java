package com.phonygames.pengine.graphics.shader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.file.PFileHandleUtils;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.collection.PSet;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

public class PShader implements Disposable, Comparable<PShader> {
  public static final String GL_VERSION = "330";
  private static final String PSHADER_COMMENT_END = " */ ";
  private static final String PSHADER_COMMENT_START = "/** PSHADER ";
  private static final PFileHandleUtils.RecursiveLoadProcessor RECURSIVE_LOAD_PROCESSOR =
      new PFileHandleUtils.RecursiveLoadProcessor() {
        @Override
        public String processLine(int totalLineNo, int rawLineNo, String prefix, String rawLine, FileHandle rawFile) {
          String shaderPrefix = PSHADER_COMMENT_START + rawFile.path() + ":" + rawLineNo + PSHADER_COMMENT_END;
          return shaderPrefix + prefix + rawLine;
        }
      };
  private static final PSet<PShader> staticShaders = new PSet<>();
  @Getter
  private static PShader activeShader;
  private final String fragmentLayout;
  /** The inputs passed to the shader filehandler loader system */
  private final String[] inputs;
  private final String prefix;
  private final FileHandle vsSourceFH, fsSourceFH;
  /** Takes precedences over the filehandles. */
  private final String rawVSCode, rawFSCode;
  private float checkValidLoggingThrottleNextCheckTime = 0;
  private String combinedStaticStringResult;
  @Getter
  private ShaderProgram shaderProgram;
  private String toStringResult;
  @Getter
  private String vertexShaderSource, fragmentShaderSource;

  public PShader(@NonNull String prefix, @NonNull String fragmentLayout, @NonNull PVertexAttributes vertexAttributes,
                 FileHandle vert, FileHandle frag, String[] inputs) {
    this(prefix, fragmentLayout,vertexAttributes,vert,frag,null, null, inputs);
  }

  public PShader(@NonNull String prefix, @NonNull String fragmentLayout, @NonNull PVertexAttributes vertexAttributes,
                 String vert, FileHandle frag, String[] inputs) {
    this(prefix, fragmentLayout,vertexAttributes,null,frag,vert, null, inputs);
  }

  public PShader(@NonNull String prefix, @NonNull String fragmentLayout, @NonNull PVertexAttributes vertexAttributes,
                 String vert, String frag, String[] inputs) {
    this(prefix, fragmentLayout,vertexAttributes,null,null,vert, frag, inputs);
  }

  public PShader(@NonNull String prefix, @NonNull String fragmentLayout, @NonNull PVertexAttributes vertexAttributes,
                 FileHandle vert, String frag, String[] inputs) {
    this(prefix, fragmentLayout,vertexAttributes,vert,null,null, frag, inputs);
  }

  private PShader(@NonNull String prefix, @NonNull String fragmentLayout, @NonNull PVertexAttributes vertexAttributes,
                 FileHandle vertFH, FileHandle fragFH, String vert, String frag, String[] inputs) {
    this.prefix = prefix + vertexAttributes.getPrefix() + "\n// PREFIX END\n\n";
    this.fragmentLayout = fragmentLayout;
    vsSourceFH = vertFH;
    fsSourceFH = fragFH;
    staticShaders.add(this);
    this.inputs = inputs == null ? new String[0] : inputs;
    rawVSCode = vert;
    rawFSCode = frag;
    reloadFromSources(false);
  }

  public void reloadFromSources(boolean logAlwaysIfSourcesChanged) {
    combinedStaticStringResult = null;
    StringBuilder vertexStringBuilder = new StringBuilder("#version " + GL_VERSION + "\n// VERTEX SHADER\n").append(this.prefix);
    StringBuilder fragmentStringBuilder =
        new StringBuilder("#version " + GL_VERSION + "\n// FRAGMENT SHADER\n").append(this.prefix).append(fragmentLayout).append("\n");
    if (rawVSCode != null) {
      vertexStringBuilder.append(PFileHandleUtils.loadRecursive(rawVSCode, Gdx.files.local(""), RECURSIVE_LOAD_PROCESSOR, inputs));
    } else {
      vertexStringBuilder.append(PFileHandleUtils.loadRecursive(vsSourceFH, RECURSIVE_LOAD_PROCESSOR, inputs));
    }
    if (rawFSCode != null) {
      fragmentStringBuilder.append(PFileHandleUtils.loadRecursive(rawFSCode, Gdx.files.local(""), RECURSIVE_LOAD_PROCESSOR, inputs));
    } else {
      fragmentStringBuilder.append(PFileHandleUtils.loadRecursive(fsSourceFH, RECURSIVE_LOAD_PROCESSOR, inputs));
    }
    int oldCombinedSourceHash = (vertexShaderSource == null ? 0 : vertexShaderSource.hashCode()) +
                                (fragmentShaderSource == null ? 0 : fragmentShaderSource.hashCode());
    vertexShaderSource = vertexStringBuilder.toString();
    fragmentShaderSource = fragmentStringBuilder.toString();
    int newCombinedSourceHash = vertexShaderSource.hashCode() + fragmentShaderSource.hashCode();
    if (shaderProgram != null) {
      shaderProgram.dispose();
    }
    shaderProgram = new ShaderProgram(vertexShaderSource, fragmentShaderSource);
    // Print the shader logs output if the shader was changed.
    String newStringResult = getCompileFailureString();
    if (!newStringResult.equals(toStringResult)) {
      if (!shaderProgram.isCompiled()) {
        PLog.w(getCompileFailureString());
      } else if (logAlwaysIfSourcesChanged && (oldCombinedSourceHash != newCombinedSourceHash)) {
        PLog.i(getCompileFailureString());
      }
    }
    toStringResult = newStringResult;
  }

  private String getCompileFailureString() {
    try {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[PShader]\n\n");
      if (vsSourceFH != null) {
        stringBuilder.append("V: ");
        stringBuilder.append(vsSourceFH.path()).append("\n");
      }
      printSource(stringBuilder, "V: ", PStringUtils.splitByLine(vertexShaderSource));

      if (fsSourceFH != null) {
        stringBuilder.append("F: ");
        stringBuilder.append(fsSourceFH.path()).append("\n");
      }
      printSource(stringBuilder, "F: ", PStringUtils.splitByLine(fragmentShaderSource));
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
                stringBuilder.append(phase == 2 ? "\nFrag shader: " : "\nVert shader: ").append(fileNameAndLine[0])
                             .append("\n");
                printSource(stringBuilder, phase == 1 ? "V: " : "F: ", rawShaderLines,
                            offendingLineNo - linesToShowBeforeAndAfter, offendingLineNo + linesToShowBeforeAndAfter,
                            offendingLineNo);
              }
              stringBuilder.append("[").append(PStringUtils.prependSpacesToLength(offendingLineNo + "", 4)).append("] ")
                           .append(errorString).append("\n");
              previousOffendingLineNo = offendingLineNo;
            }
          }
        }
        if (previousOffendingLineNo == -1) {
          // No error was emitted, but somehow; the shader still couldnt be compiled, so just add the raw log.
          stringBuilder.append(shaderProgram.getLog());
        }
      }
      stringBuilder.append("\n[/PShader compile error]\n");
      return stringBuilder.toString();
    } catch (Exception e) {
      PLog.e(e);
    }
    return shaderProgram.getLog();
  }

  private static void printSource(StringBuilder stringBuilder, String linePrefix, String[] rawLines) {
    printSource(stringBuilder, linePrefix, rawLines, 0, rawLines.length, -1);
  }

  /**
   * @param stringBuilder
   * @param rawLines
   * @param lineStart
   * @param lineEnd
   * @param lineToFlag    if -1, then the actual, post-processed line numbers will be used.
   */
  private static void printSource(StringBuilder stringBuilder, String linePrefix, String[] rawLines, int lineStart,
                                  int lineEnd, int lineToFlag) {
    for (int lineNo = lineStart; lineNo <= lineEnd; lineNo++) {
      if (lineNo < 0 || lineNo >= rawLines.length) {
        continue;
      }
      int rawLineNo = -1;
      String line = rawLines[lineNo];
      String rawLine = line;
      stringBuilder.append(linePrefix);
      if (line.startsWith(PSHADER_COMMENT_START)) {
        String[] fileNameAndLine = PStringUtils.extract(line, PSHADER_COMMENT_START, PSHADER_COMMENT_END).split(":");
        rawLineNo = Integer.parseInt(fileNameAndLine[1]);
        rawLine = PStringUtils.partAfter(line, PSHADER_COMMENT_END);
        if (lineToFlag != -1) {
          stringBuilder.append(PStringUtils.prependSpacesToLength("" + rawLineNo, 4)).append("| ");
        }
      } else {
        // Anything not loaded from a file.
        if (lineToFlag != -1) {
          stringBuilder.append("  PS| ");
        }
      }
      if (lineToFlag == -1) {
        // Use the actual, postprocessed value.
        stringBuilder.append(PStringUtils.prependSpacesToLength("" + lineNo, 4)).append("| ");
      }
      stringBuilder.append(rawLine);
      if (lineNo == lineToFlag) {
        stringBuilder.append(" <=== HERE");
      }
      stringBuilder.append("\n");
    }
  }

  public static void reloadAllFromSources() {
    try (val it = staticShaders.obtainIterator()) {
      while (it.hasNext()) {
        PShader shader = it.next();
        if (shader != null) {
          shader.reloadFromSources(true);
        }
      }
    }
  }

  @Override public int compareTo(PShader shader) {
    return combinedStaticString().hashCode() - shader.combinedStaticString().hashCode();
  }

  private String combinedStaticString() {
    if (combinedStaticStringResult == null) {
      combinedStaticStringResult = prefix + "\n" + fragmentLayout;
      combinedStaticStringResult += " [ ";
      if (inputs != null) {
        for (int a = 0; a < inputs.length; a++) {
          combinedStaticStringResult += inputs[a] + (a == inputs.length - 1 ? "" : ", ");
        }
      }
      combinedStaticStringResult += " ]\n";
      combinedStaticStringResult += vsSourceFH == null ? rawVSCode : vsSourceFH.path();
      combinedStaticStringResult += "\n";
      combinedStaticStringResult += fsSourceFH == null ? rawFSCode : fsSourceFH.path();
    }
    return combinedStaticStringResult;
  }

  @Override public void dispose() {
    if (shaderProgram != null) {
      shaderProgram.dispose();
      shaderProgram = null;
    }
    staticShaders.remove(this);
  }

  public void end() {
    if (!checkValid()) {
      return;
    }
    PAssert.isTrue(isActive());
    activeShader = null;
  }

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

  public boolean isActive() {
    return activeShader == this;
  }

  @Override public int hashCode() {
    return combinedStaticString().hashCode();
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof PShader)) {
      return false;
    }
    return combinedStaticString().equals(((PShader) o).combinedStaticString());
  }

  @Override public String toString() {
    if (toStringResult != null) {
      return toStringResult;
    }
    return super.toString();
  }

  public PShader set(String uniform, PMat4 mat4) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformMatrix(uniform, mat4.getBackingMatrix4());
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
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformf(uniform, x);
    }
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
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformf(uniform, x, y);
    }
    return this;
  }

  public PShader set(String uniform, float[] fs, int size) {
    return set(uniform, fs, size, 0, fs.length);
  }

  public PShader set(String uniform, float[] fs, int size, int offset, int count) {
    if (!checkValid()) {
      return this;
    }
    uniform = PStringUtils.concat(uniform, "[0]");
    PAssert.isTrue(isActive());
    if (shaderProgram.hasUniform(uniform)) {
      switch (size) {
        case 1:
          shaderProgram.setUniform1fv(uniform, fs, offset, count);
          break;
        case 2:
          shaderProgram.setUniform2fv(uniform, fs, offset, count);
          break;
        case 3:
          shaderProgram.setUniform3fv(uniform, fs, offset, count);
          break;
        case 4:
          shaderProgram.setUniform4fv(uniform, fs, offset, count);
          break;
        case 16:
          shaderProgram.setUniformMatrix4fv(uniform, fs, offset, count);
          break;
      }
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
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformf(uniform, x, y, z);
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
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformf(uniform, x, y, z, w);
    }
    return this;
  }

  public PShader setI(String uniform, int i) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    if (shaderProgram.hasUniform(uniform)) {
      shaderProgram.setUniformi(uniform, i);
    }
    return this;
  }

  public PShader setWithUniform(String uniform, Texture texture) {
    if (!checkValid()) {
      return this;
    }
    PAssert.isTrue(isActive());
    if (texture != null && shaderProgram.hasUniform(uniform)) {
      int bindTarget = PRenderContext.activeContext().getTextureBinder().bind(texture);
      shaderProgram.setUniformi(uniform, bindTarget);
      set(PStringUtils.concat(uniform, "Size"), texture.getWidth(), texture.getHeight(), 1f / texture.getWidth(),
          1f / texture.getHeight());
    }
    return this;
  }

  public void start(PRenderContext renderContext) {
    if (!checkValid()) {
      return;
    }
    PAssert.isFalse(isActive());
    activeShader = this;
    shaderProgram.bind();
    set(PRenderContext.UniformConstants.Vec4.u_tdtuituidt, PEngine.t, PEngine.dt, PEngine.uit, PEngine.uidt);
    set(PRenderContext.UniformConstants.Vec4.u_renderBufferSize, PRenderBuffer.activeBuffer().width(),
        PRenderBuffer.activeBuffer().height(), 1f / PRenderBuffer.activeBuffer().width(),
        1f / PRenderBuffer.activeBuffer().height());
    set(PRenderContext.UniformConstants.Mat4.u_viewProjTransform, renderContext.viewProjTransform());
    set(PRenderContext.UniformConstants.Mat4.u_viewProjTransformInvTra, renderContext.viewProjInvTraTransform());
    set(PRenderContext.UniformConstants.Vec3.u_cameraPos, renderContext.cameraPos());
    set(PRenderContext.UniformConstants.Vec3.u_cameraDir, renderContext.cameraDir());
    set(PRenderContext.UniformConstants.Vec3.u_cameraUp, renderContext.cameraUp());
  }
}
