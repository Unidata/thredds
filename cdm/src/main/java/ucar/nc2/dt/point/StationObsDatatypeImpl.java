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

import ucar.nc2.dt.point.PointObsDatatypeImpl;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.dt.Station;

/**
 * Abstract superclass for implemenation of StationObsDatatype.
 * Concrete subclass must implement getData();
 *
 * @author caron
 * @version $Revision$ $Date$
 */


public abstract class StationObsDatatypeImpl extends PointObsDatatypeImpl implements StationObsDatatype {
  protected Station station;

  public StationObsDatatypeImpl() {
  }

  public StationObsDatatypeImpl( Station station, double obsTime, double nomTime) {
    super(station, obsTime,  nomTime);
    this.station = station;
  }

  public Station getStation() { return station; }
}
