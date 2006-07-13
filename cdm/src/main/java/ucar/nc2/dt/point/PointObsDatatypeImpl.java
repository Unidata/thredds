// $Id:PointObsDatatypeImpl.java 51 2006-07-12 17:13:13Z caron $
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

import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.EarthLocation;

/**
 * Abstract superclass for implemenation of PointObsDatatype.
 * Concrete subclass must implement getData();
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */


public abstract class PointObsDatatypeImpl implements PointObsDatatype, Comparable {
  protected EarthLocation location;
  protected double obsTime, nomTime;

  public PointObsDatatypeImpl() {
  }

  public PointObsDatatypeImpl( EarthLocation location, double obsTime, double nomTime) {
    this.location = location;
    this.obsTime = obsTime;
    this.nomTime = nomTime;
  }

  public EarthLocation getLocation() { return location; }
  public double getNominalTime() { return nomTime; }
  public double getObservationTime() { return obsTime; }

  public int compareTo(Object o) {
    PointObsDatatypeImpl oo = (PointObsDatatypeImpl) o;
    return (int) (obsTime - oo.obsTime);
  }
}