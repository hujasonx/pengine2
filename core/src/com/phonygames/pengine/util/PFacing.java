package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

/** An enum describing possible facing directions in an xz grid. */
public enum PFacing {
  X, Z, mX, mZ;

  /** The opposite facing. */
  public PFacing opposite() {
    switch (this) {
      case X:
        return mX;
      case Z:
        return mZ;
      case mX:
        return X;
      case mZ:
      default:
        return Z;
    }
  }

  /** The facing that results from turning left. */
  public PFacing left() {
    switch (this) {
      case X:
        return mZ;
      case Z:
        return X;
      case mX:
        return Z;
      case mZ:
      default:
        return mX;
    }
  }

  /** The facing that results from turning right. */
  public PFacing right() {
    return left().opposite();
  }

  /** The the x component of the forward vector. */
  public int forwardX() {
    switch (this) {
      case X:
        return 1;
      case mX:
        return -1;
      case Z:
      case mZ:
      default:
        return 0;
    }
  }

  /** The the z component of the forward vector. */
  public int forwardZ() {
    switch (this) {
      case Z:
        return 1;
      case mZ:
        return -1;
      case X:
      case mX:
      default:
        return 0;
    }
  }

  /** The int value of this facing. */
  public int intValue() {
      switch (this) {
        case X:
          return 0;
        case Z:
          return 1;
        case mX:
          return 2;
        case mZ:
          return 3;
        default:
          PAssert.fail("Should not reach");
          return -1;
    }
  }

  /** Gets the facing from the int value. */
  public static PFacing get(int intValue) {
    switch (intValue){
      case 0:
        return X;
      case 1:
        return Z;
      case 2:
        return mX;
      case 3:
        return mZ;
      default:
        PAssert.fail("Should not reach");
        return X;
    }
  }

  /** The number of possible facings. */
  public static int count() {
    return 4;
  }
}
