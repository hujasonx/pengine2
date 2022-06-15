package com.phonygames.pengine.file;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.phonygames.pengine.util.PStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PFileHandleUtils {
  /**
   * Returns a list of all files, recursively, beginning at the specified file.
   * @param fileHandle root file/directory.
   */
  public static List<FileHandle> allFilesRecursive(FileHandle fileHandle) {
    return allFilesRecursive(fileHandle, null);
  }

  /**
   * Returns a list of all files, recursively, beginning at the specified file.
   * @param fileHandle     root file/directory.
   * @param extensionRegex the regex to test for extension.
   */
  public static List<FileHandle> allFilesRecursive(FileHandle fileHandle, String extensionRegex) {
    List<FileHandle> ret = new ArrayList<>();
    List<FileHandle> buffer = new ArrayList<>();
    buffer.add(fileHandle);
    while (buffer.size() > 0) {
      FileHandle f = buffer.remove(buffer.size() - 1);
      if (f.isDirectory()) {
        FileHandle[] children = f.list();
        for (int a = 0; a < children.length; a++) {
          buffer.add(children[a]);
        }
      } else {
        if (extensionRegex == null || Pattern.matches(extensionRegex, f.extension())) {
          ret.add(f);
        }
      }
    }
    return ret;
  }

  public static String loadRecursive(FileHandle fileHandle, RecursiveLoadProcessor loadProcessor) {
    StringBuilder stringBuilder = new StringBuilder();
    loadRecursive(stringBuilder, fileHandle, loadProcessor, "", 0, null);
    return stringBuilder.toString();
  }

  // Returns the total line number.
  private static int loadRecursive(StringBuilder stringBuilder, FileHandle fileHandle,
                                   RecursiveLoadProcessor loadProcessor, String prefix, int totalLineNo,
                                   String[] inputs) {
    String rawString = fileHandle.readString();
    String[] lines = PStringUtils.splitByLine(rawString);
    if (lines.length == 0) {
      return totalLineNo;
    }
    // Get the params.
    String[] params = null;
    for (int rawLineNo = 0; rawLineNo < lines.length; rawLineNo++) {
      String line = lines[rawLineNo];
      String strippedLine = line.strip();
      // Replace with parameters.
      if (inputs != null && params != null) {
        for (int a = 0; a < inputs.length; a++) {
          line = line.replaceAll(params[a], inputs[a]);
        }
      }
      // Check to see if we should be doing a recursive load.
      String extractedIncludeFilename = PStringUtils.extract(line, "#include <", ">");
      if (extractedIncludeFilename == null) {
        // Relative directory.
        extractedIncludeFilename = PStringUtils.extract(line, "#include \"", "\"");
        if (extractedIncludeFilename != null && fileHandle.parent() != null) {
          extractedIncludeFilename = fileHandle.parent().path() + "/" + extractedIncludeFilename;
        }
      }
      if (extractedIncludeFilename != null && !strippedLine.startsWith("//")) {
        totalLineNo = loadRecursive(stringBuilder, Gdx.files.local(extractedIncludeFilename), loadProcessor,
                                    prefix + PStringUtils.getLineSpacePrefix(line), totalLineNo,
                                    PStringUtils.extractStringArray(line, "[", "]", ",", true));
        continue;
      }
      // Check to see if we should be setting params.
      String[] newParams = PStringUtils.extractStringArray(line, "#params [", "]", ",", true);
      if (newParams != null) {
        params = newParams;
        continue;
      }
      // Handle the line normally, and possibly output it.
      String processedLine = loadProcessor.processLine(totalLineNo, rawLineNo, prefix, line, fileHandle);
      if (processedLine != null) {
        stringBuilder.append(processedLine).append('\n');
        totalLineNo++;
      }
    }
    return totalLineNo;
  }

  public abstract static class RecursiveLoadProcessor {
    public abstract String processLine(int totalLineNo, int rawLineNo, String prefix, String rawLine,
                                       FileHandle rawFile);
  }
}
