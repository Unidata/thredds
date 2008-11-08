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

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.ft.StationImpl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Structure;
import ucar.ma2.*;

import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Start of a CF parser. For now, its specific to TDS workshop file from imedea.
 *
 * @author caron
 * @since Nov 3, 2008
 */
public class CFpoint implements TableConfigurer {
  public enum CFFeatureType { point, station }

  public boolean isMine(NetcdfDataset ds) {
    // find datatype
    String datatype = ds.findAttValueIgnoreCase(null, "CF:featureType", null);
    if (datatype == null)
      return false;
    if (!datatype.equalsIgnoreCase(CFFeatureType.point.toString()))
      return false;

    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.startsWith("CF"))
        return true;
    }
    return false;
  }

  public TableConfig getConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(NestedTable.TableType.Singleton, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = Evaluator.getVariableName(ds, "latitude", errlog);
    nt.lon = Evaluator.getVariableName(ds, "longitude", errlog);

    nt.stnId = Evaluator.getVariableName(ds, "stn", errlog);
    //nt.stnDesc = Evaluator.getVariableName(ds, ":description", errlog);

    TableConfig obs = new TableConfig(NestedTable.TableType.Structure, "record");
    obs.dim = Evaluator.getDimension(ds, "time", errlog);
    obs.time = Evaluator.getVariableName(ds, "time", errlog);
    nt.addChild(obs);

    obs.join = new TableConfig.JoinConfig(Join.Type.Singleton);

    return nt;
  }
}
