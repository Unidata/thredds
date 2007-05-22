/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.ncml;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Attribute;
import ucar.ma2.DataType;

import java.io.IOException;

/**
 * JoinExistingOne Aggregation.
 *
 * A JoinExisting, with one coordinate per file, with coordValue specified from filename.
 *
 * @author caron
 */
public class AggregationExistingOne extends AggregationExisting {

  public AggregationExistingOne(NetcdfDataset ncd, String dimName, String recheckS) {
    super( ncd, dimName, Aggregation.Type.JOIN_EXISTING_ONE, recheckS);
  }

  protected void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException {
    super.buildDataset(isNew, cancelTask);

    // modify aggregation coordinate variable
    VariableDS joinAggCoord = (VariableDS) ncDataset.getRootGroup().findVariable(dimName);
    joinAggCoord.setDataType( DataType.STRING);
    Attribute att = joinAggCoord.findAttribute("units");
    if (null != att)
      joinAggCoord.remove(att);
  }

}
