/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Formatter;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.grib.coverage.GribCoverageDataset;

/**
 * PartitionCollection for Grib2.
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2Partition extends PartitionCollectionImmutable implements Closeable {

  Grib2Partition( PartitionCollectionMutable pc) {
    super(pc);
  }

  @Override
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(group, ds.getType());
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
    return new NetcdfDataset(ncfile);
  }

  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(group, ds.getType());
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return new ucar.nc2.dt.grid.GridDataset(ncd);
  }

  @Override
  public CoverageCollection getGridCoverage(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) {

    GribCoverageDataset gribCov = new GribCoverageDataset(this, ds, group);
    return gribCov.createCoverageCollection();
  }

  @Override
  public void addGlobalAttributes(AttributeContainer result) {
    String val = cust.getGeneratingProcessTypeName(getGenProcessType());
    if (val != null)
      result.addAttribute(new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(getBackProcessId());
    if (val != null)
      result.addAttribute(new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB2.getDescription()));
  }

  @Override
  public void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex) {
    Grib2Collection.addVariableAttributes(v, vindex, this);
  }

  @Override
  protected String makeVariableId(VariableIndex v) {
    return Grib1Collection.makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

}
