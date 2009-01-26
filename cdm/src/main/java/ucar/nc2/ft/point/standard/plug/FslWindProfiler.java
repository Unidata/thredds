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

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.*;
import ucar.nc2.constants.FeatureType;

import java.util.Formatter;

/**
 * FLS Wind profile data
 * @author caron
 * @since Apr 23, 2008
 */
public class FslWindProfiler extends TableConfigurerImpl  {

    // :title = "WPDN data : selected by ob time : time range from 1207951200 to 1207954800";
  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String title = ds.findAttValueIgnoreCase(null, "title", null);
    return title != null && (title.startsWith("WPDN data") || title.startsWith("RASS data"));
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    //TableConfig nt = new TableConfig(Table.Type.Singleton, "station");

    TableConfig profile = new TableConfig(Table.Type.MultiDimOuter, "record");
    profile.structName = "record";
    profile.dim = Evaluator.getDimension(ds, "recNum", errlog);
    profile.time = Evaluator.getVariableName(ds, "timeObs", errlog);

    profile.stnId = Evaluator.getVariableName(ds, "staName", errlog);
    profile.stnWmoId = Evaluator.getVariableName(ds, "wmoStaNum", errlog);
    profile.lat = Evaluator.getVariableName(ds, "staLat", errlog);
    profile.lon = Evaluator.getVariableName(ds, "staLon", errlog);
    // profile.elev = Evaluator.getVariableName(ds, "staElev", errlog);

    //profile.join = new TableConfig.JoinConfig(Join.Type.Identity);
    profile.featureType = FeatureType.PROFILE;
    // nt.addChild(profile);

    TableConfig levels = new TableConfig(Table.Type.MultiDimInner, "levels");
    levels.outer = Evaluator.getDimension(ds, "recNum", errlog);
    levels.dim = Evaluator.getDimension(ds, "level", errlog);
    levels.elev = Evaluator.getVariableName(ds, "levels", errlog);

    profile.addChild(levels);
    
    return profile;
  }

}
