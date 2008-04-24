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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Attribute;

import java.util.Formatter;

/**
 * @author caron
 * @since Apr 23, 2008
 */
public class Madis implements TableConfigurer {

  @Override
  public boolean isMine(NetcdfDataset ds) {
    if (ds.findVariable("staticIds") == null) return false;
    if (ds.findVariable("nStaticIds") == null) return false;
    if (ds.findVariable("lastRecord") == null) return false;
    if (ds.findVariable("prevRecord") == null) return false;
    if (ds.findVariable("latitude") == null) return false;
    if (ds.findVariable("longitude") == null) return false;

    if (!ds.hasUnlimitedDimension()) return false;
    if (ds.findGlobalAttribute("timeVariables") == null) return false;
    if (ds.findGlobalAttribute("idVariables") == null) return false;

    Attribute att = ds.findGlobalAttribute("title");
    if ((att != null) && att.getStringValue().equals("MADIS ACARS data")) return false;

    return true;
  }

  /*
  <!-- C:/data/dt2/station/madis2.sao -->
  <stationCollection>
    <table dim="maxStaticIds" limit="nStaticIds">
      <lastLink>lastRecord</lastLink>

      <table dim="recNum">
        <stationId>:stationIdVariable</stationId>
        <stationWmoId>wmoId</stationWmoId>
        <coordAxis type="time">timeObs</coordAxis>
        <coordAxis type="lat">latitude</coordAxis>
        <coordAxis type="lon">longitude</coordAxis>
        <coordAxis type="height">elevation</coordAxis>
        <prevLink>prevRecord</prevLink>
      </table>

    </table>

    <cdmDataType>:thredds_data_type</cdmDataType>
  </stationCollection>
   */

  @Override
  public TableConfig getConfig(NetcdfDataset ds, Formatter errlog) {
    TableConfig nt = new TableConfig(NestedTable.TableType.PseudoStructure, "station");
    nt.featureType = Evaluator.getFeatureType(ds, ":thredds_data_type", errlog);

    nt.dim = Evaluator.getDimension(ds, "maxStaticIds", errlog);
    nt.limit = Evaluator.getVariableName(ds, "nStaticIds", errlog);

    TableConfig obs = new TableConfig(NestedTable.TableType.Structure, "record");
    obs.dim = Evaluator.getDimension(ds, "recNum", errlog);
    obs.time = Evaluator.getVariableName(ds, "timeObs", errlog);
    obs.timeNominal = Evaluator.getVariableName(ds, "timeNominal", errlog);

    obs.stnId = Evaluator.getVariableName(ds, ":stationIdVariable", errlog);
    obs.stnWmoId = Evaluator.getVariableName(ds, "wmoId", errlog);
    obs.lat = Evaluator.getVariableName(ds, "latitude", errlog);
    obs.lon = Evaluator.getVariableName(ds, "longitude", errlog);

    TableConfig.JoinConfig join = new TableConfig.JoinConfig(Join.Type.BackwardLinkedList);
    join.start = Evaluator.getVariableName(ds, "lastRecord", errlog);
    join.next = Evaluator.getVariableName(ds, "prevRecord", errlog);
    obs.join = join;

    nt.addChild(obs);
    return nt;
  }


}
