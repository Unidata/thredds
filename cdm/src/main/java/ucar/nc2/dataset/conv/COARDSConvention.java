// $Id: COARDSConvention.java,v 1.8 2006/01/14 22:15:02 caron Exp $
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
package ucar.nc2.dataset.conv;

import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.util.CancelTask;

import java.util.List;
import java.util.StringTokenizer;
import java.io.IOException;

/**
 * COARDS Convention.
 * see http://ferret.wrc.noaa.gov/noaa_coop/coop_cdf_profile.html
 *
 * @author caron
 * @version $Revision: 1.8 $ $Date: 2006/01/14 22:15:02 $
 */

public class COARDSConvention extends CoordSysBuilder {

  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) {
    this.conventionName = "COARDS";
  }

  // we assume that coordinate axes get identified by being coordinate variables

  protected AxisType getAxisType( NetcdfDataset ncDataset, VariableEnhanced v) {

    String unit = v.getUnitsString();
    if (unit == null)
      return null;

    if( unit.equalsIgnoreCase("degrees_east") ||
            unit.equalsIgnoreCase("degrees_E") ||
            unit.equalsIgnoreCase("degreesE") ||
            unit.equalsIgnoreCase("degree_east") ||
            unit.equalsIgnoreCase("degree_E") ||
            unit.equalsIgnoreCase("degreeE"))
      return AxisType.Lon;

    if ( unit.equalsIgnoreCase("degrees_north") ||
            unit.equalsIgnoreCase("degrees_N") ||
            unit.equalsIgnoreCase("degreesN") ||
            unit.equalsIgnoreCase("degree_north") ||
            unit.equalsIgnoreCase("degree_N") ||
            unit.equalsIgnoreCase("degreeN"))
      return AxisType.Lat;

    if (SimpleUnit.isDateUnit(unit) || SimpleUnit.isTimeUnit(unit))
      return AxisType.Time;

    // look for other z coordinate
    if (SimpleUnit.isCompatible("m", unit))
      return AxisType.Height;
    if (SimpleUnit.isCompatible("mbar", unit))
      return AxisType.Pressure;
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
      return AxisType.GeoZ;

    String positive = ncDataset.findAttValueIgnoreCase((Variable) v, "positive", null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
      else
        return AxisType.GeoZ;
    }

    return null;
  }

}

    /*
        /* kludge in fixing the units for gds
    String historyAtt = ncDataset.findAttValueIgnoreCase(null, "history", null);
    if (historyAtt != null) {
      historyAtt = historyAtt.toLowerCase();
      if (historyAtt.indexOf("grads-dods") >= 0) {
        createUnitsFromLongName(ncDataset);
      }
    }


  private void createUnitsFromLongName(NetcdfDataset ncDataset) {
    // kludge in fixing the units
    List vlist = ncDataset.getVariables();
    for (int i=0; i<vlist.size(); i++) {
      Variable v = (Variable) vlist.get(i);
      Attribute attLongName = v.findAttributeIgnoreCase( "long_name");
      Attribute attUnit = v.findAttributeIgnoreCase( "units");
      if ((attLongName != null) && (attUnit == null)) {
        String longName = attLongName.getStringValue();
        int pos1 = longName.indexOf('[');
        int pos2 = longName.indexOf(']');
        if ((pos1 >= 0) && (pos2 > pos1)) {
          String units =  longName.substring(pos1+1, pos2);
          v.addAttribute( new Attribute( "units",  units));
        }
      }
    }
  } */


/**
 * $Log: COARDSConvention.java,v $
 * Revision 1.8  2006/01/14 22:15:02  caron
 * Use CoordSysBuilderIF
 *
 * Revision 1.7  2005/08/10 20:03:42  caron
 * Vertical coord doesnt need positive attribute
 *
 * Revision 1.6  2004/12/07 01:29:28  caron
 * redo convention parsing, use _Coordinate encoding.
 *
 * Revision 1.5  2004/12/03 04:46:24  caron
 * no message
 *
 * Revision 1.4  2004/12/01 05:53:40  caron
 * ncml pass 2, new convention parsing
 *
 * Revision 1.3  2004/10/19 19:45:02  caron
 * misc
 *
 * Revision 1.2  2004/09/09 22:47:40  caron
 * station updates
 *
 * Revision 1.1  2004/08/16 20:53:49  caron
 * 2.2 alpha (2)
 *
 * Revision 1.3  2004/06/07 20:24:52  caron
 * move SimpleUnit to ucar.unit.units, add aliases in UnitDB
 *
 * Revision 1.2  2003/07/12 22:09:04  caron
 * add vertical transformations
 *
 * Revision 1.1  2003/04/08 15:06:26  caron
 * nc2 version 2.1
 *
 *
 */