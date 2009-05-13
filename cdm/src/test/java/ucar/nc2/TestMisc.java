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
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;

import java.io.IOException;

/**
 * Class Description
 *
 * @author caron
 * @since May 13, 2009
 */
public class TestMisc extends TestCase {

/*  The 3D coordinate array does not return correct shape and values. Just
running this
simple code to get z values..

uri=http://coast-enviro.er.usgs.gov/models/share/erie_test.ncml;
var='temp';

z is of shape 20x2x87, it should be 20x87x193.
*/

  public void testErie() throws IOException, InvalidRangeException {
    String uri = "C:\\data\\work\\signell/erie_test.ncml";
    String var = "temp";

    GridDataset ds = GridDataset.open(uri);
    GeoGrid grid = ds.findGridByName(var);
    Section s = new Section(grid.getShape());
    System.out.printf("var = %s %n", s);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    VerticalTransform vt = gcs.getVerticalTransform();
    ArrayDouble.D3 z = vt.getCoordinateArray(0);
    Section sv = new Section(z.getShape());
    System.out.printf("3dcoord = %s %n", sv);

    s = s.removeRange(0);
    assert s.equals(sv);
  }

}
