/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

    TableConfig profile = new TableConfig(Table.Type.Structure, "record");
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
