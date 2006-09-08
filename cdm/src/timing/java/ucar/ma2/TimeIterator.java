// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import java.io.IOException;

/*
Test size= 50 Mbytes

1.4.2_09 -client

createArray took = 156 msecs
writeArray took = 1297 msecs
writeArray(setFloat(elem)) took = 531 msecs

readArray took = 1047 msecs
readArray(FastIterator) took = 750 msecs
readArray(getFloat(elem)) took = 469 msecs
readArray(ArrayFloat) took = 437 msecs

create took = 125 msecs
write took = 172 msecs
read took = 438 msecs

1.4.2_09 -server

createArray took = 141 msecs
writeArray took = 625 msecs
writeArray(setFloat(elem)) took = 281 msecs

readArray took = 469 msecs
readArray(FastIterator) took = 500 msecs
readArray(getFloat(elem)) took = 422 msecs
readArray(ArrayFloat) took = 94 msecs

create took = 140 msecs
write took = 125 msecs
read took = 94 msecs

1.5.0 -client

createArray took = 156 msecs
writeArray took = 1344 msecs
writeArray(setFloat(elem)) took = 594 msecs

readArray took = 906 msecs
readArray(FastIterator) took = 750 msecs
readArray(getFloat(elem)) took = 469 msecs
readArray(ArrayFloat) took = 437 msecs

create took = 125 msecs
write took = 188 msecs
read took = 437 msecs

1.5 -server

createArray took = 359 msecs
writeArray took = 422 msecs
writeArray(setFloat(elem)) took = 312 msecs

readArray took = 469 msecs
readArray(FastIterator) took = 516 msecs
readArray(getFloat(elem)) took = 437 msecs
readArray(ArrayFloat) took = 78 msecs

create took = 235 msecs
write took = 109 msecs
read took = 94 msecs

1.6.0 -client

createArray took = 156 msecs
writeArray took = 766 msecs
writeArray(setFloat(elem)) took = 391 msecs

readArray took = 703 msecs
readArray(FastIterator) took = 609 msecs
readArray(getFloat(elem)) took = 453 msecs
readArray(ArrayFloat) took = 157 msecs

create took = 156 msecs
write took = 109 msecs
read took = 172 msecs

1.6 server

createArray took = 359 msecs
writeArray took = 328 msecs
writeArray(setFloat(elem)) took = 109 msecs

readArray took = 313 msecs
readArray(FastIterator) took = 234 msecs
readArray(getFloat(elem)) took = 94 msecs
readArray(ArrayFloat) took = 78 msecs

create took = 219 msecs
write took = 109 msecs
read took = 94 msecs

*/

public class TimeIterator {

  public void write(float[] a) {
    for (int i = 0; i < a.length; i++) {
      a[i] = (float) i;
    }
  }

  public float read(float[] a) {
    float sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += a[i];
    }
    return sum;
  }

  public void writeArray(Array A) {
    int count = 0;
    IndexIterator iter = A.getIndexIterator();
    while (iter.hasNext())
      iter.setFloatNext( count++);
  }

  public void writeArray2(Array A) {
    int size = (int) A.getSize();
    for (int i = 0; i < size; i++) {
      A.setFloat(i, (float) i);
    }
  }

  public float readArray(Array A) {
    float sum = 0;
    IndexIterator iter = A.getIndexIterator();
    while (iter.hasNext()) {
      sum += iter.getFloatNext();
    }
    return sum;
  }

  public float readArray2(Array A) {
    float sum = 0;
    IteratorFast iter = (IteratorFast) A.getIndexIterator();
    while (iter.hasNext()) {
      sum += iter.getFloatNext();
    }
    return sum;
  }

  public float readArray3(Array A) {
    int size = (int) A.getSize();
    float sum = 0;
    for (int i = 0; i < size; i++) {
      sum += A.getFloat(i);
    }
    return sum;
  }

  public float readArray4(Array A) {
    ArrayFloat Afloat = (ArrayFloat) A;
    int size = (int) A.getSize();
    float sum = 0;
    for (int i = 0; i < size; i++) {
      sum += Afloat.getFloat(i);
    }
    return sum;
  }

  public void test(int z) {
    int size = 1000 * 1000;
    System.out.println("\nTest size= "+z+" Mbytes");

    long start = System.currentTimeMillis();
    Array A = Array.factory( DataType.FLOAT, new int[]  {z*size} );
    long took = System.currentTimeMillis() - start;
    System.out.println("createArray took = " + took+" msecs");

    start = System.currentTimeMillis();
    writeArray(A);
    took = System.currentTimeMillis() - start;
    System.out.println("writeArray took = " + took+" msecs");

    start = System.currentTimeMillis();
    writeArray2(A);
    took = System.currentTimeMillis() - start;
    System.out.println("writeArray(setFloat(elem)) took = " + took+" msecs");

    start = System.currentTimeMillis();
    readArray(A);
    took = System.currentTimeMillis() - start;
    System.out.println("readArray took = " + took+" msecs");

    start = System.currentTimeMillis();
    readArray2(A);
    took = System.currentTimeMillis() - start;
    System.out.println("readArray(FastIterator) took = " + took+" msecs");

    start = System.currentTimeMillis();
    readArray3(A);
    took = System.currentTimeMillis() - start;
    System.out.println("readArray(getFloat(elem)) took = " + took+" msecs");

    start = System.currentTimeMillis();
    readArray4(A);
    took = System.currentTimeMillis() - start;
    System.out.println("readArray(ArrayFloat) took = " + took+" msecs");

    start = System.currentTimeMillis();
    float[] data = new float[z*size];
    took = System.currentTimeMillis() - start;
    System.out.println("create took = " + took+" msecs");

    start = System.currentTimeMillis();
    write(data);
    took = System.currentTimeMillis() - start;
    System.out.println("write took = " + took+" msecs");

    start = System.currentTimeMillis();
    read(data);
    took = System.currentTimeMillis() - start;
    System.out.println("read took = " + took+" msecs");  // */
  }


  public static void main(String args[]) throws IOException {
    TimeIterator ti = new TimeIterator();
    //ti.test(1);
    //ti.test(5);
    ti.test(50);
    ti.test(50);
    ti.test(50);

    System.in.read();

  }

}
