package com.phonygames.pengine.graphics.animation;

import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PModel;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;

import lombok.Getter;

public class PAnimation {
  @Getter
  private final String name;

  private PAnimation(String name) {
    this.name = name;
  }

  public static class Builder extends PBuilder {
    private final PAnimation animation;

    public Builder(String name) {
      animation = new PAnimation(name);
    }

    public PAnimation build() {
      lockBuilder();
      return animation;
    }
  }
}
