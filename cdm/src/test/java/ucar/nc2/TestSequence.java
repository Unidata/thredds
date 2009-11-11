/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2;

import junit.framework.TestCase;

import java.util.List;
import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.ArraySequence;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;

/**
 * Describe
 *
 * @author caron
 * @since Nov 10, 2009
 */
public class TestSequence extends TestCase {

  public TestSequence(String name) {
    super(name);
  }

  NetcdfFile ncfile;

  protected void setUp() throws Exception {
    ncfile = NetcdfFile.open(TestAll.testdataDir + "lightning/nldn/200929100.ingest");
  }

  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testRead() throws IOException {

    List vars = ncfile.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable v = (Variable) vars.get(i);
      System.out.println(" " + v.getShortName() + " == " + v.getName());
    }

    Variable v = ncfile.findVariable("record");
    assert v != null;
    assert v instanceof Sequence;
    Sequence record = (Sequence) v;

    vars = record.getVariables();
    for (int i = 0; i < vars.size(); i++) {
      Variable vv = (Variable) vars.get(i);
      System.out.println(" " + vv.getShortName() + " == " + vv.getName());
    }

    Array data = v.read();
    assert data != null;
    assert data instanceof ArraySequence;

    ArraySequence as = (ArraySequence) data;
    as.getStructureDataCount();
    System.out.printf(" count = %d%n", as.getStructureDataCount());

    int count = 0;
    StructureDataIterator iter = as.getStructureDataIterator();
    while (iter.hasNext()) {
      StructureData sdata = iter.next();
      count++;
    }
    System.out.printf(" count = %d%n", count);

    int count2 = 0;
    StructureDataIterator iter2 = record.getStructureIterator();
    while (iter2.hasNext()) {
      StructureData sdata = iter2.next();
      count2++;
    }
    System.out.printf(" count2 = %d%n", count2);

    assert count2 == count;


  }

}
