/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.grib.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.http.HTTPRandomAccessFile;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib-1 Collection IOSP.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib1Iosp extends GribIosp {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);

  @Override
  public String makeVariableName(GribCollectionImmutable.VariableIndex v) {
    return makeVariableNameFromTables(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

  public static String makeVariableName(Grib1Customizer cust, FeatureCollectionConfig.GribConfig gribConfig, Grib1SectionProductDefinition pds) {
    return makeVariableNameFromTables(cust, gribConfig, pds.getCenter(), pds.getSubCenter(), pds.getTableVersion(), pds.getParameterNumber(),
            pds.getLevelType(), cust.isLayer(pds.getLevelType()), pds.getTimeRangeIndicator(), null);
  }

  private String makeVariableNameFromTables(int center, int subcenter, int version, int paramNo, int levelType, boolean isLayer, int intvType, String intvName) {
    return makeVariableNameFromTables(cust, config.gribConfig, center, subcenter, version, paramNo, levelType, isLayer, intvType, intvName);
  }

  private static String makeVariableNameFromTables(Grib1Customizer cust, FeatureCollectionConfig.GribConfig gribConfig, int center, int subcenter, int version, int paramNo,
                                 int levelType, boolean isLayer, int timeRangeIndicator, String intvName) {
    try (Formatter f = new Formatter()) {

      Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo); // code table 2
      if (param == null) {
        f.format("VAR%d-%d-%d-%d", center, subcenter, version, paramNo);
      } else {
        if (param.useName()) {
          f.format("%s", param.getName());
        } else {
          f.format("%s", GribUtils.makeNameFromDescription(param.getDescription()));
        }
      }

      if (gribConfig.useTableVersion) {
        f.format("_TableVersion%d", version);
      }

      if (gribConfig.useCenter) {
        f.format("_Center%d", center);
      }

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_%s", cust.getLevelNameShort(levelType)); // code table 3
        if (isLayer) {
          f.format("_layer");
        }
      }

      if (timeRangeIndicator >= 0) {
        GribStatType stat = cust.getStatType(timeRangeIndicator);
        if (stat != null) {
          if (intvName != null) {
            f.format("_%s", intvName);
          }
          f.format("_%s", stat.name());
        } else {
          if (intvName != null) {
            f.format("_%s", intvName);
          }
          // f.format("_%d", timeRangeIndicator);
        }
      }

      return f.toString();
    }
  }

  @Override
  public String makeVariableLongName(GribCollectionImmutable.VariableIndex v) {
    return makeVariableLongName(gribCollection.getCenter(), gribCollection.getSubcenter(), v.getTableVersion(), v.getParameter(),
            v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName(), v.getProbabilityName());
  }


  private String makeVariableLongName(int center, int subcenter, int version, int paramNo,
      int levelType, boolean isLayer, int intvType, String intvName, String probabilityName) {
    return makeVariableLongName(cust, center, subcenter, version, paramNo, levelType, isLayer, intvType, intvName, probabilityName);
  }

  static String makeVariableLongName(Grib1Customizer cust, int center, int subcenter, int version,
      int paramNo, int levelType,
      boolean isLayer, int intvType, String intvName, String probabilityName) {
    try (Formatter f = new Formatter()) {

      boolean isProb = (probabilityName != null && probabilityName.length() > 0);
      if (isProb) {
        f.format("Probability ");
      }

      Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
      if (param == null) {
        f.format("Unknown Parameter %d-%d-%d-%d", center, subcenter, version, paramNo);
      } else {
        f.format("%s", param.getDescription());
      }

      if (intvType >= 0) {
        GribStatType stat = cust.getStatType(intvType);
        if (stat != null) {
          f.format(" (%s %s)", intvName, stat.name());
        } else if (intvName != null && intvName.length() > 0) {
          f.format(" (%s)", intvName);
        }
      }

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format(" @ %s", cust.getLevelDescription(levelType));
        if (isLayer) {
          f.format(" layer");
        }
      }

      return f.toString();
    }
  }

  @Override
  protected String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableUnits(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
  }

  private String makeVariableUnits(int center, int subcenter, int version, int paramNo) {
    Grib1Parameter param = cust.getParameter(center, subcenter, version, paramNo);
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  static String makeVariableUnits(Grib1Customizer cust, GribCollectionImmutable gribCollection,
      GribCollectionImmutable.VariableIndex vindex) {
    Grib1Parameter param = cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
    String val = (param == null) ? "" : param.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib1Customizer cust;

  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (raf instanceof HTTPRandomAccessFile) { // only do remote if memory resident
      if (raf.length() > raf.getBufferSize())
        return false;

    } else {                                  // wont accept remote index
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      if (type == GribCdmIndex.GribCollectionType.GRIB1) return true;
      if (type == GribCdmIndex.GribCollectionType.Partition1) return true;
    }

    // check for GRIB1 data file
    return Grib1RecordScanner.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.GRIB1.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "GRIB1 Collection";
  }

  // public no-arg constructor for reflection
  public Grib1Iosp() {
    super(true, logger);
  }

  public Grib1Iosp(GribCollectionImmutable.GroupGC gHcs, GribCollectionImmutable.Type gtype) {
    super(true, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  public Grib1Iosp(GribCollectionImmutable gc) {
    super(true, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  protected ucar.nc2.grib.GribTables createCustomizer() throws IOException {
    Grib1ParamTables tables = (config.gribConfig.paramTable != null) ? Grib1ParamTables.factory(config.gribConfig.paramTable) :
            Grib1ParamTables.factory(config.gribConfig.paramTablePath, config.gribConfig.lookupTablePath); // so an iosp message must be received before the open()

    cust = Grib1Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), tables);
    return cust;
  }

  @Override
  protected String getVerticalCoordDesc(int vc_code) {
    return cust.getLevelDescription(vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex) {
    return cust.getParameter(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getVersion(), vindex.getParameter());
  }

  public Object getLastRecordRead() {
    return Grib1Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib1Record.lastRecordRead = null;
  }

  public Object getGribCustomizer() {
    return cust;
  }
}
