/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.Evaluator;
import ucar.nc2.ft.point.standard.Table;

import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since Nov 10, 2009
 */
public class MadisAcars extends Madis {
   static private final String TRAJ_ID = "en_tailNumber";

   public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if ((wantFeatureType != FeatureType.ANY_POINT) && (wantFeatureType != FeatureType.TRAJECTORY))
      return false;

    String title = ds.findAttValueIgnoreCase(null, "title", null);
    if ((title == null) || !title.equals("MADIS ACARS data")) return false;

    if (!ds.hasUnlimitedDimension()) return false;
    if (ds.findDimension("recNum") == null) return false;

    VNames vn = getVariableNames(ds, null);
    if (ds.findVariable(vn.lat) == null) return false;
    if (ds.findVariable(vn.lon) == null) return false;
    if (ds.findVariable(vn.obsTime) == null) return false;
    if (ds.findVariable(TRAJ_ID) == null) return false;

    return true;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    VNames vn = getVariableNames(ds, errlog);

    TableConfig trajTable = new TableConfig(Table.Type.Construct, "trajectory");
    trajTable.featureType = FeatureType.TRAJECTORY;
    trajTable.feature_id = TRAJ_ID;

    TableConfig obs = new TableConfig(Table.Type.ParentId, "record");
    obs.parentIndex = TRAJ_ID;
    obs.dimName = Evaluator.getDimensionName(ds, "recNum", errlog);
    obs.time = vn.obsTime;
    obs.timeNominal = vn.nominalTime;

    //obs.stnId = vn.stnId;
    //obs.stnDesc = vn.stnDesc;
    obs.lat = vn.lat;
    obs.lon = vn.lon;
    obs.elev = vn.elev;

    trajTable.addChild(obs);
    return trajTable;
  }

}
