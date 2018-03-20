/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.standard.plug;

import ucar.nc2.ft.point.standard.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;

import java.util.Formatter;

/**
 * NOT DONE YET
 * NDBC (National Data Buoy Center) Convention with netcdf4 files
 * @author caron
 * @since Sept 6, 2011
 */
public class NdbcNetcdf4 extends TableConfigurerImpl  {

  public boolean isMine(FeatureType wantFeatureType, NetcdfDataset ds) {
    if (ds.getFileTypeId().equals("HDF5")) return false;

    String dataProvider = ds.findAttValueIgnoreCase(null, "data_provider", null);
    if (dataProvider == null)
      dataProvider = ds.findAttValueIgnoreCase(null, "institution", "");
    if (!dataProvider.contains("National Data Buoy Center"))
      return false;

    if (null == ds.findAttValueIgnoreCase(null, "station_name", null)) return false;
    if (null == ds.findAttValueIgnoreCase(null, "nominal_latitude", null)) return false;
    if (null == ds.findAttValueIgnoreCase(null, "nominal_longitude", null)) return false;

    return true;
  }

  /*
  <!-- C:/data/dt2/station/ndbc.nc -->
  <stationFeature>
    <stationId>":station"</stationId>
    <stationDesc>":description"</stationDesc>
    <coordAxis type="lat">lat</coordAxis>
    <coordAxis type="lon">lon</coordAxis>
    <coordAxis type="height">0</coordAxis>
    <table dim="time">
      <coordAxis type="time">time</coordAxis>
    </table>
  </stationFeature>
   */

   public TableConfig getConfig(FeatureType wantFeatureType, NetcdfDataset ds, Formatter errlog) {
    Dimension obsDim = ds.findDimension("time");
    if (obsDim == null) {
      CoordinateAxis axis = CoordSysEvaluator.findCoordByType(ds, AxisType.Time);
      if ((axis != null) && axis.isScalar())
        obsDim = axis.getDimension(0);
    }

    if (obsDim == null) {
      errlog.format("Must have an Observation dimension: unlimited dimension, or from Time Coordinate");
      return null;
    }
     boolean hasStruct = Evaluator.hasNetcdf3RecordStructure(ds);



    // otherwise, make it a Station
    TableConfig nt = new TableConfig(Table.Type.Top, "station");
    nt.featureType = FeatureType.STATION;

    nt.lat = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lat);
    nt.lon = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Lon);

    nt.stnId = ds.findAttValueIgnoreCase(null, "station_name", null);
    nt.stnWmoId = ds.findAttValueIgnoreCase(null, "wmo_id", null);

    nt.stnDesc = ds.findAttValueIgnoreCase(null, "description", null);
    if (nt.stnDesc == null)
      nt.stnDesc = ds.findAttValueIgnoreCase(null, "comment", null);

    TableConfig obs = new TableConfig(Table.Type.Structure, hasStruct ? "record" : obsDim.getShortName());
    obs.structName = "record";
    obs.structureType = hasStruct ? TableConfig.StructureType.Structure : TableConfig.StructureType.PsuedoStructure;
    obs.dimName = obsDim.getShortName();
    obs.time = CoordSysEvaluator.findCoordNameByType(ds, AxisType.Time);
    nt.addChild(obs);

    return nt;
  }
}
