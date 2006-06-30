package ucar.nc2.dods;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;

import java.io.*;

/** Test nc2 dods in the JUnit framework.
 *
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

public class TestDODSnestedSequence extends TestCase {
  private boolean debug = false;

  public TestDODSnestedSequence( String name) {
    super(name);
  }

   public void testNestedSequenceParent() throws IOException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("NestedSeq");

    Variable v = dodsfile.findVariable("person1");
    assert null != v;
    assert v instanceof Structure;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.isVariableLength();

    Array a = v.read();  // DODSNetcdfFile.readFromServer = <person1>
    assert a.getRank() == 1;
    assert a.getSize() == 5 : a.getSize();
    //NCdump.printArray(a, "person",System.out,null);

    int count = 0;
    int fib = 1, prev = 1;
    IndexIterator iter = a.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      StructureMembers.Member stuff = data.findMember("stuff");
      assert stuff != null;

      assert data.findMember("age") != null;
      int agev = data.getScalarInt("age");
      assert agev == fib : fib +"!="+agev;

      count++;
      int hold  = fib;
      fib += prev;
      prev = hold;
    }

  }

  public void testNestedSequence() throws IOException, InvalidRangeException {
    DODSNetcdfFile dodsfile = TestDODSRead.open("NestedSeq");

    Variable v = dodsfile.findVariable("person1");
    Structure s = (Structure) v;

    v = s.findVariable("stuff");
    assert null != v;
    assert v instanceof Structure;
    assert v instanceof DODSStructure;
    assert v.getRank() == 1;
    assert v.isVariableLength();

    Array sa = v.readAllStructures(null, true); // DODSNetcdfFile.readFromServer = <person1.stuff>
    assert sa.getRank() == 1;
    assert sa.getSize() == 25 : sa.getSize();

    int count = 0;
    IndexIterator iter = sa.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      assert data.findMember("foo") != null;

      int foo = data.getScalarInt("foo");
      assert foo == count*16 : foo;
      count++;
    }

    Array a = v.read(); // // DODSNetcdfFile.readFromServer = <person1.stuff>
    assert a.getRank() == 1;
    assert a.getSize() == 25 : a.getSize();

    NCdump.printArray(a, "stuff",System.out,null);

    count = 0;
    iter = a.getIndexIterator();
    while (iter.hasNext()) {
      StructureData data = (StructureData) iter.next();

      assert data.findMember("foo") != null;

      int foo = data.getScalarInt("foo");
      assert foo == count*16 : foo;

      count++;
    }

  }

  public void testCE() throws IOException, InvalidRangeException {
    DODSNetcdfFile dodsFile = TestDODSRead.open("NestedSeq2");
    Variable outerSequence = dodsFile.findVariable("person1");

    String CE = "person1.age,person1.stuff&person1.age=3";
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(outerSequence, CE);
    assert as.getSize() == 1;

    StructureData outerStructure = as.getStructureData(0);
    StructureMembers outerMembers = outerStructure.getStructureMembers();
    assert outerMembers.findMember("age") != null;
    assert outerMembers.findMember("stuff") != null;

    // get at the inner sequence
    ArrayStructure asInner = (ArrayStructure) outerStructure.getArray("stuff");
    StructureMembers innerMembers = asInner.getStructureMembers();
    assert innerMembers.findMember("foo") != null;
    assert innerMembers.findMember("bar") != null;

    assert asInner.getSize() == 3 : asInner.getSize();

    StructureData firstInner = asInner.getStructureData(0);
    StructureMembers firstMembers = firstInner.getStructureMembers();
    assert firstMembers.findMember("foo") != null;
    assert firstMembers.findMember("bar") != null;

    //StructureMembers.Member timeMember = innerMembers.findMember(timeVar.getShortName());

  }


  // server+"NestedSeq2", "person1.age,person1.stuff&person1.age=3"

  /* boolean show = false;
  public void testReadNestedSequence() throws IOException {

    DODSNetcdfFile dodsfile = TestDODSRead.open("test.23");

    DODSStructure struct = dodsfile.findStructure("exp");
    assert null != struct;

    DODSStructure datas = struct.read();

    DODSSequence seq = (DODSSequence) datas.findStructureByShortName("ComplexSequence");
    assert null != seq;

    int count = 0;
    Iterator iter = seq.getSequenceIterator(null);
    while (iter.hasNext()) {
      if (debug) System.out.println(" testReadStructure row = "+ count);
      count++;
      DODSStructure data = (DODSStructure) iter.next();

      DODSGrid profile = (DODSGrid) data.findStructureByShortName("profile");
      assert profile != null;
      DODSVariable v = profile.findVariableByShortName("depth");
      assert v != null;
      assert v.hasCachedData();
      Array a = v.read();
      assert a.getRank() == 1;

      Dimension d = dodsfile.findDimension("exp.ComplexSequence.profile.depth");
      assert d != null;
      assert d.getLength() == a.getSize();

      if (debug) System.out.println(profile.getName()+" == \n"+profile);
      break;
    }
  } */

}
