package com.phonygames.pengine.util.jni;

import com.badlogic.gdx.jnigen.FileDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PJniAntBuilder {
  public PJniAntBuilder() {
  }

  public static boolean executeAnt(String buildFile, String... params) {
    FileDescriptor build = new FileDescriptor(buildFile);
    String ant = System.getProperty("os.name").contains("Windows") ? "ant.bat" : "ant";
    List<String> command = new ArrayList();
    command.add(ant);
    command.add("-f");
    command.add(build.file().getAbsolutePath());
    command.addAll(Arrays.asList(params));
    String[] args = (String[]) command.toArray(new String[0]);
    System.out.println("Executing '" + command + "'");
    return startProcess(build.parent().file(), args);
  }

  private static boolean startProcess(File directory, String... command) {
    try {
      final Process process = (new ProcessBuilder(command)).redirectErrorStream(true).directory(directory).start();
      Thread t = new Thread(new Runnable() {
        @Override public void run() {
          BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line = null;
          try {
            while ((line = reader.readLine()) != null) {
              this.printFileLineNumber(line);
            }
          } catch (IOException var4) {
            var4.printStackTrace();
          }
        }

        private void printFileLineNumber(String line) {
          if (!line.contains("warning") && !line.contains("error")) {
            System.out.println(line);
          } else {
            try {
              String fileName = this.getFileName(line);
              String error = this.getError(line);
              int lineNumber = this.getLineNumber(line) - 1;
              if (fileName != null && lineNumber >= 0) {
                FileDescriptor file = new FileDescriptor(fileName);
                if (file.exists()) {
                  String[] content = file.readString().split("\n");
                  if (lineNumber < content.length) {
                    for (int i = lineNumber; i >= 0; --i) {
                      String contentLine = content[i];
                      if (contentLine.startsWith("//@line:")) {
                        int javaLineNumber = Integer.parseInt(contentLine.split(":")[1].trim());
                        System.out.flush();
                        if (line.contains("warning")) {
                          System.out.println(
                              "(" + file.nameWithoutExtension() + ".java:" + (javaLineNumber + (lineNumber - i) - 1) +
                              "): " + error + ", original: " + line);
                          System.out.flush();
                        } else {
                          System.err.println(
                              "(" + file.nameWithoutExtension() + ".java:" + (javaLineNumber + (lineNumber - i) - 1) +
                              "): " + error + ", original: " + line);
                          System.err.flush();
                        }
                        return;
                      }
                    }
                  }
                } else {
                  System.out.println(line);
                }
              }
            } catch (Throwable var10) {
              System.out.println(line);
            }
          }
        }

        private String getFileName(String line) {
          Pattern pattern = Pattern.compile("(.*):([0-9])+:[0-9]+:");
          Matcher matcher = pattern.matcher(line);
          matcher.find();
          String fileName = matcher.groupCount() >= 2 ? matcher.group(1).trim() : null;
          if (fileName == null) {
            return null;
          } else {
            int index = fileName.indexOf(" ");
            return index != -1 ? fileName.substring(index).trim() : fileName;
          }
        }

        private String getError(String line) {
          Pattern pattern = Pattern.compile(":[0-9]+:[0-9]+:(.+)");
          Matcher matcher = pattern.matcher(line);
          matcher.find();
          return matcher.groupCount() >= 1 ? matcher.group(1).trim() : null;
        }

        private int getLineNumber(String line) {
          Pattern pattern = Pattern.compile(":([0-9]+):[0-9]+:");
          Matcher matcher = pattern.matcher(line);
          matcher.find();
          return matcher.groupCount() >= 1 ? Integer.parseInt(matcher.group(1)) : -1;
        }
      });
      t.setDaemon(true);
      t.start();
      process.waitFor();
      return process.exitValue() == 0;
    } catch (Exception var4) {
      var4.printStackTrace();
      return false;
    }
  }

  public static void executeNdk(String directory) {
    FileDescriptor build = new FileDescriptor(directory);
    String command = "ndk-build";
    startProcess(build.file(), command);
  }
}


