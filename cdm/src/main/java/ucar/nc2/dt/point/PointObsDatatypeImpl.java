// $Id: PointObsDatatypeImpl.java,v 1.2 2005/05/15 23:06:51 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * @author john
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