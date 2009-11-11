/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.point.standard.TableConfig;
import ucar.nc2.ft.point.standard.Evaluator;
import ucar.nc2.ft.point.standard.Table;
import ucar.nc2.Dimension;

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
