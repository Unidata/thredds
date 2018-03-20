/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import junit.framework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestScaleOffsetMissingForStructure extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testNetcdfFile() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfDataset.openFile(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc", null)) {
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
      try (StructureDataIterator siter = s.getStructureIterator()) {
        while (siter.hasNext()) {
          sdata = siter.next();
          m = sdata.findMember("testScale");
          assert m != null;
          assert m.getUnitsString().equals("meters");
          dval = sdata.convertScalarDouble(m.getName());
          double expect = (count == 0) ? -999.0 : 13.0;
          assert dval == expect : dval + "!=" + expect;
          count++;
        }
      }
    }
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
        assert TestAll.nearlyEquals(dval, 1040.8407) : dval;
      count++;
    } */

    ncfile.close();
  }

  public void testNetcdfDatasetAttributes() throws IOException, InvalidRangeException {
    try (NetcdfDataset ncfile = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testScaleRecord.nc")) {
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

      try (StructureDataIterator siter = s.getStructureIterator()) {
        while (siter.hasNext()) {
          sdata = siter.next();
          m = sdata.findMember("testScale");
          assert null != m;
          assert m.getUnitsString().equals("meters");
        }
      }
    }
  }
}
