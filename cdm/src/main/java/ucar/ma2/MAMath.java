/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import ucar.nc2.util.Misc;

/**
 * Element by element algebra on Arrays
 *
 * @author caron
 * @see Index
 */
public class MAMath {

  /**
   * Add elements of two arrays together, allocating the result array.
   * The result type and the operation type are taken from the type of a.
   *
   * @param a add values from here
   * @param b add values from here
   * @return result = a + b
   * @throws IllegalArgumentException      a and b are not conformable
   * @throws UnsupportedOperationException dont support this data type yet
   */
  public static Array add(Array a, Array b) throws IllegalArgumentException {

    Array result = Array.factory(a.getDataType(), a.getShape());

    if (a.getElementType() == double.class) {
      addDouble(result, a, b);
    } else
      throw new UnsupportedOperationException();

    return result;
  }

  /**
   * Add elements of two arrays together as doubles, place sum in the result array.
   * The values from the arrays a and b are converted to double (if needed),
   * and the sum is converted to the type of result (if needed).
   *
   * @param result result array
   * @param a operand
   * @param b operand
   * @throws IllegalArgumentException a,b,and result are not conformable
   */
  public static void addDouble(Array result, Array a, Array b)
      throws IllegalArgumentException {

    if (!conformable(result, a) || !conformable(a, b))
      throw new IllegalArgumentException();

    IndexIterator iterR = result.getIndexIterator();
    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterB = b.getIndexIterator();

    while (iterA.hasNext())
      iterR.setDoubleNext(iterA.getDoubleNext() + iterB.getDoubleNext());
  }

  /**
   * Check that two arrays are conformable.
   *
   * @param a operand
   * @param b operand
   * @return true if conformable
   */
  public static boolean conformable(Array a, Array b) {
    return conformable(a.getShape(), b.getShape());
  }

  /**
   * Check that two array shapes are conformable.
   * The shapes must match exactly, except that dimensions of length 1 are ignored.
   *
   * @param shapeA shape of array 1
   * @param shapeB shape of array 2
   * @return true if conformable
   */
  public static boolean conformable(int[] shapeA, int[] shapeB) {
    if (reducedRank(shapeA) != reducedRank(shapeB))
      return false;

    int rankB = shapeB.length;

    int dimB = 0;
    for (int aShapeA : shapeA) {
      //System.out.println(dimA + " "+ dimB);

      //skip length 1 dimensions
      if (aShapeA == 1)
        continue;
      while (dimB < rankB)
        if (shapeB[dimB] == 1) dimB++;
        else break;

      // test same shape (NB dimB cant be > rankB due to first test)
      if (aShapeA != shapeB[dimB])
        return false;
      dimB++;
    }

    return true;
  }

  /**
   * Convert original array to desired type
   *
   * @param org original array
   * @param wantType desired type
   * @return converted data of desired type, or original array if it is already
   */
  public static Array convert( Array org, DataType wantType) {
    if (org == null) return null;
    Class wantClass = wantType.getPrimitiveClassType();
    if (org.getElementType().equals(wantClass))
      return org;

    Array result = Array.factory(wantType, org.getShape());
    copy(wantType, org.getIndexIterator(), result.getIndexIterator());
    return result;
  }

  /**
   * Copy using iterators. Will copy until !from.hasNext().
   *
   * @param dataType use this operation type (eg DataType.DOUBLE uses getDoubleNext())
   * @param from     copy from here
   * @param to       copy to here
   * @throws IllegalArgumentException      a and b are not conformable
   * @throws UnsupportedOperationException dont support this data type
   */
  public static void copy(DataType dataType, IndexIterator from, IndexIterator to) throws IllegalArgumentException {
    if (dataType == DataType.DOUBLE) {
      while (from.hasNext())
        to.setDoubleNext(from.getDoubleNext());
    } else if (dataType == DataType.FLOAT) {
      while (from.hasNext())
        to.setFloatNext(from.getFloatNext());
    } else if (dataType.getPrimitiveClassType() == long.class) {
      while (from.hasNext())
        to.setLongNext(from.getLongNext());
    } else if (dataType.getPrimitiveClassType() == int.class) {
      while (from.hasNext())
        to.setIntNext(from.getIntNext());
    } else if (dataType.getPrimitiveClassType() == short.class) {
      while (from.hasNext())
        to.setShortNext(from.getShortNext());
    } else if (dataType == DataType.CHAR) {
      while (from.hasNext())
        to.setCharNext(from.getCharNext());
    } else if (dataType.getPrimitiveClassType() == byte.class) {
      while (from.hasNext())
        to.setByteNext(from.getByteNext());
    } else if (dataType == DataType.BOOLEAN) {
      while (from.hasNext())
        to.setBooleanNext(from.getBooleanNext());
    } else {
      while (from.hasNext())
        to.setObjectNext(from.getObjectNext());
    }
  }

