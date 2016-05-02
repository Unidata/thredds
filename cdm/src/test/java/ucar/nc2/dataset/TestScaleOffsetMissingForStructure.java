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
package ucar.nc2.dataset;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

public class TestScaleOffsetMissingForStructure extends TestCase {

  public void testNetcdfFile() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openFile(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc", null);
    Variable v = ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.SHORT;

    Array data = v.read();
    Index ima = data.getIndex();
    short val = data.getShort(ima);
    assert val == -999;

    assert v.getUnitsString().equals("m");
    v.addAttribute(new Attribute("units", "meters"));
    assert v.getUnitsString().equals("meters");

    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    Structure s = (Structure) ncfile.findVariable("record");
    assert (s != null);

    Variable v2 = s.findVariable("testScale");
    Attribute att = v2.findAttribute("units");
    assert att.getStringValue().equals("meters");
    assert v2.getUnitsString().equals("meters");

    StructureData sdata = s.readStructure(0);
    StructureMembers.Member m = sdata.findMember("testScale");
    assert null != m;
    assert m.getUnitsString().equals("meters");

    double dval = sdata.convertScalarDouble(m.getName());
    assert dval == -999.0;

    int count = 0;
    StructureDataIterator siter = s.getStructureIterator();
    try {
      while (siter.hasNext()) {
        sdata = siter.next();
        m = sdata.findMember("testScale");
        assert m.getUnitsString().equals("meters");

        assert null != m;
        dval = sdata.convertScalarDouble(m.getName());
        double expect = (count == 0) ? -999.0 : 13.0;
        assert dval == expect : dval + "!=" + expect;
        count++;
      }
    } finally {
      siter.finish();
    }
    ncfile.close();
  }

  public void testNetcdfDataset() throws IOException, InvalidRangeException {
    NetcdfDataset ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc");
    System.out.printf("Open %s%n", ncfile.getLocation());
    VariableDS v = (VariableDS) ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.FLOAT;

    Array data = v.read();
    Index ima = data.getIndex();
    float val = data.getFloat(ima);
    assert v.isMissing(val);
    assert Float.isNaN(val) : val;

    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    Structure s = (Structure) ncfile.findVariable("record");
    assert (s != null);

    VariableDS vm = (VariableDS) s.findVariable("testScale");
    Array vmData = vm.read();
    if (vmData.hasNext()) {
      float vmval = vmData.nextFloat();
      assert vm.isMissing(vmval);
      assert Float.isNaN(vmval) : vmval;
    }

    StructureData sdata = s.readStructure(0);
    StructureMembers.Member m = sdata.findMember("testScale");
    assert null != m;

    /* LOOK heres the problem where StructureData.getScalarXXX doesnt use enhanced values
    float dval = sdata.getScalarFloat( m.getName());
    assert Float.isNaN(dval) : dval;

    int count = 0;
    StructureDataIterator siter = s.getStructureIterator();
    while (siter.hasNext()) {
      sdata = siter.next();
      m = sdata.findMember("testScale");
      assert null != m;

      dval = sdata.getScalarFloat( m);
      if (count == 0)
        assert Float.isNaN(dval) : dval;
      else
        assert TestAll.closeEnough(dval, 1040.8407) : dval;
      count++;
    } */

    ncfile.close();
  }

  public void testNetcdfDatasetAttributes() throws IOException, InvalidRangeException {
    NetcdfDataset ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc");
    VariableDS v = (VariableDS) ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.FLOAT;

    assert v.getUnitsString().equals("m");
    v.addAttribute(new Attribute("units", "meters"));
    assert v.getUnitsString().equals("meters");

    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    Structure s = (Structure) ncfile.findVariable("record");
    assert (s != null);

    Variable v2 = s.findVariable("testScale");
    assert v2.getUnitsString().equals("meters");
    assert v2.getDataType() == DataType.FLOAT;

    StructureData sdata = s.readStructure(0);
    StructureMembers.Member m = sdata.findMember("testScale");
    assert null != m;
    assert m.getUnitsString().equals("meters") : m.getUnitsString();
    assert m.getDataType() == DataType.FLOAT;

    StructureDataIterator siter = s.getStructureIterator();
    try {
      while (siter.hasNext()) {
        sdata = siter.next();
        m = sdata.findMember("testScale");
        assert null != m;
        assert m.getUnitsString().equals("meters");
      }
    } finally {
      siter.finish();
    }
    ncfile.close();
  }
}
