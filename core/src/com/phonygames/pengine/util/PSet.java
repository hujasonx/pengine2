package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.val;

/**
 * List class whose iterator() does not allocate and will always be in order.
 */
public class PSet<E> extends PPooledIterable<E> {
  private static final int[] SAMPLED_PRIMES =
      new int[]{23, 29, 37, 41, 47, 53, 59, 67, 79, 89, 101, 113, 127, 149, 167, 191, 211, 233, 257, 283, 313, 347, 383,
                431, 479, 541, 599, 659, 727, 809, 907, 1009, 1117, 1229, 1361, 1499, 1657, 1823, 2011, 2213, 2437,
                2683, 2953, 3251, 3581, 3943, 4339, 4783, 5273, 5801, 6389, 7039, 7753, 8537, 9391, 10331, 11369, 12511,
                13763, 15149, 16673, 18341, 20177, 22229, 24469, 26921, 29629, 32603, 35869, 39461, 43411, 47777, 52561,
                57829, 63617, 69991, 76991, 84691, 93169, 102497, 112757, 124067, 136481, 150131, 165161, 181693,
                199873, 219871, 241861, 266051, 292661, 321947, 354143, 389561, 428531, 471389, 518533, 570389, 627433,
                690187, 759223, 835207, 918733, 1010617, 1111687, 1222889, 1345207, 1479733, 1627723, 1790501, 1969567,
                2166529, 2383219, 2621551, 2883733, 3172123, 3489347, 3838283, 4222117, 4644347, 5108813, 5619701,
                6181699, 6799889, 7479887, 8227883, 9050687, 9955783, 10951363, 12046553, 13251233, 14576381, 16034021,
                17637437, 19401197, 21341317, 23475457, 25823011, 28405337, 31245887, 34370477, 37807529, 41588299,
                45747139, 50321863, 55354099, 60889523, 66978481, 73676333, 81043979, 89148377, 98063219, 107869547,
                118656547, 130522207, 143574439, 157931881, 173725103, 191097619, 210207391, 231228161, 254351011,
                279786131, 307764781, 338541253, 372395399, 409634959, 450598531, 495658417, 545224271, 599746691,
                659721389, 725693587, 798262921, 878089241, 965898187, 1062488003, 1168736809, 1285610587, 1414171793,
                1555588997, 1711147927, 1882262803, 2070489097};
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PPool<PPooledIterable.PPoolableIterator<E>> iteratorPool =
      new PPool<PPooledIterable.PPoolableIterator<E>>() {
        @Override protected PPooledIterable.PPoolableIterator<E> newObject() {
          return new PSetIterator<>();
        }
      };
  Object[] elements;
  boolean[] invalid;
  int numElements, numInvalid;
  private float densityTolerance = .15f;
  // The density that the buffer should be filled, with either invalid or elements.
  private float desiredDensity = .6f;
  private int numTaintedCellsToRegen = -1;

  public PSet() {
  }

  /**
   * Returns a nearby (higher) prime number.
   * @param a
   * @return A prime number >= a.
   */
  public static int sampledHigherPrime(float a) {
    if (a < SAMPLED_PRIMES[0]) {
      return SAMPLED_PRIMES[0];
    }
    int rangeLower = 0, rangeHigher = SAMPLED_PRIMES.length - 1;
    int bestGuess = SAMPLED_PRIMES[SAMPLED_PRIMES.length - 1];
    // Binary search for a prime.
    while (rangeHigher > rangeLower) {
      int guessIndex = (rangeHigher + rangeLower) / 2;
      int primeAtGuess = SAMPLED_PRIMES[guessIndex];
      if (primeAtGuess < bestGuess && primeAtGuess >= a) {
        bestGuess = primeAtGuess;
      }
      if (a > primeAtGuess) {
        rangeLower = guessIndex + 1;
      } else if (a < primeAtGuess) {
        rangeHigher = guessIndex;
      } else {
        return primeAtGuess;
      }
    }
    return bestGuess;
  }

  public boolean add(E e) {
    regenBuffersIfNeeded();
    int index = indexOfElementInternal(e);
    if (index == -1) {
      regenBuffers();
      return add(e);
    } else {
      if (elements[index] != null) {
        return false;
      }
      numElements++;
      elements[index] = e;
      return true;
    }
  }

  public boolean addAll(Collection<? extends E> collection) {
    for (val v : collection) {
      add(v);
    }
    return true;
  }

  private boolean addNeverRegen(E e) {
    if (e == null) {
      return false;
    }
    int index = indexOfElementInternal(e);
    if (index == -1) {
      PAssert.fail("This method will never regen, but this set needs to be regened.");
      return false;
    } else {
      if (elements[index] != null) {
        return false;
      }
      numElements++;
      elements[index] = e;
      return true;
    }
  }