  /**
   * Copy array a to array result, the result array will be in canonical order
   * The operation type is taken from the type of a.
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException      a and b are not conformable
   * @throws UnsupportedOperationException dont support this data type yet
   */
  public static void copy(Array result, Array a) throws IllegalArgumentException {
    Class classType = a.getElementType();
    if (classType == double.class) {
      copyDouble(result, a);
    } else if (classType == float.class) {
      copyFloat(result, a);
    } else if (classType == long.class) {
      copyLong(result, a);
    } else if (classType == int.class) {
      copyInt(result, a);
    } else if (classType == short.class) {
      copyShort(result, a);
    } else if (classType == char.class) {
      copyChar(result, a);
    } else if (classType == byte.class) {
      copyByte(result, a);
    } else if (classType == boolean.class) {
      copyBoolean(result, a);
    } else
      copyObject(result, a);
  }

  /**
   * copy array a to array result as doubles
   * The values from the arrays a are converted to double (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyDouble(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setDoubleNext(iterA.getDoubleNext());
  }

  /**
   * copy array a to array result as floats
   * The values from the arrays a are converted to float (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyFloat(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setFloatNext(iterA.getFloatNext());
  }

  /**
   * copy array a to array result as longs
   * The values from the array a are converted to long (if needed),
   * and then converted to the type of result (if needed).
   * @param result copy to here
   * @param a copy from here
   *
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyLong(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setLongNext(iterA.getLongNext());
  }

  /**
   * copy array a to array result as integers
   * The values from the arrays a are converted to integer (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyInt(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setIntNext(iterA.getIntNext());
  }

  /**
   * copy array a to array result as shorts
   * The values from the array a are converted to short (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyShort(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setShortNext(iterA.getShortNext());
  }

  /**
   * copy array a to array result as char
   * The values from the array a are converted to char (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyChar(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setCharNext(iterA.getCharNext());
  }


  /**
   * copy array a to array result as bytes
   * The values from the array a are converted to byte (if needed),
   * and then converted to the type of result (if needed).
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyByte(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setByteNext(iterA.getByteNext());
  }

  /**
   * copy array a to array result as bytes
   * The array a and result must be type boolean
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyBoolean(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setBooleanNext(iterA.getBooleanNext());
  }

  /**
   * copy array a to array result as an Object
   * The array a and result must be type object
   *
   * @param result copy to here
   * @param a copy from here
   *
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyObject(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext()) {
      iterR.setObjectNext( iterA.getObjectNext());
    }
  }

  /**
   * Calculate the reduced rank of this shape, by subtracting dimensions with length 1
   * @param shape shape of the array
   * @return rank without dimensions of length 1
   */
  public static int reducedRank(int[] shape) {
    int rank = 0;
    for (int aShape : shape) {
      if (aShape > 1)
        rank++;
    }
    return rank;
  }

