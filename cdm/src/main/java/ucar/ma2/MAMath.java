// $Id: MAMath.java,v 1.5 2006/02/16 23:02:31 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * @see Index
 * @author @caron
 * @version $Revision: 1.5 $ $Date: 2006/02/16 23:02:31 $
 */
public class MAMath {

  /** Add elements of two arrays together, allocating the result array.
   *  The result type and the operation type are taken from the type of a.
   * @return result = a + b
   * @exception IllegalArgumentException a and b are not conformable
   * @exception UnsupportedOperationException dont support this data type yet
   */
  public static Array add( Array a, Array b) throws IllegalArgumentException {

    Array result = Array.factory( a.getElementType(), a.getShape());

    if (a.getElementType() == double.class) {
      addDouble( result, a , b);
    } else
      throw new UnsupportedOperationException();

    return result;
  }

  /** Add elements of two arrays together as doubles, place sum in the result array.
   * The values from the arrays a and b are converted to double (if needed),
   * and the sum is converted to the type of result (if needed).
   * @exception IllegalArgumentException a,b,and result are not conformable
   */
  public static void addDouble( Array result, Array a, Array b)
        throws IllegalArgumentException {

    if (!conformable(result, a) || !conformable(a, b))
      throw new IllegalArgumentException();

    IndexIterator iterR = result.getIndexIterator();
    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterB = b.getIndexIterator();

    while (iterA.hasNext())
      iterR.setDoubleNext(iterA.getDoubleNext() + iterB.getDoubleNext());

  }

  /** Check that two arrays are conformable.
   *  @return true if conformable
   */
  public static boolean conformable(Array a, Array b) {
    return conformable(a.getShape(), b.getShape());
  }
  /** Check that two array shapes are conformable.
   *  The shapes must match exactly, except that dimensions of length 1 are ignored.
   *  @return true if conformable
   */
  public static boolean conformable(int [] shapeA, int [] shapeB) {
    if (reducedRank(shapeA) != reducedRank( shapeB))
      return false;

    int rankA = shapeA.length;
    int rankB = shapeB.length;

    int dimB = 0;
    for (int dimA=0; dimA<rankA; dimA++) {
      //System.out.println(dimA + " "+ dimB);

        //skip length 1 dimensions
      if (shapeA[dimA] == 1)
        continue;
      while (dimB < rankB)
        if (shapeB[dimB] == 1) dimB++; else break;

        // test same shape (NB dimB cant be > rankB due to first test)
      if (shapeA[dimA] != shapeB[dimB])
        return false;
      dimB++;
    }

    return true;
  }

  /** copy array a to array result, the result array will be in canonical order
   *  The operation type is taken from the type of a.
   * @exception IllegalArgumentException a and b are not conformable
   * @exception UnsupportedOperationException dont support this data type yet
   */
  public static void copy( Array result, Array a) throws IllegalArgumentException {
    Class classType = a.getElementType();
    if (classType == double.class) {
      copyDouble( result, a);
    } else if (classType == float.class) {
      copyFloat( result, a);
    } else if (classType == long.class) {
      copyLong( result, a);
    } else if (classType == int.class) {
      copyInt( result, a);
    } else if (classType == short.class) {
      copyShort( result, a);
    } else if (classType == char.class) {
      copyChar( result, a);
    } else if (classType == byte.class) {
      copyByte( result, a);
    } else if (classType == boolean.class) {
      copyBoolean( result, a);
    } else
      copyObject( result, a);
  }

