package com.phonygames.pengine.math;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public abstract class PSODynamics<T extends PVec<T>> implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PSODynamics1> pool1 = new PPool<PSODynamics1>() {
    @Override protected PSODynamics1 newObject() {
      return new PSODynamics1();
    }
  };
  private static final PPool<PSODynamics2> pool2 = new PPool<PSODynamics2>() {
    @Override protected PSODynamics2 newObject() {
      return new PSODynamics2();
    }
  };
  private static final PPool<PSODynamics3> pool3 = new PPool<PSODynamics3>() {
    @Override protected PSODynamics3 newObject() {
      return new PSODynamics3();
    }
  };
  private static final PPool<PSODynamics4> pool4 = new PPool<PSODynamics4>() {
    @Override protected PSODynamics4 newObject() {
      return new PSODynamics4();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final T goal, pos, vel;
  private final T goalPrev;
  private final PPool<T> tPool;
  protected float k1, k2, k3; // Dynamics constants.

  private PSODynamics(PPool<T> pool) {
    this.tPool = pool;
    goal = pool.obtain();
    goalPrev = pool.obtain();
    pos = pool.obtain();
    vel = pool.obtain();
    reset();
  }

  @Override public void reset() {
    goal.setZero();
  }

  public static PSODynamics1 obtain1() {
    return pool1.obtain();
  }

  public static PSODynamics2 obtain2() {
    return pool2.obtain();
  }

  public static PSODynamics3 obtain3() {
    return pool3.obtain();
  }

  public static PSODynamics4 obtain4() {
    return pool4.obtain();
  }

  public static float zetaFromMagnitudeRemainingPerPeriod(float freq, float magReduceFraction) {
    if (freq == 0) { // If omega is infinity, the spring has no mass and moves instantly.
      return 1;
    }
    return zetaFromMagnitudeRemainingPerTime(magReduceFraction, 1 / freq);
  }

  public static float zetaFromMagnitudeRemainingPerTime(float magReduceFraction, float time) {
    return (float) (Math.log(magReduceFraction) / (-4 * MathUtils.PI * time));
  }

  public void frameUpdate() {
    T goalDelta = tPool.obtain();
    goalDelta.set(goal).add(goalPrev, -1).scl(1f / PEngine.dt);
    goalPrev.set(goal);
    float k2Stable = Math.max(Math.max(k2, PEngine.dt * PEngine.dt / 2 + PEngine.dt * k1 / 2), PEngine.dt * k1);
    pos.add(vel, PEngine.dt);
    T temp = tPool.obtain().set(goal).add(goalDelta, k3).add(pos, -1).add(vel, -k1);
    vel.add(temp, PEngine.dt / k2Stable);
    temp.free();
  }

  public void setDynamicsParams(float freq, float zeta, float response) {
    k1 = zeta / (MathUtils.PI * freq);
    k2 = 1 / ((2 * MathUtils.PI * freq) * (2 * MathUtils.PI * freq));
    k3 = response * zeta / (2 * MathUtils.PI * freq);
  }

  public void setGoal(T goal) {
    this.goal.set(goal);
  }

  public void setVel(T vel) {
    this.vel.set(vel);
  }

  public static class PSODynamics1 extends PSODynamics<PVec1> {
    private PSODynamics1() {
      super(PVec1.getStaticPool());
    }

    public PSODynamics1 setGoal(float x) {
      this.goal().set(x);
      return this;
    }
  }

  public static class PSODynamics2 extends PSODynamics<PVec2> {
    private PSODynamics2() {
      super(PVec2.getStaticPool());
    }

    public PSODynamics2 setGoal(float x, float y) {
      this.goal().set(x, y);
      return this;
    }
  }

  public static class PSODynamics3 extends PSODynamics<PVec3> {
    private PSODynamics3() {
      super(PVec3.getStaticPool());
    }

    public PSODynamics3 setGoal(float x, float y, float z) {
      this.goal().set(x, y, z);
      return this;
    }
  }

  public static class PSODynamics4 extends PSODynamics<PVec4> {
    private PSODynamics4() {
      super(PVec4.getStaticPool());
    }

    public PSODynamics4 setGoal(float x, float y, float z, float w) {
      this.goal().set(x, y, z, w);
      return this;
    }
  }
}