  public static double getMinimum(Array a) {
    IndexIterator iter = a.getIndexIterator();
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (Double.isNaN(val)) continue;
      if (val < min)
        min = val;
    }
    return min;
  }

  public static double getMaximum(Array a) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (Double.isNaN(val)) continue;
      if (val > max)
        max = val;
    }
    return max;
  }

  /**
   * Find min and max value in this array, getting values as doubles. Skip Double.NaN.
   *
   * @param a the array.
   * @return MinMax
   */
  public static MAMath.MinMax getMinMax(Array a) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (Double.isNaN(val)) continue;
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MinMax(min, max);
  }

  public static MAMath.MinMax getMinMaxSkipMissingData(Array a, IsMissingEvaluator eval) {
    if (eval == null || !eval.hasMissing())
      return MAMath.getMinMax(a);

    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (eval.isMissing(val))
        continue;
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MAMath.MinMax(min, max);
  }


  public static double getMinimumSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIterator();
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val < min))
        min = val;
    }
    return min;
  }

  public static double getMaximumSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val > max))
        max = val;
    }
    return max;
  }

  public static MAMath.MinMax getMinMaxSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (val == missingValue)
        continue;
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MinMax(min, max);
  }


  /**
   * Set all the elements of this array to the given double value.
   * The value is converted to the element type of the array, if needed.
   *
   * @param result change this Array
   * @param val set all elements to this value
   */
  public static void setDouble(Array result, double val) {
    IndexIterator iter = result.getIndexIterator();
    while (iter.hasNext()) {
      iter.setDoubleNext(val);
    }
  }

  /**
   * sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   * @param a read values from this Array
   * @return sum of elements
   */
  public static double sumDouble(Array a) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIterator();
    while (iterA.hasNext()) {
      sum += iterA.getDoubleNext();
    }
    return sum;
  }

  /**
   * sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   * @param a read values from this Array
   * @param missingValue skip values equal to this, or which are NaNs
   * @return sum of elements
   */
  public static double sumDoubleSkipMissingData(Array a, double missingValue) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIterator();
    while (iterA.hasNext()) {
      double val = iterA.getDoubleNext();
      if ((val == missingValue) || Double.isNaN(val))
        continue;
      sum += val;
    }
    return sum;
  }

  /**
   * Holds a minimum and maximum value.
   */
  public static class MinMax {
    public double min, max;

    public MinMax(double min, double max) {
      this.min = min;
      this.max = max;
    }

    @Override
    public String toString() {
      return "MinMax{" +
              "min=" + min +
              ", max=" + max +
              '}';
    }
  }

  /**
   * Calculate the scale/offset for an array of numbers.
   * <pre>
   * If signed:
   *   then
   *     max value unpacked = 2^(n-1) - 1 packed
   *     min value unpacked = -(2^(n-1) - 1) packed
   *   note that -2^(n-1) is unused, and a good place to map missing values
   *   by solving 2 eq in 2 unknowns, we get:
   *     scale = (max - min) / (2^n - 2)
   *     offset = (max + min) / 2
   * If unsigned then
   *     max value unpacked = 2^n - 1 packed
   *     min value unpacked = 0 packed
   *   and:
   *     scale = (max - min) / (2^n - 1)
   *     offset = min
   *   One could modify this to allow a holder for missing values.
   * </pre>
   * @param a array to convert (not changed)
   * @param missingValue skip these
   * @param nbits map into this many bits
   * @return ScaleOffset, calculated as above.
   */
  public static MAMath.ScaleOffset calcScaleOffsetSkipMissingData(Array a, double missingValue, int nbits) {
    MAMath.MinMax minmax = getMinMaxSkipMissingData(a, missingValue);

    if (a.isUnsigned()) {
      long size = (1L << nbits) - 1;
      double offset = minmax.min;
      double scale =(minmax.max - minmax.min) / size;
      return new ScaleOffset(scale, offset);

    } else {
      long size = (1L << nbits) - 2;
      double offset = (minmax.max + minmax.min) / 2;
      double scale =(minmax.max - minmax.min) / size;
      return new ScaleOffset(scale, offset);
    }
  }

  public static Array convert2packed(Array unpacked, double missingValue, int nbits, DataType packedType) {
    MAMath.ScaleOffset scaleOffset = calcScaleOffsetSkipMissingData(unpacked, missingValue, nbits);
    Array result = Array.factory(packedType, unpacked.getShape());
    IndexIterator riter = result.getIndexIterator();
    while (unpacked.hasNext()) {
      double uv = unpacked.nextDouble();
      double pv = (uv - scaleOffset.offset) / scaleOffset.scale;
      riter.setDoubleNext( pv);
    }
    return result;
  }

  public static Array convert2Unpacked(Array packed, ScaleOffset scaleOffset) {
    Array result = Array.factory(DataType.DOUBLE, packed.getShape());
    IndexIterator riter = result.getIndexIterator();
    while (packed.hasNext())  {
      riter.setDoubleNext( packed.nextDouble() * scaleOffset.scale + scaleOffset.offset);
    }
    return result;
  }

  /**
   * Holds a scale and offset.
   */
  public static class ScaleOffset {
    public double scale, offset;

    public ScaleOffset(double scale, double offset) {
      this.scale = scale;
      this.offset = offset;
    }
  }

  /**
   * Returns true if the specified arrays have the same size, signedness, and <b>approximately</b> equal corresponding
   * elements. {@code float} elements must be within {@link Misc#defaultMaxRelativeDiffFloat} of each other, as
   * determined by {@link Misc#nearlyEquals(double, double, double)}. Similarly, {@code double} elements must be within
   * {@link Misc#defaultMaxRelativeDiffDouble} of each other.
   * <p>
   * {@link #equals(Array, Array)} is an alternative to this method that requires that corresponding elements be
   * <b>exactly</b> equal. It is suitable for use in {@link Object#equals} implementations, whereas this method isn't.
   *
   * @param data1  one array to be tested for equality.
   * @param data2  the other array to be tested for equality.
   * @return true if the specified arrays have the same size, signedness, and approximately equal corresponding elems.
   */
  public static boolean nearlyEquals(Array data1, Array data2) {
    if (data1 == data2) {  // Covers case when both are null.
      return true;
    } else if (data1 == null || data2 == null) {
      return false;
    }

    if (data1.getSize() != data2.getSize()) return false;
    if (data1.isUnsigned() != data2.isUnsigned()) return false;
    DataType dt = DataType.getType(data1);

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (dt == DataType.DOUBLE) {
      while (iter1.hasNext() && iter2.hasNext()) {
        double v1 = iter1.getDoubleNext();
        double v2 = iter2.getDoubleNext();
        if (!Misc.nearlyEquals(v1, v2, Misc.defaultMaxRelativeDiffDouble))
          return false;
      }
    } else if (dt == DataType.FLOAT) {
      while (iter1.hasNext() && iter2.hasNext()) {
        float v1 = iter1.getFloatNext();
        float v2 = iter2.getFloatNext();
        if (!Misc.nearlyEquals(v1, v2, Misc.defaultMaxRelativeDiffFloat))
          return false;
      }
    } else if (dt.getPrimitiveClassType() == int.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        int v1 = iter1.getIntNext();
        int v2 = iter2.getIntNext();
        if (v1 != v2) return false;
      }
    } else if (dt.getPrimitiveClassType() == byte.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        short v1 = iter1.getShortNext();
        short v2 = iter2.getShortNext();
        if (v1 != v2) return false;
      }
    } else if (dt.getPrimitiveClassType() == short.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        byte v1 = iter1.getByteNext();
        byte v2 = iter2.getByteNext();
        if (v1 != v2) return false;
      }
    } else if (dt.getPrimitiveClassType() == long.class) {
      while (iter1.hasNext() && iter2.hasNext()) {
        long v1 = iter1.getLongNext();
        long v2 = iter2.getLongNext();
        if (v1 != v2) return false;
      }
    } else {
      while (iter1.hasNext() && iter2.hasNext()) {
        if (!Objects.equals(iter1.next(), iter2.next())) {
          return false;
        }
      }
    }

    return true;
  }


  /**
   * Returns true if the specified arrays have the same data type, shape, and equal corresponding elements. This method
   * is suitable for use in {@link Object#equals} implementations.
   * <p>
   * Note that floating-point elements must be exactly equal, not merely within some epsilon of each other. This is
   * because it's <b>impossible</b> to write a strictly-conforming {@link Object#equals} implementation when an
   * epsilon is incorporated, due to the transitivity requirement.
   * <p>
   * {@link #nearlyEquals} is an alternative to this method that returns true if the corresponding elements are
   * "approximately" equal to each other.
   *
   * @param array1 one array to be tested for equality.
   * @param array2 the other array to be tested for equality.
   * @return true if the specified arrays have the same data type, shape, and equal corresponding elements.
   * @see <a href=http://goo.gl/psfLb>Is there a way to get a hashcode of a float with epsilon? - Stack Overflow</a>
   */
  // TODO: Should we add this to Array as the Object.equals() implementation? How much work is that?
  public static boolean equals(Array array1, Array array2) {
    if (array1 == array2) {  // Covers case when both are null.
      return true;
    } else if (array1 == null || array2 == null) {
      return false;
    }

    // MAMath.nearlyEquals() does not require DataTypes to be equal, but in so doing, it becomes non-symmetric.
    // For example, suppose we have 2 arrays:
    //     ArrayLong  al;
    //     ArrayShort as;
    // If al contains elements that don't fit in a short, we could have the following:
    //     MAMath.nearlyEquals(al, as);  // true
    //     MAMath.nearlyEquals(as, al);  // false
    // This is because when MAMath.nearlyEquals() does comparisons, elements from the 2nd array are converted to the
    // type of the 1st array.
    //
    // In our implementation, we avoid this problem--and thus preserve symmetry--by insisting that the element
    // types of the 2 arrays are the same. This means that we lose some of the "flexibility" offered by the MAMath
    // version, but it turns out that it's not a good idea to compare different Number subtypes in the first place:
    // http://stackoverflow.com/questions/480632/why-doesnt-java-lang-number-implement-comparable
    if (array1.getDataType() != array2.getDataType()) {
      return false;
    }

    // MAMath.nearlyEquals() only requires that the 2 arrays have the same size, not the same shape. That was an
    // option I considered. Also, there's MAMath.conformable(), which returns true if shapes are equal after
    // reduction (e.g. { 3,4,5 } and { 3,1,4,1,5 } are conformable). By definition, conformable arrays have
    // the same size.
    //
    // Ultimately, I decided on the stricter requirement that the arrays must have the exact same shape. That's
    // because if 2 objects compare as equal, there's a general expectation that you can perform the same
    // operations on them and get the same result. But imagine this:
    //     Array a1 = Array.factory(DataType.INT, new int[] { 3,4 }, new int[] { 0,1,2,3,4,5,6,7,8,9,10,11 });
    //     Array a2 = Array.factory(DataType.INT, new int[] { 2,6 }, new int[] { 0,1,2,3,4,5,6,7,8,9,10,11 });
    // MAMath.nearlyEquals() will consider the arrays equal because they have the same size, but:
    //     a1.getInt(a1.getIndex().set(1, 1)) == 5
    //     a2.getInt(a2.getIndex().set(1, 1)) == 7
    if (!Arrays.equals(array1.getShape(), array2.getShape())) {
      return false;
    }

    // Note that we did not examine the Indexes of the arrays, nor their 1D backing stores. That's because we're
    // interested in the VIEWS of the data that the Arrays present, not with how the data is manipulated behind the
    // scenes.

    IndexIterator iter1 = array1.getIndexIterator();
    IndexIterator iter2 = array2.getIndexIterator();

    while (iter1.hasNext() && iter2.hasNext()) {
      if (!Objects.equals(iter1.next(), iter2.next())) {
        return false;
      }
      if (!Arrays.equals(iter1.getCurrentCounter(), iter2.getCurrentCounter())) {
        return false;
      }
    }

    assert !iter1.hasNext() && !iter2.hasNext();    // Iterators ought to be the same length.
    return true;
  }

  /**
   * An implementation of {@link Object#hashCode} that is consistent with {@link #equals(Array, Array)}.
   *
   * @param array an array to hash.
   * @return a hash code value for the array.
   */
  // TODO: Should we add this to Array as the Object.hashCode() implementation? How much work is that?
  public static int hashCode(Array array) {
    if (array == null) {
      return 0;
    }

    int hash = 3;
    hash = 29 * hash + array.getDataType().hashCode();
    hash = 29 * hash + Arrays.hashCode(array.getShape());

    // We can't simply hash array.getStorage(), because array may be a "view" that doesn't include all of the
    // elements in the backing store.
    for (IndexIterator iter = array.getIndexIterator(); iter.hasNext(); ) {
      hash = 29 * hash + iter.next().hashCode();
      hash = 29 * hash + Arrays.hashCode(iter.getCurrentCounter());
    }

    return hash;
  }
}
