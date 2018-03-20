/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Test promoteGlobalAttribute
 *
 * @author caron
 * @since Jan 13, 2009
 */
public class TestAggPromote  extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestAggPromote( String name) {
    super(name);
  }

  public void testPromote1() throws IOException, InvalidRangeException {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='time' type='joinExisting'>\n" +
      "    <promoteGlobalAttribute name='times' orgName='time_coverage_end' />\n" +
      "    <scan dateFormatMark='CG#yyyyDDD_HHmmss' location='src/test/data/ncml/nc/cg/' suffix='.nc' subdirs='false' />\n" +
      "  </aggregation>\n" +
      "</netcdf>";

    String filename = "file:./"+ TestNcML.topDir + "aggExisting1.xml";

    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(xml), null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);

    Variable times = ncfile.findVariable("times");
    assert null != times;
    assert times.getRank() == 1;
    assert times.getSize() == 3;

    assert times.getDimension(0).getShortName().equals("time");
  }
}
