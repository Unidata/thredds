package ucar.unidata.util.test;

import ucar.ma2.Array;

/**
 * ma2 testing utilities
 *
 * @author caron
 * @since 3/23/12
 */
public class UtilsMa2Test {

  static public void testEquals(Array array1, Array array2) {
    assert array1.getElementType() ==  array2.getElementType();
    assert array1.getSize() ==  array2.getSize();

    testShape(array1.getShape(), array2.getShape());

    // see if backing store is identical content
    Object jarray1 = array1.getStorage();
    Object jarray2 = array1.getStorage();
    int n = (int) array1.getSize();
    for (int i=0; i< n; i++)
       assert( java.lang.reflect.Array.get(jarray1, i).equals( java.lang.reflect.Array.get(jarray2, i)));
   }

  static private void testShape(int[] shape1, int shape2[]) {
    assert shape1.length == shape2.length;
    for (int i=0; i<shape1.length; i++)
      assert shape1[i] == shape2[i];
  }

  static public void testJarrayEquals(Object jarray1, Object jarray2, int n) {
    for (int i=0; i< n; i++)
       assert( java.lang.reflect.Array.get(jarray1, i).equals( java.lang.reflect.Array.get(jarray2, i)));
   }
}
