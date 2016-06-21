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
package ucar.nc2.dods;

import java.io.IOException;

import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.StructureData;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

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
@Category(NeedsExternalResource.class)
public class TestDODSSequence {

  @org.junit.Test
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

}
