package com.phonygames.pengine.navmesh.recast;

import org.recast4j.recast.geom.InputGeomProvider;
import org.recast4j.recast.geom.SimpleInputGeomProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PRecastObjImporter {
  ObjImporterContext context;

  public InputGeomProvider load(InputStream is) {
    context = new ObjImporterContext();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        readLine(line, context);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    }
    return new SimpleInputGeomProvider(context.vertexPositions, context.meshFaces);
  }

  private void readLine(String line, ObjImporterContext context) {
    if (line.startsWith("v")) {
      readVertex(line, context);
    } else if (line.startsWith("f")) {
      readFace(line, context);
    }
  }

  private void readVertex(String line, ObjImporterContext context) {
    if (line.startsWith("v ")) {
      float[] vert = readVector3f(line);
      for (float vp : vert) {
        context.vertexPositions.add(vp);
      }
    }
  }

  private void readFace(String line, ObjImporterContext context) {
    String[] v = line.split("\\s+");
    if (v.length < 4) {
      throw new RuntimeException("Invalid number of face vertices: 3 coordinates expected, found " + v.length);
    }
    for (int j = 0; j < v.length - 3; j++) {
      context.meshFaces.add(readFaceVertex(v[1], context));
      for (int i = 0; i < 2; i++) {
        context.meshFaces.add(readFaceVertex(v[2 + j + i], context));
      }
    }
  }

  private float[] readVector3f(String line) {
    String[] v = line.split("\\s+");
    if (v.length < 4) {
      throw new RuntimeException("Invalid vector, expected 3 coordinates, found " + (v.length - 1));
    }
    return new float[]{Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3])};
  }

  private int readFaceVertex(String face, ObjImporterContext context) {
    String[] v = face.split("/");
    return getIndex(Integer.parseInt(v[0]), context.vertexPositions.size());
  }

  private int getIndex(int posi, int size) {
    if (posi > 0) {
      posi--;
    } else if (posi < 0) {
      posi = size + posi;
    } else {
      throw new RuntimeException("0 vertex index");
    }
    return posi;
  }

  public class ObjImporterContext {
    List<Integer> meshFaces = new ArrayList<>();
    List<Float> vertexPositions = new ArrayList<>();
  }
}
