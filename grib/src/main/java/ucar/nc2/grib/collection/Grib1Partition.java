/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import ucar.nc2.*;
import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.coverage.GribCoverageDataset;

import java.io.IOException;
import java.util.Formatter;

/**
 * PartitionCollection for Grib1.
 *
 * @author caron
 * @since 2/21/14
 */
public class Grib1Partition extends PartitionCollectionImmutable {

  Grib1Partition( PartitionCollectionMutable pc) {
    super(pc);
  }

  @Override
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib1Iosp iosp = new ucar.nc2.grib.collection.Grib1Iosp(group, ds.getType());
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
    return new NetcdfDataset(ncfile);
  }

  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib1Iosp iosp = new ucar.nc2.grib.collection.Grib1Iosp(group, ds.getType());
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
    String val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null) result.addAttribute(new Attribute(GribUtils.GEN_PROCESS, val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB1.getDescription()));
  }

  @Override
  public void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex) {
    Grib1Collection.addVariableAttributes(v, vindex, this);
  }

  @Override
  public String makeVariableId(GribCollectionImmutable.VariableIndex v) {
    return Grib1Collection.makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

}
