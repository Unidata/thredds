/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.geoloc.LatLonRect;

import java.util.Date;
import java.io.IOException;

/**
 * @author caron
 * @since Sep 7, 2007
 */
public class CFpointObsDataset extends FeatureDatasetImpl implements PointObsDataset {

  public CFpointObsDataset(NetcdfDataset ncfile) {
    super( ncfile);
  }

  public Class getFeatureClass() {
    return PointObsFeature.class;
  }

  public PointCollection subset(LatLonRect boundingBox, Date start, Date end) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public DataCost getDataCost() {
    return null;
  }

  protected void setStartDate() {
  }

  protected void setEndDate() {
  }

  protected void setBoundingBox() {
  }
}
