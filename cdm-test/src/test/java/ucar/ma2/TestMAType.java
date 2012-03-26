/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.ma2;

import junit.framework.*;

/** Test ma2  methods in the JUnit framework. */

public class TestMAType extends TestCase {
  public TestMAType( String name) {
    super(name);
  }

  /* public void testCast() {
    double d = 12.0;
    char c = (char) d;
    System.out.println(d+" == "+c);

    Double dob = new Double( 99.9);
    castBoolean((Object) dob);
  } */


  /* public void castObject(Array A) {
    Index ima = A.getIndex();

    boolean notNumeric = (A.getElementType() == char.class) ||
                         (A.getElementType() == boolean.class);

    //System.out.println(A.getElementType()+" numeric = "+notNumeric);

    Double dob = new Double( 99.9);
    try {
      A.setObject(ima, dob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Float fob = new Float( 99.9F);
    try {
      A.setObject(ima, fob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Long lob = new Long( 9999);
    try {
      A.setObject(ima, lob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Integer iob = new Integer( 999);
    try {
      A.setObject(ima, iob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Short sob = new Short( "99");
    try {
      A.setObject(ima, sob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Byte byob = new Byte( "9");
    try {
      A.setObject(ima, byob);
      assert(!notNumeric);
    } catch (RuntimeException re) {
      assert(notNumeric);
    }

    Boolean boob = new Boolean( false);
    try {
      A.setObject(ima, boob);
      assert(A.getElementType() == boolean.class);
    } catch (RuntimeException re) {
      assert(A.getElementType() != boolean.class);
    }

    Character cob = new Character( 'c');
    try {
      A.setObject(ima, cob);
      assert(A.getElementType() == char.class);
    } catch (RuntimeException re) {
      assert(A.getElementType() != char.class);
    }
  } */


  public void convert(Array A, boolean isBoolean) {
    Index ima = A.getIndex();
    System.out.println("test conversion on type "+ A.getElementType());

    try {
      double d = A.getDouble(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      float f = A.getFloat(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      long l = A.getLong(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      int i = A.getInt(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      short s = A.getShort(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      byte b = A.getByte(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      char c = A.getChar(ima);
      assert(!isBoolean);
    } catch (RuntimeException re) {
      assert(isBoolean);
    }

    try {
      boolean bb = A.getBoolean(ima);
      assert(isBoolean);
    } catch (RuntimeException re) {
      assert(!isBoolean);
    }


  }

   public void testConvert() {
    int [] shape = new int[1];
    shape[0] = 10;

    Array A = new ArrayDouble(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayFloat(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayLong(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayInt(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayShort(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayByte(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayChar(shape);
    convert(A, false);
    //castObject(A);

    A = new ArrayBoolean(shape);
    convert(A, true);
    //castObject(A);
  }



}
