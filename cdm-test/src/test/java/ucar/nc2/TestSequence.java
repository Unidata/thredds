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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Nov 10, 2009
 */
@Category(NeedsCdmUnitTest.class)
public class TestSequence {

  @Test
  public void testRead() throws IOException {

    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "ft/point/200929100.ingest")) {
      for (Variable v : ncfile.getVariables()) {
        System.out.println(" " + v.getShortName() + " == " + v.getFullName());
      }

      Variable v = ncfile.findVariable("record");
      assert v != null;
      assert v instanceof Sequence;
      Sequence record = (Sequence) v;

      for (Variable vv : record.getVariables()) {
        System.out.println(" " + vv.getShortName() + " == " + vv.getFullName());
      }

      Array data = v.read();
      assert data != null;
      assert data instanceof ArraySequence;

      ArraySequence as = (ArraySequence) data;
      as.getStructureDataCount();
      System.out.printf(" count = %d%n", as.getStructureDataCount());

      int count = 0;
      try (StructureDataIterator iter = as.getStructureDataIterator()) {
        while (iter.hasNext()) {
          StructureData sdata = iter.next();
          count++;
        }
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

  @Test
  public void testReadNestedSequence() throws IOException {

    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/bufr/userExamples/5900.20030601.rass")) {

      Variable v = ncfile.findVariable("obs");
      assert v != null;
      assert v instanceof Sequence;
      Sequence record = (Sequence) v;

      System.out.printf("Sequence.getVariables %s%n", ncfile.getLocation());
      for (Variable vv : record.getVariables()) {
        System.out.printf(" %s == %s%n", vv.getShortName(), vv.getFullName());
      }
      System.out.printf("%n");

      int nr = 0;
      StructureDataIterator iter2 = record.getStructureIterator();
      while (iter2.hasNext()) { // loop over all StructureData
        StructureData sdata = iter2.next();
        ArraySequence nested = sdata.getArraySequence("seq1");
        System.out.printf("count nested=%d%n", nested.getStructureDataCount());
        nr++;
      }
      System.out.printf("count %s = %d%n", record.getFullName(), nr);

      Array data = v.read();
      assert data instanceof ArraySequence;
      ArraySequence as = (ArraySequence) data;
      System.out.printf(" ArraySequence.getStructureDataCount = %d%n", as.getStructureDataCount());
      System.out.printf("%n");

      showArraySequence(as);

      // PrintWriter pw = new PrintWriter(System.out);
      int count = 0;
      try (StructureDataIterator iter = as.getStructureDataIterator()) {
        while (iter.hasNext()) {
          StructureData sdata = iter.next();
          ArraySequence nested = sdata.getArraySequence("seq1");
          if (count == 0) showArraySequence(nested);
          count++;

          try (StructureDataIterator nestedIter = nested.getStructureDataIterator()) {
            while (nestedIter.hasNext()) {
              StructureData nestedData = nestedIter.next();
              assert nestedData != null;
              int fld1Value = nestedData.getScalarShort("Virtual_temperature");
            }
          }
        }
      }
      System.out.printf(" actual count = %d%n", count);
      System.out.printf(" ArraySequence.getStructureDataCount = %d%n", as.getStructureDataCount());
    }
  }

  private void showArraySequence(ArraySequence as) {
    System.out.printf("ArraySequence.getMembers%n");
    for (StructureMembers.Member m : as.getMembers()) {
      System.out.printf(" %s (%s)%n", m, m.getDataType());
    }
    System.out.printf("%n");

  }

}
