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
package ucar.nc2;

import junit.framework.*;
import org.junit.Assert;
import ucar.ma2.*;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.*;

/**
 * Test reading record data
 */

public class TestStructure extends TestCase {

  public TestStructure(String name) {
    super(name);
  }

  NetcdfFile ncfile;

  protected void setUp() throws Exception {
    ncfile = TestDir.openFileLocal("testWriteRecord.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }

  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testNames() {
    List<Variable> vars = ncfile.getVariables();
    String[] trueNames = {"rh", "T", "lat", "lon", "time", "recordvarTest",
            "record"};
    for (int i = 0; i < vars.size(); i++) {
      Assert.assertEquals("Checking names", trueNames[i],
              vars.get(i).getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    vars = record.getVariables();
    String[] trueRecordNames = {"record.rh", "record.T", "record.time",
            "record.recordvarTest"};
    for (int i = 0; i < vars.size(); i++) {
      Assert.assertEquals("Checking record names", trueRecordNames[i],
              vars.get(i).getFullName());
    }

    Variable time = ncfile.findVariable("record.time");
    assert time != null;

    Variable time2 = record.findVariable("time");
    assert time2 != null;

    Assert.assertEquals(time, time2);
  }

  public void testReadStructureCountBytesRead() throws IOException, InvalidRangeException {

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    // read all at once
    long totalAll = 0;
    Array dataAll = record.read();
    IndexIterator iter = dataAll.getIndexIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        totalAll += data.getSize() * m.getDataType().getSize();
      }
    }
    Assert.assertEquals("Total bytes read", 304, totalAll);

    // read one at a time
    int numrecs = record.getShape()[0];
    long totalOne = 0;
    for (int i = 0; i < numrecs; i++) {
      StructureData sd = record.readStructure(i);

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        totalOne += data.getSize() * m.getDataType().getSize();
      }
    }
    Assert.assertEquals("testReadStructureCountBytesRead", totalAll, totalOne);

    // read with the iterator
    long totalIter = 0;
    StructureDataIterator iter2 = record.getStructureIterator();
    try {
      while (iter2.hasNext()) {
        StructureData sd = iter2.next();
        Iterator viter = sd.getMembers().iterator();
        while (viter.hasNext()) {
          StructureMembers.Member m = (StructureMembers.Member) viter.next();
          Array data = sd.getArray(m);
          totalIter += data.getSize() * m.getDataType().getSize();
        }
      }
    } finally {
      iter2.finish();
    }
    Assert.assertEquals("Bytes through iteration", totalIter, totalOne);
  }

  public void testN3ReadStructureCheckValues() throws IOException, InvalidRangeException {

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    // read all at once
    int recnum = 0;
    Array dataAll = record.read();
    IndexIterator iter = dataAll.getIndexIterator();
    while (iter.hasNext()) {
      StructureData s = (StructureData) iter.next();
      Array rh = s.getArray("rh");
      assert (rh instanceof ArrayInt.D2);
      checkValues(rh, recnum); // check the values are right
      recnum++;
    }

    // read one at a time
    int numrecs = record.getShape()[0];
    long totalOne = 0;
    for (int i = 0; i < numrecs; i++) {
      StructureData s = record.readStructure(i);
      Array rh = s.getArray("rh");
      assert (rh instanceof ArrayInt.D2);
      checkValues(rh, i); // check the values are right
    }

    // read using iterator
    recnum = 0;
    StructureDataIterator iter2 = record.getStructureIterator();
    try {
      while (iter2.hasNext()) {
        StructureData s = iter2.next();
        Array rh = s.getArray("rh");
        assert (rh instanceof ArrayInt.D2);
        checkValues(rh, recnum); // check the values are right
        recnum++;
      }
    } finally {
      iter2.finish();
    }
  }

  /* public void testN3ReadStructureWithCE() throws IOException, InvalidRangeException {
    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;
    assert record.isUnlimited();

    Iterator iter = record.getVariables().iterator();
    while (iter.hasNext()) {
      Variable v = (Variable) iter.next();
      assert !v.isUnlimited();
    }

    Variable rh = record.findVariable("rh");
    Array data;

 /*   //System.out.println("rh = \n"+rh);
    checkValues( rh.read(), 0); // check the values are right

    data = ncfile.read("record(0).rh", true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.reduce(), 0); // check the values are right     

    data = ncfile.read("record(1).rh", true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.reduce(), 1); // check the values are right

    data = ncfile.read("record.rh", true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.slice(0, 0), 0); // check the values are right
    checkValues( data.slice(0, 1), 1); // check the values are right

    /* data = rh.readAllStructures(null, true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.slice(0, 0), 0); // check the values are right
    checkValues( data.slice(0, 1), 1); // check the values are right

    data = rh.readAllStructuresSpec("0,:,:", true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.reduce(), 0); // check the values are right

    data = rh.readAllStructuresSpec("1,:,:", true);
    assert data instanceof ArrayInt.D3;
    checkValues( data.reduce(), 1); // check the values are right

    System.out.println("*** testN3ReadStructureWithCE ok");
  }     */

  public void readBothWays(String filename) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(filename);
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    //System.out.println(ncfile);
    ncfile.close();

    ncfile = NetcdfFile.open(filename);
    //System.out.println(ncfile);
    ncfile.close();
  }

  public void testReadBothWaysV3mode() throws IOException {
    //readBothWays(TestAll.testdataDir+"grid/netcdf/mm5/n040.nc");
    readBothWays(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc");
    //readBothWays(TestAll.testdataDir+"station/ldm-old/2004061915_metar.nc");

    //System.out.println("*** testReadBothWaysV3mode ok");
  }

  private void checkValues(Array rh, int recnum) {
    assert (rh instanceof ArrayInt.D2) : rh.getClass().getName();

    // check the values are right
    ArrayInt.D2 rha = (ArrayInt.D2) rh;
    int[] shape = rha.getShape();
    for (int j = 0; j < shape[0]; j++) {
      for (int k = 0; k < shape[1]; k++) {
        int want = 20 * recnum + 4 * j + k + 1;
        int val = rha.get(j, k);
        Assert.assertEquals(" " + recnum + " " + j + " " + k + " " + want +
                " " + val, want, val);
      }
    }
  }
}