  public void clear() {
    numElements = 0;
    numInvalid = 0;
    elements = new Object[SAMPLED_PRIMES[0]];
    invalid = new boolean[SAMPLED_PRIMES[0]];
  }

  public boolean containsAll(Collection<?> collection) {
    for (val v : collection) {
      if (!contains(v)) {
        return false;
      }
    }
    return true;
  }

  public boolean contains(@NonNull Object o) {
    int index = indexOfElementInternal((E) o);
    return index != -1 && elements[index] != null;
  }

  /**
   * Returns the index of the element in the elements list. It can point to an empty
   * cell if the object does not exist in the set.
   * @param e
   * @return The index, -1 if there is not space to insert the element.
   */
  int indexOfElementInternal(@NonNull E e) {
    int hash = e.hashCode();
    for (int a = 0; a < elements.length; a++) {
      int index = PNumberUtils.mod(hash + a, elements.length);
      if (!invalid[index]) {
        if (elements[index] != null) {
          if (elements[index].equals(e)) {
            return index;
          }
        } else {
          // If the cell is empty and valid, that means the object does not exist.
          return index;
        }
      }
    }
    return -1;
  }

  public boolean isEmpty() {
    return numElements == 0;
  }

  @Override public final PPooledIterable.PPoolableIterator<E> obtainIterator() {
    PSetIterator<E> ret = (PSetIterator<E>) iteratorPool().obtain();
    ret.returnPool = iteratorPool();
    ret.set = this;
    return ret;
  }

  private void regenBuffers() {
    numInvalid = 0;
    int bufferSize = sampledHigherPrime(numElements / desiredDensity);
    Object[] elementsPrev = elements;
    boolean[] invalidPrev = invalid;
    numTaintedCellsToRegen = Math.min((int) (bufferSize * (desiredDensity + densityTolerance)), bufferSize - 2);
    invalid = new boolean[bufferSize];
    elements = new Object[bufferSize];
    if (elementsPrev != null) {
      for (int a = 0; a < elementsPrev.length; a++) {
        if (!invalidPrev[a]) {
          addNeverRegen((E) elementsPrev[a]);
        }
      }
    }
  }

  private void regenBuffersIfNeeded() {
    int numTaintedCells = numInvalid + numElements;
    if (numTaintedCells >= numTaintedCellsToRegen) {
      regenBuffers();
    }
  }

  public boolean removeAll(Collection<?> collection) {
    for (val v : collection) {
      remove(v);
    }
    return true;
  }

  public boolean remove(Object o) {
    int index = indexOfElementInternal((E) o);
    if (index != -1 && elements[index] != null) {
      invalid[index] = true;
      elements[index] = null;
      numInvalid++;
      numElements--;
      return true;
    }
    return false;
  }

  public int size() {
    return numElements;
  }

  public Object[] toArray() {
    Object[] arr = new Object[numElements];
    int index = 0;
    for (int a = 0; a < elements.length; a++) {
      if (!invalid[a]) {
        arr[index++] = elements[a];
      }
    }
    return arr;
  }

  public <T> T[] toArray(T[] ts) {
    PAssert.failNotImplemented("toArray");
    return null;
  }

  private class PSetIterator<E> extends PPooledIterable.PPoolableIterator<E> {
    private static final int UNSET = -1, AT_END = -2;
    private boolean active = false;
    private int currentIndex = -1;
    // -1 if not found/unset, -2 if at end.
    private int nextIndex = UNSET;
    private PSet<E> set;

    public PSetIterator() {
    }

    @Override public boolean hasNext() {
      return calcNextIndex() != -2;
    }

    private int calcNextIndex() {
      if (nextIndex != UNSET) {
        return nextIndex;
      }
      if (set.elements == null) {
        return nextIndex = AT_END;
      }
      for (int a = currentIndex + 1; a < set.elements.length; a++) {
        if (set.elements[a] != null && !set.invalid[a]) {
          return nextIndex = a;
        }
      }
      return nextIndex = AT_END;
    }

    @Override public E next() {
      currentIndex = nextIndex;
      nextIndex = UNSET;
      calcNextIndex();
      return (E) set.elements[currentIndex];
    }

    @Override public void remove() {
      set.invalid[currentIndex] = true;
      set.numElements--;
      set.numInvalid++;
    }

    @Override public void reset() {
      currentIndex = UNSET;
      nextIndex = UNSET;
      calcNextIndex();
    }
  }
}
