/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caron
 * @since Jan 19, 2008
 */
@Category(NeedsCdmUnitTest.class)
public class TestStructureSubset extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  NetcdfFile ncfile;
  protected void setUp() throws Exception {
    ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir+"ft/station/Surface_METAR_20080205_0000.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }
  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testReadStructureSubset() throws IOException, InvalidRangeException {

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    List<String> vars = new ArrayList<String>();
    vars.add( "wind_speed");
    vars.add( "wind_gust");
    vars.add( "report");
    Structure subset = record.select(vars);

    // read entire subset
    ArrayStructure dataAll = (ArrayStructure) subset.read();

    StructureMembers sm = dataAll.getStructureMembers();
    for(StructureMembers.Member m : sm.getMembers()) {
      Variable v = subset.findVariable(m.getName());
      assert v != null;
      Array mdata = dataAll.extractMemberArray(m);
      assert mdata.getShape()[0] == dataAll.getShape()[0];
      assert mdata.getElementType() == m.getDataType().getPrimitiveClassType();
      System.out.println(m.getName()+ " shape="+new Section(mdata.getShape()));
    }
    System.out.println("*** TestStructureSubset ok");
  }

  public void testReadStructureSection() throws IOException, InvalidRangeException {

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    Structure subset = (Structure) record.section(new Section("0:10"));
    assert subset != null;
    assert subset.getRank() == 1;
    assert subset.getSize() == 11;

    // read entire subset
    ArrayStructure dataAll = (ArrayStructure) subset.read(new Section("0:10"));
    assert dataAll.getSize() == 11;

    StructureMembers sm =dataAll.getStructureMembers();
    for(StructureMembers.Member m : sm.getMembers()) {
      Variable v = subset.findVariable(m.getName());
      assert v != null;
      Array mdata = dataAll.extractMemberArray(m);
      assert mdata.getShape()[0] == dataAll.getShape()[0];
      assert mdata.getElementType() == m.getDataType().getPrimitiveClassType();
      System.out.println(m.getName()+ " shape="+new Section(mdata.getShape()));
    }
    System.out.println("*** TestStructureSubset ok");
  }


}
