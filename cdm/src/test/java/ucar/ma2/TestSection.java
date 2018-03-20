/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NCdumpW;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test ma2 section methods in the JUnit framework. */

public class TestSection extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestSection( String name) {
    super(name);
  }

  int m = 4, n = 5, p = 6;
  int [] sA = { m, n, p };
  int stride1 = p;
  int stride2 = n * p;

  ArrayDouble A = new ArrayDouble(sA);
  int i,j,k;

  ArrayDouble secA;
  int m1 = 2;
  int m2 = 3;
  int mlen = (m2 - m1 + 1);


  public void setUp() throws InvalidRangeException {
    Index ima = A.getIndex();

    // write
    for (i=0; i<m; i++) {
      ima.set0(i);
      for (j=0; j<n; j++) {
        ima.set1(j);
        for (k=0; k<p; k++) {
          ima.set2(k);
          A.setDouble(ima, (double) (i*100+j*10+k));
        }
      }
    }

    // section
    secA = (ArrayDouble) A.section( new Section(m1+":"+m2+",:,:").getRanges() );
  }

  public void testSection() {
    Index ima = secA.getIndex();
    int [] shape = ima.getShape();
    //System.out.println(ima);
    assert( ima.getRank() == 3);
    assert( ima.getSize() == (mlen * n * p));
    assert( shape[0] == mlen);
    assert( shape[1] == n);
    assert( shape[2] == p);
  }

  public void testSectionRead() {
    Index ima = secA.getIndex();

    for (i=0; i<mlen; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = secA.getDouble(ima.set(i,j,k));
          assert (val == (i+m1)*100+j*10+k);
        }
      }
    }
    System.out.println("testSection ");
  }

  public void testIter() {
    IndexIterator iter = secA.getIndexIterator();
    for (i=0; i<mlen; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = iter.getDoubleNext();
          double myVal = (double) (i+m1)*100+j*10+k;
          assert (val == (i+m1)*100+j*10+k);
        }
      }
    }
    System.out.println("testSectionIter ");
  }

  public void testRangeIterator() {
    IndexIterator iter;
    try {
      iter = A.getRangeIterator( new Section(m1+":"+m2+",:,:").getRanges() );
    } catch (InvalidRangeException e) {
      System.out.println("testMAsection failed == "+ e);
      return;
    }

    for (i=0; i<mlen; i++) {
      for (j=0; j<n; j++) {
        for (k=0; k<p; k++) {
          double val = iter.getDoubleNext();
          double myVal = (double) (i+m1)*100+j*10+k;
          assert (val == (i+m1)*100+j*10+k);
        }
      }
    }
    System.out.println("testRangeIterator ");
  }


  public void testSectionReduce() {
    ArrayDouble secA2;
    try {
      secA2 = (ArrayDouble) A.section( new Section("2,:,:").getRanges() );
    } catch (InvalidRangeException e) {
      System.out.println("testSectionReduce failed == "+ e);
      return;
    }

    Index ima = secA2.getIndex();
    int [] shape = ima.getShape();

    assert( ima.getRank() == 2);
    assert( ima.getSize() == (n * p));
    assert( shape[0] == n);
    assert( shape[1] == p);

    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          ima.set(i,j);
          double val = secA2.getDouble(ima);
          //System.out.println(val+ " "+val);
          assert (val == 200+i*10+j);
      }
    }

    IndexIterator iter = secA2.getIndexIterator();
    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+val);
          assert (val == 200+i*10+j);
      }
    }
    System.out.println("testSectionReduce ");
  }

  public void testSectionNoReduce() throws InvalidRangeException {
    ArrayDouble secA2 = (ArrayDouble) A.sectionNoReduce( new Section("2,:,:").getRanges() );

    Index ima = secA2.getIndex();
    int [] shape = ima.getShape();

    assert( ima.getRank() == 3);
    assert( ima.getSize() == (n * p));
    assert( shape[0] == 1);
    assert( shape[1] == n);
    assert( shape[2] == p);

    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          ima.set(0,i,j);
          double val = secA2.getDouble(ima);
          //System.out.println(val+ " "+val);
          assert (val == 200+i*10+j);
      }
    }

    IndexIterator iter = secA2.getIndexIterator();
    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+val);
          assert (val == 200+i*10+j);
      }
    }
    System.out.println("testSectionNoReduce ");
  }

  public void testDoubleRange() throws InvalidRangeException {
    ArrayDouble secA2 = (ArrayDouble) A.section( new Section("2,:,:").getRanges()  );

    ArrayDouble secA1 = (ArrayDouble) secA2.section( new Section(":,1").getRanges()  );

    Index ima = secA1.getIndex();

    int [] shape = ima.getShape();
    assert( ima.getRank() == 1);
    assert( ima.getSize() == n);
    assert( shape[0] == n);

    for (j=0; j<n; j++) {
          ima.set(j);
          double val = secA1.getDouble(ima);
          //System.out.println("testDoubleRange read1 = "+ val);
          assert (val == 200+j*10+1);
    }

    IndexIterator iter = secA1.getIndexIterator();
    for (i=0; i<n; i++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+myVal);
          //System.out.println("testDoubleRange read2 = "+ val);
          assert (val == 200+i*10+1);
    }
    System.out.println("testDoubleRange");
  }

  public void testDoubleRange2() throws InvalidRangeException {
    ArrayDouble secA2 = (ArrayDouble) A.section( new Section("2,:,:").getRanges()  );
    secA2 = (ArrayDouble) secA2.reduce();

    ArrayDouble secA1 = (ArrayDouble) secA2.section( new Section(":,1").getRanges()  );

    Index ima = secA1.getIndex();

    int [] shape = ima.getShape();
    assert( ima.getRank() == 1);
    assert( ima.getSize() == n);
    assert( shape[0] == n);

    for (j=0; j<n; j++) {
          ima.set(j);
          double val = secA1.getDouble(ima);
          //System.out.println("testDoubleRange read1 = "+ val);
          assert (val == 200+j*10+1);
    }

    IndexIterator iter = secA1.getIndexIterator();
    for (i=0; i<n; i++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+myVal);
          //System.out.println("testDoubleRange read2 = "+ val);
          assert (val == 200+i*10+1);
    }
    System.out.println("testDoubleRange 2");
  }

  public void testRange3() throws InvalidRangeException {
    ArrayDouble secA2 = (ArrayDouble) A.section( new Section(1+":"+(m-2)+",0:"+(n-2)+",1:"+(p-1)).getRanges() );

    Index imaOrg = A.getIndex();
    Index ima = secA2.getIndex();

    int [] shape = ima.getShape();
    assert( ima.getRank() == 3);
    assert( ima.getSize() == (m-2)*(n-1)*(p-1));
    assert( shape[0] == m-2);
    assert( shape[1] == n-1);
    assert( shape[2] == p-1);

    for (i=0; i<m-2; i++) {
      for (j=0; j<n-1; j++) {
        for (k=0; k<p-1; k++) {
          ima.set(i,j);
          double val = secA2.getDouble(ima.set(i,j,k));
          double valOrg = A.getDouble(imaOrg.set(i+1,j,k+1));
          //System.out.println("testSectionRange read1 = "+ val);
          assert (val == valOrg);
        }
      }
    }

    System.out.println("testRange 3");
  }

  public void testSlice() {
    ArrayDouble  secA2 = (ArrayDouble) A.slice( 0, 2 );
    Index ima = secA2.getIndex();
    ima = secA2.getIndex();

    int [] shape = ima.getShape();
    assert( ima.getRank() == 2);
    assert( ima.getSize() == (n * p));
    assert( shape[0] == n);
    assert( shape[1] == p);

    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          ima.set(i,j);
          double val = secA2.getDouble(ima);
          //System.out.println("testSectionRange read1 = "+ val);
          assert (val == 200+i*10+j);
      }
    }

    IndexIterator iter = secA2.getIndexIterator();
    for (i=0; i<n; i++) {
      for (j=0; j<p; j++) {
          double val = iter.getDoubleNext();
          //double myVal = (double) (i+m1)*100+j*10+k;
          //System.out.println(val+ " "+myVal);
          //System.out.println("testSectionRange read2 = "+ val);
          assert (val == 200+i*10+j);
      }
    }
    System.out.println("testSlice ");
  }

  public void testSliceOne() throws InvalidRangeException {
    ArrayDouble secA1 = (ArrayDouble) A.sectionNoReduce( new Section("1,0:"+(n-2)+",1:"+(p-1)).getRanges() );

    int [] shape = secA1.getShape();
    assert( secA1.getRank() == 3);
    assert( secA1.getSize() == (n-1)*(p-1));
    assert( shape[0] == 1);
    assert( shape[1] == n-1);
    assert( shape[2] == p-1);

    ArrayDouble  secA2 = (ArrayDouble) secA1.slice( 1, 2);
    shape = secA2.getShape();
    assert( secA2.getRank() == 2);
    assert( secA2.getSize() == (p-1));
    assert( shape[0] == 1);
    assert( shape[1] == p-1);

    Index imaOrg = A.getIndex();
    Index ima = secA2.getIndex();
    for (k=0; k<p-1; k++) {
      double val = secA2.getDouble(ima.set(0, k));
      double valOrg = A.getDouble(imaOrg.set(1,2,k+1));
      //System.out.println("testSectionRange read1 = "+ val);
      assert (val == valOrg);
    }

    System.out.println("testSlice with length 1 dim");
  }

  public void testSectionCopy() throws InvalidRangeException {
    ArrayDouble secA2 = (ArrayDouble) A.section( new Section(1+":"+(m-2)+",0:"+(n-2)+",1:"+(p-1)).getRanges() ).copy();

    Index imaOrg = A.getIndex();
    Index ima = secA2.getIndex();

    int [] shape = ima.getShape();
    assert( ima.getRank() == 3);
    assert( ima.getSize() == (m-2)*(n-1)*(p-1));
    assert( shape[0] == m-2);
    assert( shape[1] == n-1);
    assert( shape[2] == p-1);
    assert( secA2 instanceof ArrayDouble.D3);

    for (i=0; i<m-2; i++) {
      for (j=0; j<n-1; j++) {
        for (k=0; k<p-1; k++) {
          ima.set(i,j);
          double val = secA2.getDouble(ima.set(i,j,k));
          secA2.setDouble(ima, 0.0);
          double valOrg = A.getDouble(imaOrg.set(i+1,j,k+1));
          //System.out.println("testSectionRange read1 = "+ val);
          assert (val == valOrg);
        }
      }
    }
    System.out.println("test sectionCopy");
  }

  public void testIntersect() throws InvalidRangeException {
    Section have = new Section("101:101,0:1919");
    Section want = new Section("100:229:3,100:191:7");

    boolean b = have.intersects( want);
    assert !b;
  }

  public void testIntersectWithStrides() throws InvalidRangeException {
    Section have = new Section("121:121:1,0:1919:1");
    Section want = new Section("100:229:3,100:191:7");
    Section intersectCorrect = new Section("121:121:3,100:191:7");
    Section intersect = have.intersect( want);
    assert intersect.equals(intersectCorrect);

    int offset = want.offset( intersect);
    assert offset == 98 : offset;
  }

  /* public void testSubsection() throws InvalidRangeException {
    Section s = new Section("0:180,0:287");
    int total = (int) s.computeSize();
    int start = 0;
    int nelems = 0x1000;
    while (start < total) {
      Section ss = s.subsection(start, start+nelems);
      start += nelems;
      System.out.println(" section "+ss);
    }
  } */


  /* christine.e.smit@nasa.gov

  I'm doing a double slice on a three-dimensional variable to get a 1-D slice and then trying to read out the values.
  Unfortunately, indexing seems to give me values from the full original array rather than the slice I'm working with.

  Array twoD = array.slice(0, 1);
  Array oneD = twoD.slice(0,1);
  System.out.println(oneD.getDouble(0));
  System.out.println(oneD);
  System.out.println(array.getDouble(0));

  This is what I see:

  178.0
  -9999 -9999 -9999 81 66 93 103 -9999 -9999 -9999 -9999 -9999
  178.0

  I would expect the first print statement to print -9999 since that is the first element of the slice I'm working on.
  So this kind of looks like a bug to me. How do I get the first element of my slice rather than of the entire parent array?
   */
  public void testSlice3D() throws InvalidRangeException, IOException {
    Array a = Array.makeArray(DataType.DOUBLE, 1000, 0.0, 1.0);
    Array a3 = a.reshape(new int[] {10,10,10});

    logger.debug("{}", NCdumpW.toString(a3, "test a3", null));
    Array a2 = a3.slice(0,1);

    logger.debug("{}", NCdumpW.toString(a2, "a3.slice(0,1)", null));

    Array a1 = a2.slice(0,1);

    logger.debug("{}", NCdumpW.toString(a1, "a2.slice(0,1)", null));

    ArrayDouble.D2 twoD = (ArrayDouble.D2) a2;
    System.out.printf("wrong= %f%n", a2.getDouble(0));
    System.out.printf("right= %f%n", twoD.get(0, 0));

    ArrayDouble.D1 oneD = (ArrayDouble.D1) a1;
    System.out.printf("wrong= %f%n", a1.getDouble(0));
    System.out.printf("right= %f%n", oneD.get(0));
   }
}
