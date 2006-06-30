package ucar.ma2;

import junit.framework.*;

public class TestMAVector extends TestCase {

  public TestMAVector(String name) {
    super(name);
  }

  public void testDot() {
    System.out.println("testDot");

    MAVector v1 = new MAVector(5);
    MAVector v2 = new MAVector(5);

    for (int i=0; i<5; i++) {
      v1.setDouble(i, 1.0);
      v2.setDouble(i, 1.0);
    }

    assert( tolerance( v1.dot(v2) - 5.0));
    assert( tolerance( v1.norm() - Math.sqrt(5.0)) );
    v1.normalize();
    assert( tolerance( v1.norm() - 1.0) );
  }

  public void testCos() {
    System.out.println("testVectorCos");

    MAVector v1 = new MAVector(5);
    MAVector v2 = new MAVector(5);

    for (int i=0; i<5; i++) {
      v1.setDouble(i, 1.0);
      v2.setDouble(i, 1.0);
    }
    //System.out.println(v1.cos(v2)+" "+v1.dot(v2)+" "+v1.norm()+" "+v2.norm());
    assert( tolerance( v1.cos(v2) - 1.0));

    MAVector v3 = new MAVector(2);
    MAVector v4 = new MAVector(2);

    v3.setDouble(0, 1.0);
    v3.setDouble(1, 1.0);

    v4.setDouble(0, 1.0);
    v4.setDouble(1, -1.0);
    assert( tolerance( v3.cos(v4)));

    v4.setDouble(0, -1.0);
    v4.setDouble(1, -1.0);
    assert( tolerance( v3.cos(v4) + 1.0));

    v4.setDouble(0, -1.0);
    v4.setDouble(1, 1.0);
    assert( tolerance( v3.cos(v4)));

  }

  private boolean tolerance( double val) {
    return Math.abs(val) < 1.0e-10;
  }


}