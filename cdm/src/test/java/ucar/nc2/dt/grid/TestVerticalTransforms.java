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

package ucar.nc2.dt.grid;

import junit.framework.TestCase;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.TestAll;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

public class TestVerticalTransforms extends TestCase {

  public TestVerticalTransforms(String name) {
    super(name);
  }

  public void testWRF() throws Exception {
    testDataset( TestAll.testdataDir + "grid/netcdf/wrf/wrfout_v2_Lambert.nc");
    testDataset( TestAll.testdataDir + "grid/netcdf/wrf/wrfout_d01_2006-03-08_21-00-00");
  }

  private void testDataset( String location) throws IOException, InvalidRangeException {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(location);
    assert dataset != null;

    testGrid( dataset.findGridByName("U"));
    testGrid( dataset.findGridByName("V"));
    testGrid( dataset.findGridByName("W"));
    testGrid( dataset.findGridByName("T"));

    dataset.close();
  }

  private void testGrid( GeoGrid grid) throws IOException, InvalidRangeException {
    assert null != grid;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    Array data = grid.readDataSlice(0, -1, -1, -1);
    assert data.getRank() == 3;

    CoordinateAxis zaxis = gcs.getVerticalAxis();
    assert data.getShape()[0] == zaxis.getSize() : zaxis.getSize();

    CoordinateAxis yaxis = gcs.getYHorizAxis();
    assert data.getShape()[1] == yaxis.getSize() : yaxis.getSize();

    CoordinateAxis xaxis = gcs.getXHorizAxis();
    assert data.getShape()[2] == xaxis.getSize() : xaxis.getSize();

    VerticalTransform vt = gcs.getVerticalTransform();
    assert vt != null;
    assert vt.getUnitString() != null;

    ucar.ma2.ArrayDouble.D3 vcoord = vt.getCoordinateArray(0);
    assert vcoord.getShape()[0] ==  zaxis.getSize() : vcoord.getShape()[0];
    assert vcoord.getShape()[1] ==  yaxis.getSize() : vcoord.getShape()[1];
    assert vcoord.getShape()[2] ==  xaxis.getSize() : vcoord.getShape()[2];
  }

}
