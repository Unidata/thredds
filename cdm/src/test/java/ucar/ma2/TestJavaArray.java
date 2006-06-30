package ucar.ma2;

import junit.framework.*;

/** Test ma2 contructors from java arrays . */

public class TestJavaArray extends TestCase {

  public TestJavaArray( String name) {
    super(name);
  }

  public void test1Dim() {
    Array aa;
    try {
      aa = Array.factory( new double [] {1.0, 2.0, 3.0} );
    } catch (Exception e) {
      System.out.println("TestJavaArray 1D: "+e);
      assert(false);
      return;
    }

    assert( aa.getRank() == 1);
    int shape[] = aa.getShape();
    assert( shape[0] == 3);

    Index ima = aa.getIndex();
    System.out.println( aa.getFloat(ima.set(1)));
    assert( aa.getFloat(ima.set(0)) == 1.0);
    assert( aa.getFloat(ima.set(1)) == 2.0);
    assert( aa.getFloat(ima.set(2)) == 3.0);

    IndexIterator ai = aa.getIndexIterator();
    System.out.println( ai.getFloatNext());
    assert( ai.getFloatCurrent() == 1.0);
    assert( ai.getFloatNext() == 2.0);
    assert( ai.getFloatNext() == 3.0);

    double [] newArray = (double []) aa.copyTo1DJavaArray();
    assert( newArray[0] == 1.0);
    assert( newArray[1] == 2.0);
    assert( newArray[2] == 3.0);

    newArray = (double []) aa.copyToNDJavaArray();
    assert( newArray[0] == 1.0);
    assert( newArray[1] == 2.0);
    assert( newArray[2] == 3.0);
  }

  public void testNDim() {
    double[][][] tData = {
        {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}},
        {{2.5, 5, 7.5, 10}, {5, 10, 15, 20}, {7.5, 15, 22.5, 30}}
    };

    Array aa;
    try {
      aa = Array.factory( tData);
    } catch (Exception e) {
      System.out.println("TestJavaArray: "+e);
      assert(false);
      return;
    }

    assert( aa.getRank() == 3);
    int shape[] = aa.getShape();
    assert( shape[0] == 2);
    assert( shape[1] == 3);
    assert( shape[2] == 4);

    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++)
      for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++)
          assert( aa.getDouble(ima.set(i,j,k)) == tData[i][j][k]);

    double[][][] newArray = (double [][][]) aa.copyToNDJavaArray();
    for (int i=0; i<shape[0]; i++)
      for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          // System.out.println(newArray[i][j][k]+" "+tData[i][j][k]);
          assert( newArray[i][j][k] == tData[i][j][k]);
        }
  }

  public void testTypes() {
    typeSpecifics( new double[][][] {
        {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}},
        {{2.5, 5, 7.5, 10}, {5, 10, 15, 20}, {7.5, 15, 22.5, 30}}
    });
    typeSpecifics( new float[][] {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}} );
    typeSpecifics( new long[][]  {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}} );
    typeSpecifics( new int[][]   {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}} );
    typeSpecifics( new short[][] {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}} );
    typeSpecifics( new byte[][] {{  1, 2,   3,  4}, {2,  4,  6,  8}, {  3,  6,  9,   12}} );
    typeSpecifics( new char[][] {{  '1', '2',   '3',  '4'}, {'2',  '4',  '6',  '8'}, {  '3',  '6',  '9',   '2'}} );
    typeSpecifics( new boolean[][] {{  true, false, true}, {  false,true, false}} );

  }

  private void typeSpecifics(Object javaArray) {
    System.out.println("typeSpecifics: "+javaArray);
    Array aa;
    try {
      aa = Array.factory( javaArray);
    } catch (Exception e) {
      System.out.println("TestJavaArray: "+e);
      assert(false);
      return;
    }

    Object newArray = aa.copyToNDJavaArray();
    testEquals( javaArray, newArray);
  }

  private void testEquals(Object jArray, Object newArray) {
    Class cType = jArray.getClass().getComponentType();
    int n = java.lang.reflect.Array.getLength(jArray);

    if (cType.isPrimitive()) {
      for (int i=0; i< n; i++)
        assert( java.lang.reflect.Array.get(jArray, i).equals( java.lang.reflect.Array.get(newArray, i)));
    } else {
      for (int i=0; i< n; i++)  // recurse
        testEquals(java.lang.reflect.Array.get(jArray, i), java.lang.reflect.Array.get(newArray, i));
    }
  }

}