  /** copy array a to array result as doubles
   * The values from the arrays a are converted to double (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyDouble( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException("copy arrays are not conformable");

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setDoubleNext(iterA.getDoubleNext());
  }

  /** copy array a to array result as floats
   * The values from the arrays a are converted to float (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyFloat( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setFloatNext(iterA.getFloatNext());
  }

  /** copy array a to array result as longs
   * The values from the array a are converted to long (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyLong( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setLongNext(iterA.getLongNext());
  }


  /** copy array a to array result as integers
   * The values from the arrays a are converted to integer (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyInt( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setIntNext(iterA.getIntNext());
  }

  /** copy array a to array result as shorts
   * The values from the array a are converted to short (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyShort( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setShortNext(iterA.getShortNext());
  }

  /** copy array a to array result as char
   * The values from the array a are converted to char (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyChar( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setCharNext(iterA.getCharNext());
  }


  /** copy array a to array result as bytes
   * The values from the array a are converted to byte (if needed),
   * and then converted to the type of result (if needed).
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyByte( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setByteNext(iterA.getByteNext());
  }

   /** copy array a to array result as bytes
   * The array a and result must be type boolean
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyBoolean( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setBooleanNext(iterA.getBooleanNext());
  }

  /** copy array a to array result as an Object
   * The array a and result must be type object
   * @exception IllegalArgumentException a and result are not conformable
   */
  public static void copyObject( Array result, Array a) throws IllegalArgumentException {
    if (!conformable(a, result))
      throw new IllegalArgumentException();

    IndexIterator iterA = a.getIndexIterator();
    IndexIterator iterR = result.getIndexIterator();
    while (iterA.hasNext())
      iterR.setObjectNext(iterA.getObjectNext());
  }

  /** Calculate the reduced rank of this shape, by subtracting dimensions with length 1 */
  public static int reducedRank( int [] shape) {
    int rank = 0;
    for (int ii=0; ii< shape.length; ii++) {
      if (shape[ii] > 1)
        rank++;
    }
    return rank;
  }

  public static double getMinimum( Array a) {
    IndexIterator iter = a.getIndexIterator();
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (val < min)
        min = val;
    }
    return min;
  }

  public static double getMaximum( Array a) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (val > max)
        max = val;
    }
    return max;
  }

  public static MAMath.MinMax getMinMax( Array a) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if (val > max)
        max = val;
      if (val < min)
        min = val;
    }
    return new MinMax(min,max);
  }

  public static double getMinimumSkipMissingData( Array a, double missingValue) {
    IndexIterator iter = a.getIndexIterator();
    double min = Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val < min))
        min = val;
    }
    return min;
  }

  public static double getMaximumSkipMissingData( Array a, double missingValue) {
    IndexIterator iter = a.getIndexIterator();
    double max = -Double.MAX_VALUE;
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      if ((val != missingValue) && (val > max))
        max = val;
    }
    return max;
  }

  public static MAMath.MinMax getMinMaxSkipMissingData( Array a, double missingValue) {
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
    return new MinMax(min,max);
  }


  /** Set all the elements of this array to the given double value.
   * The value is converted to the element type of the array, if needed.
   */
  public static void setDouble( Array result, double val) {
    IndexIterator iter = result.getIndexIterator();
    while (iter.hasNext()) {
      iter.setDoubleNext(val);
    }
  }

  /** sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   */
  public static double sumDouble( Array a) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIterator();
    while (iterA.hasNext())
      sum += iterA.getDoubleNext();
    return sum;
  }

  /** sum all of the elements of array a as doubles.
   * The values from the array a are converted to double (if needed).
   */
  public static double sumDoubleSkipMissingData( Array a, double missingValue) {
    double sum = 0;
    IndexIterator iterA = a.getIndexIterator();
    while (iterA.hasNext())  {
      double val = iterA.getDoubleNext();
      if ((val == missingValue) || Double.isNaN( val))
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
    public MinMax( double min, double max) {
      this.min = min;
      this.max = max;
    }
  }

}

/* Change History:
   $Log: MAMath.java,v $
   Revision 1.5  2006/02/16 23:02:31  caron
   *** empty log message ***

   Revision 1.4  2005/03/05 03:48:19  caron
   bug using Double.MIN_VALUE instead of -Double.MAX_VALUE when finding limits

   Revision 1.3  2004/08/16 20:53:44  caron
   2.2 alpha (2)

   Revision 1.2  2004/07/12 23:40:15  caron
   2.2 alpha 1.0 checkin

 */
