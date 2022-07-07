package com.phonygames.pengine.util;

public interface PSortableByScore<T> extends Comparable<PSortableByScore<T>> {
  float score();

  @Override public default int compareTo(PSortableByScore<T> other) {
    float score = score();
    float otherScore = other.score();
    if (score > otherScore) {return 1;}
    if (score < otherScore) {return -1;}
    return 0;
  }
}
