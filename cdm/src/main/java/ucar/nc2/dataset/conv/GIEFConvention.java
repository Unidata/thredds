// $Id: GIEFConvention.java,v 1.6 2006/01/14 22:15:02 caron Exp $
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

import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.util.List;
import java.io.IOException;

/**
 * GEIF Convention.
 * https://www.metnet.navy.mil/~hofschnr/GIEF-F/1.2/
 *
 * @author caron
 * @version $Revision: 1.6 $ $Date: 2006/01/14 22:15:02 $
 */

public class GIEFConvention extends CoordSysBuilder {
  private Attribute translation, affine;

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    this.conventionName = "GIEF";
    NcMLReader.wrapNcMLresource( ds, CoordSysBuilder.resourcesDir+"GIEF.ncml", cancelTask);

    Variable timeVar = ds.findVariable("time");
    String time_units = ds.findAttValueIgnoreCase(null, "time_units", null);
    timeVar.addAttribute( new Attribute( "units", time_units));

    Variable levelVar = ds.findVariable("level");
    String level_units = ds.findAttValueIgnoreCase(null, "level_units", null);
    String level_name = ds.findAttValueIgnoreCase(null, "level_name", null);
    levelVar.addAttribute( new Attribute( "units", level_units));
    levelVar.addAttribute( new Attribute( "long_name", level_name));

    // may be 1 or 2 data variables
    String unit_name = ds.findAttValueIgnoreCase(null, "unit_name", null);
    String parameter_name = ds.findAttValueIgnoreCase(null, "parameter_name", null);
    List vlist = ds.getVariables();
    for (int i = 0; i < vlist.size(); i++) {
      Variable v =  (Variable) vlist.get(i);
      if (v.getRank() > 1) {
        v.addAttribute( new Attribute( "units", unit_name));
        v.addAttribute( new Attribute( "long_name", v.getName()+" "+parameter_name));
        v.addAttribute( new Attribute( "_CoordinateAxes", "time level latitude longitude"));
      }
    }

    translation = ds.findGlobalAttributeIgnoreCase("translation");
    affine = ds.findGlobalAttributeIgnoreCase("affine_transformation");

    // LOOK only handling the 1D case
    // add lat
    double startLat = translation.getNumericValue(1).doubleValue();
    double incrLat = affine.getNumericValue(6).doubleValue();
    Dimension latDim = ds.findDimension( "row");
    Variable latVar = ds.findVariable("latitude");
    ds.setValues( latVar, latDim.getLength(), startLat, incrLat);

    // add lon
    double startLon = translation.getNumericValue(0).doubleValue();
    double incrLon = affine.getNumericValue(3).doubleValue();
    Dimension lonDim = ds.findDimension( "column");
    Variable lonVar = ds.findVariable("longitude");
    ds.setValues( lonVar, lonDim.getLength(), startLon, incrLon);
  }

}

/**
 * $Log: GIEFConvention.java,v $
 * Revision 1.6  2006/01/14 22:15:02  caron
 * Use CoordSysBuilderIF
 *
 * Revision 1.5  2005/02/20 00:37:00  caron
 * reorganize resources
 *
 * Revision 1.4  2005/01/05 22:47:13  caron
 * no message
 *
 * Revision 1.3  2004/12/10 17:04:17  caron
 * *** empty log message ***
 *
 * Revision 1.2  2004/12/01 05:53:40  caron
 * ncml pass 2, new convention parsing
 *
 * Revision 1.1  2004/09/28 21:30:48  caron
 * add GIEF
 *
 * Revision 1.1  2004/08/16 20:53:49  caron
 * 2.2 alpha (2)
 *
 * Revision 1.1  2003/04/08 15:06:26  caron
 * nc2 version 2.1
 *
 *
 */