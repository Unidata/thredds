// $Id: RadialDatasetAdapter.java,v 1.4 2005/05/11 00:10:02 caron Exp $
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
import ucar.nc2.dt.RadialDataset.RadialVariable;
import ucar.nc2.units.DateUnit;

import java.util.*;

/**
 * Make a NetcdfDataset into a RadialDataset.
 *
 * @author caron
 * @version $Revision: 1.4 $ $Date: 2005/05/11 00:10:02 $
 */

public abstract class RadialDatasetAdapter extends TypedDatasetImpl implements RadialDataset {
  protected HashMap csHash = new HashMap();
  protected ucar.nc2.units.DateUnit dateUnits;

  /**
   * Create a GridDataset from a NetcdfDataset.
   * @param ds underlying NetcdfDataset.
   */
  protected RadialDatasetAdapter( NetcdfDataset ds) {
    super(ds);
    //this.ds = ds;

    // look for radial data variables
    parseInfo.append("RadialDatasetAdapter look for RadialVariables\n");
    List vars  = ds.getVariables();
    for (int i=0; i< vars.size(); i++) {
      VariableEnhanced varDS = (VariableEnhanced) vars.get(i);
      constructCoordinateSystems( ds, varDS);
    }
  }

  protected void constructCoordinateSystems(NetcdfDataset ds, VariableEnhanced v) {

      if (v instanceof StructureDS) {
        StructureDS s = (StructureDS) v;
        List members = s.getVariables();
        for (int i = 0; i < members.size(); i++) {
          VariableEnhanced nested =  (VariableEnhanced) members.get(i);
          // LOOK flatten here ??
          constructCoordinateSystems( ds, nested);
        }
      } else {

        // see if it has a radialCS
        // LOOK: should add geogrid it multiple times if there are multiple geoCS ??
        RadialCoordSys rcs = null;
        List csys  = v.getCoordinateSystems();
        for (int j=0; j< csys.size(); j++) {
          CoordinateSystem cs = (CoordinateSystem) csys.get(j);
          rcs = RadialCoordSys.makeRadialCoordSys( parseInfo, cs, v);
          if (rcs != null) break;
        }

        if (rcs != null)
          addRadialVariable( v, rcs);
        }
   }

  protected void addRadialVariable( VariableEnhanced varDS, RadialCoordSys gcs) {
    RadialCoordSys gcsUse = (RadialCoordSys) csHash.get( gcs.getName());
    if (null == gcsUse) {
      csHash.put( gcs.getName(), gcs);
      parseInfo.append(" -make new RadialCoordSys= "+gcs.getName()+"\n");
      gcsUse = gcs;
    }

    RadialVariable rvar = makeRadialVariable( varDS, gcsUse);
    dataVariables.add( rvar);
  }

  protected abstract RadialVariable makeRadialVariable( VariableEnhanced varDS, RadialCoordSys gcs);

  protected abstract void setTimeUnits(); // reminder for subclasses to set this

  ////////////////////////////////////////////

  public ucar.nc2.units.DateUnit getTimeUnits() { return dateUnits; }

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();
    sbuff.append(super.getDetailInfo());
    sbuff.append("  timeUnits= "+dateUnits+"\n");
    return sbuff.toString();
  }


}
