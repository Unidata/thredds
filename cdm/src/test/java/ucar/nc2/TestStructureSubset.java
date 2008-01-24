/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

import ucar.ma2.*;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jan 19, 2008
 */
public class TestStructureSubset extends TestCase {

  NetcdfFile ncfile;
  protected void setUp() throws Exception {
    ncfile = NetcdfFile.open("C:/data/metars/Surface_METAR_20070330_0000.nc");
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }
  protected void tearDown() throws Exception {
    ncfile.close();
  }

  public void testReadStructureSubset() throws IOException, InvalidRangeException {

    Structure record = (Structure) ncfile.findVariable("record");
    assert record != null;

    List<Variable> vars = new ArrayList<Variable>();
    vars.add( record.findVariable("wind_speed"));
    vars.add( record.findVariable("wind_gust"));
    vars.add( record.findVariable("report"));
    Structure subset = record.subsetMembers(vars);

    // read entire subset
    ArrayStructure dataAll = (ArrayStructure) subset.read();

    StructureMembers sm =dataAll.getStructureMembers();
    for(StructureMembers.Member m : sm.getMembers()) {
      Variable v = subset.findVariable(m.getName());
      assert v != null;
      Array mdata = dataAll.getMemberArray(m);
      assert mdata.getShape()[0] == dataAll.getShape()[0];
      assert mdata.getElementType() == m.getDataType().getPrimitiveClassType();
      System.out.println(m.getName()+ " shape="+new Section(mdata.getShape()));
    }
    System.out.println("*** TestStructureSubset ok");
  }

}
