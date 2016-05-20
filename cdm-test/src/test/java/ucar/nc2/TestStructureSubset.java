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

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author caron
 * @since Jan 19, 2008
 */
@Category(NeedsCdmUnitTest.class)
public class TestStructureSubset extends TestCase {

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
