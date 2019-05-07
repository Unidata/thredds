/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import ucar.nc2.grib.coord.CoordinateTimeAbstract;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.coverage.GribCoverageDataset;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib2 specific subclass of GribCollection.
 *
 * @author John
 * @since 9/5/11
 */
public class Grib2Collection extends GribCollectionImmutable {

  Grib2Collection(GribCollectionMutable gc) {
    super(gc);
  }

  @Override
  @Nullable
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
                           FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    if (filename == null) {
      Grib2Iosp iosp = new Grib2Iosp(group, ds.getType());
      NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
      return new NetcdfDataset(ncfile);

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(false, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;

        Grib2Iosp iosp = new Grib2Iosp(gc);
        NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
        return new NetcdfDataset(ncfile);
      }
      return null;
    }
  }

  @Override
  @Nullable
  public ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
               FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    if (filename == null) {
      Grib2Iosp iosp = new Grib2Iosp(group, ds.getType());
      NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation()+"#"+group.getId(), null);
      NetcdfDataset ncd = new NetcdfDataset(ncfile);
      return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(false, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;

        Grib2Iosp iosp = new Grib2Iosp(gc);
        NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
        NetcdfDataset ncd = new NetcdfDataset(ncfile);
        return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
      }
      return null;
    }
  }

  @Override
  @Nullable
  public CoverageCollection getGridCoverage(Dataset ds, GroupGC group, String filename,
               FeatureCollectionConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    if (filename == null) {
      GribCoverageDataset gribCov = new GribCoverageDataset(this, ds, group);
      return gribCov.createCoverageCollection();

    } else {
      MFile wantFile = findMFileByName(filename);
      if (wantFile != null) {
        GribCollectionImmutable gc = GribCdmIndex.openGribCollectionFromDataFile(false, wantFile, CollectionUpdateType.nocheck, gribConfig, errlog, logger);  // LOOK thread-safety : creating ncx
        if (gc == null) return null;

        GribCoverageDataset gribCov = new GribCoverageDataset(gc, null, null);
        return gribCov.createCoverageCollection();
      }
      return null;
    }
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

    /* http://www.ncl.ucar.edu/Document/Manuals/Ref_Manual/NclFormatSupport.shtml#GRIB
    GRIB2 data variable name encoding

    (Note: examples show intermediate steps in the formation of the name)

    if production status is TIGGE test or operational and matches entry in TIGGE table:
       <parameter_short_name> (ex: t)
    else if entry matching product discipline, parameter category, and parameter number is found:
       <parameter_short_name> (ex: TMP)
    else:
       VAR_<product_discipline_number>_<parameter_category_number>_<parameter_number> (ex: VAR_3_0_9)

    _P<product_definition_template_number> (ex: TMP_P0)

    if single level type:
       _L<level_type_number> (ex: TMP_P0_L103)
    else if two levels of the same type:
       _2L<level_type_number> (ex: TMP_P0_2L106)
    else if two levels of different types:
       _2L<_first_level_type_number>_<second_level_type_number> (ex: LCLD_P0_2L212_213)

    if grid type is supported (fully or partially):
       _G<grid_abbreviation><grid_number> (ex: UGRD_P0_L108_GLC0)
    else:
       _G<grid_number> (ex: UGRD_P0_2L104_G0)

    if not statistically processed variable and not duplicate name the name is complete at this point.

    if statistically-processed variable and constant statistical processing duration:
       if statistical processing type is defined:
          _<statistical_processing_type_abbreviation><statistical_processing_duration><duration_units> (ex: APCP_P8_L1_GLL0_acc3h)
       else
          _<statistical_processing_duration><duration_units> (ex: TMAX_P8_L103_GCA0_6h)
    else if statistically-processed variable and variable-duration processing always begins at initial time:
       _<statistical_processing_type_abbreviation> (ex: ssr_P11_GCA0_acc)

    if variable name is duplicate of existing variable name (this should not normally occur):
       _n (where n begins with 1 for first duplicate) (ex: TMAX_P8_L103_GCA0_6h_1)

     VAR_%d-%d-%d[_error][_L%d][_layer][_I%s_S%d][_D%d][_Prob_%s]
      %d-%d-%d = discipline-category-paramNo
      L = level type
      S = stat type
      D = derived type
   */
  @Override
  protected String makeVariableId(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableId(vindex, this);
  }

  private static String makeVariableId(GribCollectionImmutable.VariableIndex vindex,
      GribCollectionImmutable gc) {
    try (Formatter f = new Formatter()) {

      f.format("VAR_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());

      if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
        f.format("_error");  // its an "error" type variable - add to name
      }

      if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_L%d", vindex.getLevelType()); // code table 4.5
        if (vindex.isLayer()) {
          f.format("_layer");
        }
      }

      String intvName = vindex.getIntvName();
      if (intvName != null && !intvName.isEmpty()) {
        if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS)) {
          f.format("_Imixed");
        } else {
          f.format("_I%s", intvName);
        }
      }

      if (vindex.getIntvType() >= 0) {
        f.format("_S%s", vindex.getIntvType());
      }

      if (vindex.getEnsDerivedType() >= 0) {
        f.format("_D%d", vindex.getEnsDerivedType());
      } else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
        String s = StringUtil2.substitute(vindex.getProbabilityName(), ".", "p");
        f.format("_Prob_%s", s);
      }

      return f.toString();
    }
  }

  @Override
  public void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex) {
    addVariableAttributes(v, vindex, this);
  }

  static void addVariableAttributes(AttributeContainer v, GribCollectionImmutable.VariableIndex vindex, GribCollectionImmutable gc) {
    Grib2Tables cust2 = (Grib2Tables) gc.cust;

    v.addAttribute(new Attribute(Grib.VARIABLE_ID_ATTNAME, gc.makeVariableId(vindex)));
    int[] param = new int[]{vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter()};
    v.addAttribute(new Attribute("Grib2_Parameter", Array.makeFromJavaArray(param, false)));
    String disc = cust2.getCodeTableValue("0.0", vindex.getDiscipline());
    if (disc != null) v.addAttribute(new Attribute("Grib2_Parameter_Discipline", disc));
    String cat = cust2.getCategory(vindex.getDiscipline(), vindex.getCategory());
    if (cat != null)
      v.addAttribute(new Attribute("Grib2_Parameter_Category", cat));
    GribTables.Parameter entry = cust2.getParameter(vindex);
    if (entry != null) v.addAttribute(new Attribute("Grib2_Parameter_Name", entry.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING)
      v.addAttribute(new Attribute("Grib2_Level_Type", vindex.getLevelType()));
    String ldesc = cust2.getLevelName(vindex.getLevelType());
    if (ldesc != null)
      v.addAttribute(new Attribute("Grib2_Level_Desc", ldesc));

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib2_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
      v.addAttribute(new Attribute("Grib2_Probability_Type", vindex.getProbType()));
      v.addAttribute(new Attribute("Grib2_Probability_Name", vindex.getProbabilityName()));
    }

    if (vindex.getGenProcessType() >= 0) {
      String genProcessTypeName = cust2.getGeneratingProcessTypeName(vindex.getGenProcessType());
      if (genProcessTypeName != null)
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", genProcessTypeName));
      else
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", vindex.getGenProcessType()));
    }

    String statType = cust2.getStatisticName(vindex.getIntvType());
    if (statType != null) {
      v.addAttribute(new Attribute("Grib2_Statistical_Process_Type", statType));
    }

  }

}
