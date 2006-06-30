// $Id: TestVerticalTransforms.java,v 1.1 2006/06/27 18:25:43 caron Exp $
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

package ucar.nc2.dataset.grid;

import junit.framework.TestCase;

import ucar.nc2.dataset.CoordinateAxis;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Jun 27, 2006
 * Time: 10:07:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestVerticalTransforms extends TestCase {

  public TestVerticalTransforms(String name) {
    super(name);
  }

  public void testWRF() throws Exception {
    testDataset( "R:/testdata/grid/netcdf/wrf/wrfout_v2_Lambert.nc");
    testDataset( "R:/testdata/grid/netcdf/wrf/wrfout_d01_2006-03-08_21-00-00");
  }

  private void testDataset( String location) throws IOException, InvalidRangeException {
    GridDataset dataset = GridDataset.open(location);
    assert dataset != null;

    testGrid( dataset.findGridByName("U"));
    testGrid( dataset.findGridByName("V"));
    testGrid( dataset.findGridByName("W"));
    testGrid( dataset.findGridByName("T"));

    dataset.close();
  }

  private void testGrid( GeoGrid grid) throws IOException, InvalidRangeException {
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
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
