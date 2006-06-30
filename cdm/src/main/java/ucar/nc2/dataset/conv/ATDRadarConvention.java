// $Id: ATDRadarConvention.java,v 1.7 2006/01/14 22:15:01 caron Exp $
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

import java.io.IOException;

/**
 * ATD Radar file (ad hoc guesses).
 *
 * @author caron
 * @version $Revision: 1.7 $ $Date: 2006/01/14 22:15:01 $
 */

public class ATDRadarConvention extends CoordSysBuilder {

  /** return true if we think this is a ATDRadarConvention file. */
  public static boolean isMine( NetcdfFile ncfile) {
    // not really sure until we can examine more files
    String s =  ncfile.findAttValueIgnoreCase(null, "sensor_name", "none");
    return s.equalsIgnoreCase("CRAFT/NEXRAD");
  }

  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
    this.conventionName = "ATDRadar";
    NcMLReader.wrapNcMLresource( ncDataset, CoordSysBuilder.resourcesDir+"ATDRadar.ncml", cancelTask);
  }

}

/**
 * $Log: ATDRadarConvention.java,v $
 * Revision 1.7  2006/01/14 22:15:01  caron
 * Use CoordSysBuilderIF
 *
 * Revision 1.6  2005/02/20 00:36:59  caron
 * reorganize resources
 *
 * Revision 1.5  2005/01/05 22:47:13  caron
 * no message
 *
 * Revision 1.4  2004/12/10 17:04:17  caron
 * *** empty log message ***
 *
 * Revision 1.3  2004/12/01 05:53:39  caron
 * ncml pass 2, new convention parsing
 *
 * Revision 1.2  2004/09/28 21:30:47  caron
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