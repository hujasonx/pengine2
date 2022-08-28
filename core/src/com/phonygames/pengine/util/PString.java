package com.phonygames.pengine.util;

import com.badlogic.gdx.utils.StringBuilder;
import com.phonygames.pengine.exception.PAssert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PString extends PBasic<PString> {
  @Getter(value = AccessLevel.PUBLIC)
  private static final PPool<PString> staticPool = new PPool<PString>() {
    @Override public PString newObject() {
      return new PString();
    }
  };
  private final StringBuilder stringBuilder = new StringBuilder();
  private String value;
  private boolean valueDirty;

  private PString() {
    reset();
  }

  public PString append(String value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public static PString obtain() {
    return staticPool.obtain();
  }

  public PString appendBr() {
    stringBuilder.append('\n');
    valueDirty = true;
    return this;
  }

  public PString append(float value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString append(boolean value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString append(int value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString append(double value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString append(PString value) {
    stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString clear() {
    value = "";
    stringBuilder.clear();
    valueDirty = false;
    return this;
  }

  public PString substring(PString out, int start, int end) {
    out.set(stringBuilder.substring(start, end));
    return out;
  }

  public PString substring(PString out, int start) {
    out.set(stringBuilder.substring(start));
    return out;
  }

  public char charAt(int index) {
    return stringBuilder.charAt(index);
  }

  @Override public int compareTo(PString pString) {
    PAssert.failNotImplemented("compareTo"); // TODO: FIXME
    return 0;
  }

  @Override public boolean equalsT(PString pString) {
    PAssert.failNotImplemented("equalsT"); // TODO: FIXME
    return false;
  }

  public int length() {
    return this.stringBuilder.length();
  }

  @Override public void reset() {
    valueDirty = false;
    value = "";
    stringBuilder.clear();

  }

  public PString set(String value) {
    this.value = value;
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = false;
    return this;
  }

  public PString set(int value) {
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString set(float value) {
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString set(boolean value) {
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString set(short value) {
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString set(double value) {
    this.stringBuilder.clear();
    this.stringBuilder.append(value);
    valueDirty = true;
    return this;
  }

  public PString set(Object o) {
    this.stringBuilder.clear();
    this.stringBuilder.append(o);
    valueDirty = true;
    return this;
  }

  @Override protected PPool<PString> staticPool() {
    return staticPool;
  }

  @Override public PString set(PString other) {
    value = other.toString();
    stringBuilder.clear();
    stringBuilder.append(value);
    valueDirty = false;
    return this;
  }

  @Override public String toString() {
    if (valueDirty) {
      value = stringBuilder.toString();
    }
    return value;
  }
}
