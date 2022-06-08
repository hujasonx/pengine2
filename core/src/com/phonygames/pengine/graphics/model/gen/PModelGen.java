package com.phonygames.pengine.graphics.model.gen;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.graphics.model.PVertexAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.NonNull;

public class PModelGen {
  private final Map<String, Part> parts = new HashMap<>();
  private final Map<String, Part> physicsParts = new HashMap<>();

  public Part addPart(String name, PVertexAttributes vertexAttributes) {
    return new Part(name, false, vertexAttributes);
  }

  public Part addPhysicsPart(String name) {
    return new Part(name, true, PVertexAttributes.Templates.PHYSICS);
  }

  static class Part {
    private final PVertexAttributes vertexAttributes;
    @Getter
    private final String name;
    @Getter
    private final Optional<String> footstepSoundStrategy = Optional.empty();
    @Getter
    private final boolean isPhysicsPart;

    private final float[] currentVertexValues;


    private Part(@NonNull String name, boolean isPhysicsPart, PVertexAttributes vertexAttributes) {
      this.name = name;
      this.isPhysicsPart = isPhysicsPart;
      this.vertexAttributes = vertexAttributes;

      currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
    }
  }
}
