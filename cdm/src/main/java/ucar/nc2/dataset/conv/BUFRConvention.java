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

package ucar.nc2.dataset.conv;

import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.ProjectionCT;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.ma2.StructureData;

import java.io.IOException;

/**
 * Process BUFR files read through the CDM
 *
 * @author caron
 * @since Aug 19, 2008
 */
public class BUFRConvention extends CoordSysBuilder {

  public BUFRConvention() {
    this.conventionName = "BUFR/CDM";
  }

  /**
   * create a NetcdfDataset out of this NetcdfFile, adding coordinates etc.
   */
  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    if (null != ds.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.
  }

 
}
