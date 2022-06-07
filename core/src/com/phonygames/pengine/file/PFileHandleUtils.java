package com.phonygames.pengine.file;

import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PFileHandleUtils {

  /**
   * Returns a list of all files, recursively, beginning at the specified file.
   *
   * @param fileHandle root file/directory.
   */
  public static List<FileHandle> allFilesRecursive(FileHandle fileHandle) {
    return allFilesRecursive(fileHandle, null);
  }

  /**
   * Returns a list of all files, recursively, beginning at the specified file.
   *
   * @param fileHandle root file/directory.
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
}
