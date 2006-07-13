// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.point;
import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

import java.io.IOException;

/** Test StationObsDataset adapters in the JUnit framework. */

public class TestScaleOffsetMissing extends TestCase {
  String topDir = ucar.nc2.TestAll.testdataDir+ "point/netcdf/";
  public TestScaleOffsetMissing( String name) {
    super(name);
  }

  public void testNetcdfFile() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openFile(TestAll.reletiveDir+"testScaleRecord.nc", null);
    Variable v = ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.SHORT;

    Array data = v.read();
    Index ima = data.getIndex();
    short val = data.getShort( ima);
    assert val == -999;

    assert v.getUnitsString().equals("m");
    v.addAttribute( new Attribute("units", "meters"));
    assert v.getUnitsString().equals("meters");

    ncfile.addRecordStructure();
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

    double dval = sdata.convertScalarDouble( m);
    assert dval == -999.0;

    int count = 0;
    Structure.Iterator siter = s.getStructureIterator();
    while (siter.hasNext()) {
      sdata = siter.next();
      m = sdata.findMember("testScale");
      assert m.getUnitsString().equals("meters");

      assert null != m;
      dval = sdata.convertScalarDouble( m);
      double expect = (count == 0) ? -999.0 : 13.0;
      assert dval == expect : dval + "!="+ expect ;
      count++;
    }

    ncfile.close();
  }

  public void testNetcdfDataset() throws IOException, InvalidRangeException {
    NetcdfDataset ncfile = NetcdfDataset.openDataset(TestAll.reletiveDir+"testScaleRecord.nc");
    VariableDS v = (VariableDS) ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.FLOAT;

    Array data = v.read();
    Index ima = data.getIndex();
    float val = data.getFloat( ima);
    assert Float.isNaN(val) : val;

    ncfile.addRecordStructure();
    Structure s = (Structure) ncfile.findVariable("record");
    assert (s != null);

    StructureData sdata = s.readStructure(0);
    StructureMembers.Member m = sdata.findMember("testScale");
    assert null != m;
    float dval = sdata.convertScalarFloat( m);
    assert Float.isNaN(dval) : dval;

    int count = 0;
    Structure.Iterator siter = s.getStructureIterator();
    while (siter.hasNext()) {
      sdata = siter.next();
      m = sdata.findMember("testScale");
      assert null != m;

      dval = sdata.convertScalarFloat( m);
      if (count == 0)
        assert Float.isNaN(dval) : dval;
      else
        assert TestAll.closeEnough(dval, 1040.8407) : dval;
      count++;
    }

    ncfile.close();
  }

  public void testNetcdfDatasetAttributes() throws IOException, InvalidRangeException {
    NetcdfDataset ncfile = NetcdfDataset.openDataset(TestAll.reletiveDir+"testScaleRecord.nc");
    VariableDS v = (VariableDS) ncfile.findVariable("testScale");
    assert null != v;
    assert v.getDataType() == DataType.FLOAT;

    assert v.getUnitsString().equals("m");
    v.addAttribute( new Attribute("units", "meters"));
    assert v.getUnitsString().equals("meters");

    ncfile.addRecordStructure();
    Structure s = (Structure) ncfile.findVariable("record");
    assert (s != null);

    Variable v2 = s.findVariable("testScale");
    v2.addAttribute( new Attribute("units", "meters"));
    assert v2.getUnitsString().equals("meters");

    StructureData sdata = s.readStructure(0);
    StructureMembers.Member m = sdata.findMember("testScale");
    assert m.getUnitsString().equals("meters");
    assert null != m;

    Structure.Iterator siter = s.getStructureIterator();
    while (siter.hasNext()) {
      sdata = siter.next();
      m = sdata.findMember("testScale");
      assert null != m;
      assert m.getUnitsString().equals("meters");
    }

    ncfile.close();
  }
}

/* Change History:
   $Log: TestScaleOffsetMissing.java,v $
   Revision 1.3  2005/07/25 00:07:12  caron
   cache debugging

   Revision 1.2  2005/05/23 20:55:36  caron
   fix member.getUnitsString() when attribute is changed

   Revision 1.1  2005/05/23 20:18:38  caron
   refactor for scale/offset/missing

*/