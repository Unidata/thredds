/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.ma2;

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

    Array result = Array.factory(a.getElementType(), a.getShape());

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

    int rankA = shapeA.length;
    int rankB = shapeB.length;

    int dimB = 0;
    for (int dimA = 0; dimA < rankA; dimA++) {
      //System.out.println(dimA + " "+ dimB);

      //skip length 1 dimensions
      if (shapeA[dimA] == 1)
        continue;
      while (dimB < rankB)
        if (shapeB[dimB] == 1) dimB++;
        else break;

      // test same shape (NB dimB cant be > rankB due to first test)
      if (shapeA[dimA] != shapeB[dimB])
        return false;
      dimB++;
    }

    return true;
  }

  /**
   * Convert unsigned data to signed data of a wider type.
   *
   * @param unsigned must be of type byte, short or int
   * @return converted data of type short, int, or long
   */
  public static Array convertUnsigned( Array unsigned) {
    if (unsigned.getElementType().equals(byte.class)) {
      Array result = Array.factory(DataType.SHORT, unsigned.getShape());
      IndexIterator ii = result.getIndexIterator();
      unsigned.resetLocalIterator();
      while (unsigned.hasNext())
        ii.setShortNext( DataType.unsignedByteToShort(unsigned.nextByte()));
      return result;

    } else if (unsigned.getElementType().equals(short.class)) {
      Array result = Array.factory(DataType.INT, unsigned.getShape());
      IndexIterator ii = result.getIndexIterator();
      unsigned.resetLocalIterator();
      while (unsigned.hasNext())
        ii.setIntNext( DataType.unsignedShortToInt(unsigned.nextShort()));
      return result;

    } else if (unsigned.getElementType().equals(int.class)) {
      Array result = Array.factory(DataType.LONG, unsigned.getShape());
      IndexIterator ii = result.getIndexIterator();
      unsigned.resetLocalIterator();
      while (unsigned.hasNext())
        ii.setLongNext( DataType.unsignedIntToLong(unsigned.nextInt()));
      return result;
    }

    throw new IllegalArgumentException("Cant convertUnsigned type= "+unsigned.getElementType());
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
    } else if (dataType == DataType.LONG) {
      while (from.hasNext())
        to.setLongNext(from.getLongNext());
    } else if ((dataType == DataType.INT) || (dataType == DataType.ENUM4)) {
      while (from.hasNext())
        to.setIntNext(from.getIntNext());
    } else if ((dataType == DataType.SHORT) || (dataType == DataType.ENUM2)) {
      while (from.hasNext())
        to.setShortNext(from.getShortNext());
    } else if (dataType == DataType.CHAR) {
      while (from.hasNext())
        to.setCharNext(from.getCharNext());
    } else if ((dataType == DataType.BYTE) || (dataType == DataType.ENUM1)) {
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
   * copy array a to array result, the result array will be in canonical order
   * The operation type is taken from the type of a.
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
   * @throws IllegalArgumentException a and result are not conformable
   */
  public static void copyObject(Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setObjectNext(iterA.getObjectNext());
  }

  /**
   * Calculate the reduced rank of this shape, by subtracting dimensions with length 1
   */
  public static int reducedRank(int[] shape) {
    int rank = 0;
    for (int ii = 0; ii < shape.length; ii++) {
      if (shape[ii] > 1)
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
    IndexIterator iter = a.getIndexIteratorFast();
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
    IndexIterator iter = a.getIndexIteratorFast();
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

  public static double getMinimumSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIteratorFast();
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val < min))
        min = val;
    }
    return min;
  }

  public static double getMaximumSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIteratorFast();
    double max = -Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val > max))
        max = val;
    }
    return max;
  }

  public static MAMath.MinMax getMinMaxSkipMissingData(Array a, double missingValue) {
    IndexIterator iter = a.getIndexIteratorFast();
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
   */
  public static void setDouble(Array result, double val) {
    IndexIterator iter = result.getIndexIteratorFast();
    while (iter.hasNext()) {
      iter.setDoubleNext(val);
    }
  }

  /**
   * sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   */
  public static double sumDouble(Array a) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIteratorFast();
    while (iterA.hasNext())
      sum += iterA.getDoubleNext();
    return sum;
  }

  /**
   * sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   */
  public static double sumDoubleSkipMissingData(Array a, double missingValue) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIteratorFast();
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
  }

}
