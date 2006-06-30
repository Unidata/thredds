package ucar.nc2.dods;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;

import java.io.*;

/** Test nc2 dods in the JUnit framework.
Dataset {
    Sequence {
        Int32 age;
        Sequence {
            Int16 foo;
        } stuff;
    } person1;
} NestedSequences;
---------------------------------------------
person1.age, person1.foo
1, person1.stuff.foo
0
16
32
48
64

2, person1.stuff.foo
80
96
112
128
144

3, person1.stuff.foo
160
176
192
208
224

5, person1.stuff.foo
240
256
272
288
304

8, person1.stuff.foo
320
336
352
368
384
 */

public class TestDODSSequence extends TestCase {
  private boolean debug = false;

  public TestDODSSequence( String name) {
    super(name);
  }

  public void testReadSequence() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("test.07");

    Variable v = dodsfile.findVariable("person");
    assert null != v;
    assert v instanceof Structure;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;

    Array a = v.read();
    assert a.getRank() == 1;
    assert a.getSize() == 5 : a.getSize();
    //NCdump.printArray(a, "person",System.out,null);

    int count = 0;
    int fib = 1, prev = 1;
    IndexIterator iter = a.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      assert data.findMember("name") != null;

      String name = data.getScalarString("name");
      assert name != null;
      assert name.equals("This is a data test string (pass "+count+").") : name;

      assert data.findMember("age") != null;
      int agev = data.getScalarInt("age");
      assert agev == fib : fib +"!="+agev;

      count++;
      int hold  = fib;
      fib += prev;
      prev = hold;
    }

  }

  boolean show = false;
  /* public void testReadNestedSequence() throws IOException {

    DODSNetcdfFile dodsfile = TestDODSRead.open("test.23");

    Variable seq = dodsfile.findVariable("exp/ComplexSequence");
    assert null != seq;

    Array datas = seq.read();
  } */

}
