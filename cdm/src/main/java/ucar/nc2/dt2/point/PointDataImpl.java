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
package ucar.nc2.dt2.point;

import ucar.nc2.dt2.PointData;
import ucar.nc2.dt2.EarthLocation;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureData;

import java.util.Date;
import java.io.IOException;

/**
 * @author caron
 * @since Feb 29, 2008
 */
public class PointDataImpl implements PointData {
  double time;
  EarthLocation loc;
  DateUnit dateUnit;
  StructureData sdata;

  public PointDataImpl(EarthLocation loc, double time, DateUnit dateUnit, StructureData sdata) {
    this.time = time;
    this.dateUnit = dateUnit;
    this.loc = loc;
    this.sdata = sdata;
  }

  public double getObservationTime() {
    return time;
  }

  public Date getObservationTimeAsDate() {
    return dateUnit.makeDate(time);
  }

  public EarthLocation getLocation() {
    return loc;
  }

  public StructureData getData() throws IOException {
    return sdata;
  }
}
