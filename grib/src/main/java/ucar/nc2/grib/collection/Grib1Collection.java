/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.nc2.*;
import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.coverage.GribCoverageDataset;
import ucar.nc2.grib.grib1.Grib1Parameter;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib1-specific subclass of GribCollection.
 *
 * @author John
 * @since 9/5/11
 */
public class Grib1Collection extends GribCollectionImmutable {

  Grib1Collection(GribCollectionMutable gc) {
    super(gc);
  }

  @Override
  @Nullable
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename, FeatureCollectionConfig gribConfig,
                                                         Formatter errlog, org.slf4j.Logger logger) throws IOException {
    if (filename == null) {
      Grib1Iosp iosp = new Grib1Iosp(group, ds.getType());
      NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
      return new NetcdfDataset(ncfile);

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(true, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;

        Grib1Iosp iosp = new Grib1Iosp(gc);
        NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
        return new NetcdfDataset(ncfile);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename, FeatureCollectionConfig gribConfig,
                                                     Formatter errlog, org.slf4j.Logger logger) throws IOException {
    if (filename == null) {
      Grib1Iosp iosp = new Grib1Iosp(group, ds.getType());
      NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation()+"#"+group.getId(), null);
      NetcdfDataset ncd = new NetcdfDataset(ncfile);
      return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(true, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;

        Grib1Iosp iosp = new Grib1Iosp(gc);
        NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
        NetcdfDataset ncd = new NetcdfDataset(ncfile);
        return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
      }
      return null;
    }
  }

  @Override
  @Nullable
  public CoverageCollection getGridCoverage(Dataset ds, GroupGC group, String filename, FeatureCollectionConfig gribConfig,
                                                     Formatter errlog, org.slf4j.Logger logger) throws IOException {
    if (filename == null) {
      GribCoverageDataset gribCov = new GribCoverageDataset(this, ds, group);
      return gribCov.createCoverageCollection();

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(true, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;
        GribCoverageDataset gribCov = new GribCoverageDataset(gc, null, null);
        return gribCov.createCoverageCollection();
      }
      return null;
    }
  }

  @Override
  public void addGlobalAttributes(AttributeContainer result) {
    String val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(new Attribute(GribUtils.GEN_PROCESS, val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB1.getDescription()));
  }

  @Override
  public String makeVariableId(GribCollectionImmutable.VariableIndex v) {
    return makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

  static String makeVariableId(int center, int subcenter, int tableVersion, int paramNo, int levelType, boolean isLayer, int intvType, String intvName) {
    try (Formatter f = new Formatter()) {

      f.format("VAR_%d-%d-%d-%d", center, subcenter, tableVersion,
          paramNo);  // "VAR_7-15--1-20_L1";

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_L%d", levelType); // code table 4.5
        if (isLayer) {
          f.format("_layer");
        }
      }

      if (intvType >= 0) {
        if (intvName != null) {
          if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS)) {
            f.format("_Imixed");
          } else {
            f.format("_I%s", intvName);
          }
        }
        f.format("_S%s", intvType);
      }

      return f.toString();
    }
  }

  @Override
  public void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex) {
    addVariableAttributes(v, vindex, this);
  }

  static void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex, GribCollectionImmutable gc) {
    Grib1Customizer cust1 = (Grib1Customizer) gc.cust;

    // Grib attributes
    v.addAttribute(new Attribute(Grib.VARIABLE_ID_ATTNAME, gc.makeVariableId(vindex)));
    v.addAttribute(new Attribute("Grib1_Center", gc.getCenter()));
    v.addAttribute(new Attribute("Grib1_Subcenter", gc.getSubcenter()));
    v.addAttribute(new Attribute("Grib1_TableVersion", vindex.getTableVersion()));
    v.addAttribute(new Attribute("Grib1_Parameter", vindex.getParameter()));
    Grib1Parameter param = cust1.getParameter(gc.getCenter(), gc.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
    if (param != null && param.getName() != null)
      v.addAttribute(new Attribute("Grib1_Parameter_Name", param.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING)
      v.addAttribute(new Attribute("Grib1_Level_Type", vindex.getLevelType()));
    String ldesc = cust1.getLevelDescription(vindex.getLevelType());
    if (ldesc != null)
      v.addAttribute(new Attribute("Grib1_Level_Desc", ldesc));


    String timeTypeName = cust1.getTimeTypeName(vindex.getIntvType());
    if ( timeTypeName != null && timeTypeName.length() != 0) {
      v.addAttribute(new Attribute("Grib1_Interval_Type", vindex.getIntvType()));
      v.addAttribute(new Attribute("Grib1_Interval_Name", timeTypeName));
    }

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib1_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0)
      v.addAttribute(new Attribute("Grib1_Probability_Type", vindex.getProbabilityName()));
  }

}
