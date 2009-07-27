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

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

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

    assert times.getDimension(0).getName().equals("time");
  }
}
