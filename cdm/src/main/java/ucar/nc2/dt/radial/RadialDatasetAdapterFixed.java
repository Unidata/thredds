// $Id: RadialDatasetAdapterFixed.java,v 1.2 2005/04/21 01:34:03 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.nc2.dt.radial;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

import java.util.*;

/**
 * Make a NetcdfDataset into a RadialDataset.
 *
 * @author caron
 * @version $Revision: 1.2 $ $Date: 2005/04/21 01:34:03 $
 */

public abstract class RadialDatasetAdapterFixed extends RadialDatasetAdapter implements RadialDatasetFixed {
  protected EarthLocation origin;

  public RadialDatasetAdapterFixed( NetcdfDataset ds) {
    super(ds);
  }

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append(super.getDetailInfo());
    sbuff.append("  center= "+getEarthLocation()+"\n");
    return sbuff.toString();
  }

  protected abstract void setEarthLocation(); // reminder for subclasses to set this

  public ucar.nc2.dt.EarthLocation getEarthLocation() { return origin; }

  // you must set EarthLocation before you call this.
  protected void setBoundingBox() {
    LatLonRect largestBB = null;
    // look through all the coord systems
    Iterator iter = csHash.values().iterator();
    while (iter.hasNext()) {
      RadialCoordSys sys = (RadialCoordSys) iter.next();
      sys.setOrigin( origin);
      LatLonRect bb = sys.getBoundingBox();
      if (largestBB == null)
        largestBB = bb;
      else
        largestBB.extend( bb);
    }
    boundingBox = largestBB;
  }
}
