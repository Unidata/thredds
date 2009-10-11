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

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.Structure;

import java.util.StringTokenizer;
import java.util.Formatter;

/**
 * BUFR datasets
 *
 * @author caron
 * @since Jan 24, 2009
 */
public class BufrCdm extends TableConfigurerImpl {
  private final String BufrConvention = "BUFR/CDM";

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    String conv = ds.findAttValueIgnoreCase(null, "Conventions", null);
    if (conv == null) return false;
    if (conv.equals(BufrConvention)) return true;

    StringTokenizer stoke = new StringTokenizer(conv, ",");
    while (stoke.hasMoreTokens()) {
      String toke = stoke.nextToken().trim();
      if (toke.equals(BufrConvention))
        return true;
    }
    return false;
  }

  public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    String ftypeS = ds.findAttValueIgnoreCase(null, CF.featureTypeAtt, null);
    CF.FeatureType ftype = (ftypeS == null) ? CF.FeatureType.point : CF.FeatureType.valueOf(ftypeS);
    switch (ftype) {
     /*  case point:
        return getPointConfig(ds, errlog);
      case station:
        return getStationConfig(ds, errlog);
      case profile:
        return getProfileConfig(ds, errlog);   */
      case trajectory:
        return getTrajectoryConfig(ds, errlog);
      case stationProfile:
        return getStationProfileConfig(ds, errlog);
      default:
        throw new IllegalStateException("invalid ftype= " + ftype);
    }
  }

  protected TableConfig getTrajectoryConfig(NetcdfDataset ds, Formatter errlog) {

    // the profile values are the inner sequence
    TableConfig obsTable = new TableConfig(Table.Type.Structure, "obsRecord");
    Structure obsStruct = (Structure) ds.findVariable("obsRecord");
    obsTable.structName = obsStruct.getName();
    obsTable.nestedTableName = obsStruct.getShortName();
    obsTable.lat = Evaluator.getNameOfVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Lat.toString());
    obsTable.lon = Evaluator.getNameOfVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Lon.toString());
    obsTable.elev = Evaluator.getNameOfVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Height.toString());
    obsTable.time = Evaluator.getNameOfVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Time.toString());

    return obsTable;
  }


  protected TableConfig getStationProfileConfig(NetcdfDataset ds, Formatter errlog) {
    // construct the station table by reading through the timeseries
    TableConfig stnTable = new TableConfig(Table.Type.Construct, "station");
    stnTable.featureType = FeatureType.STATION_PROFILE;
    stnTable.structName = "obsRecord";

    Structure stnStruct = (Structure) ds.findVariable("obsRecord");
    stnTable.lat = Evaluator.getNameOfVariableWithAttribute(stnStruct, _Coordinate.AxisType, AxisType.Lat.toString());
    stnTable.lon = Evaluator.getNameOfVariableWithAttribute(stnStruct, _Coordinate.AxisType, AxisType.Lon.toString());
    stnTable.stnAlt = Evaluator.getNameOfVariableWithAttribute(stnStruct, _Coordinate.AxisType, AxisType.Height.toString());

    stnTable.stnId = Evaluator.getNameOfVariableWithAttribute(stnStruct, "standard_name", "station_name");
    stnTable.stnWmoId = Evaluator.getNameOfVariableWithAttribute(stnStruct, "standard_name", "wmo_station");
    if (stnTable.stnId == null) stnTable.stnId = stnTable.stnWmoId;

    // the time series is just the outer structure
    TableConfig timeseriesTable = new TableConfig(Table.Type.Structure, "obsRecord");
    timeseriesTable.time = Evaluator.getNameOfVariableWithAttribute(stnStruct, _Coordinate.AxisType, AxisType.Time.toString());
    timeseriesTable.structName = "obsRecord";
    stnTable.addChild(timeseriesTable);

    // the profile values are the inner sequence
    TableConfig obsTable = new TableConfig(Table.Type.NestedStructure, "seq1"); // LOOK name seq1 not good ?
    Structure obsStruct = (Structure) stnStruct.findVariable("seq1");
    obsTable.structName = obsStruct.getName();
    obsTable.nestedTableName = obsStruct.getShortName();
    obsTable.elev = Evaluator.getNameOfVariableWithAttribute(obsStruct, _Coordinate.AxisType, AxisType.Height.toString());
    timeseriesTable.addChild(obsTable);

    return stnTable;
  }

}
