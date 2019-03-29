/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib1.Grib1Gds;
import ucar.nc2.grib.grib1.Grib1SectionGridDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Grib1-specific reading of ncx files.
 *
 * @author caron
 * @since 2/20/14
 */
public class Grib1CollectionBuilderFromIndex extends GribCollectionBuilderFromIndex {

  // read in the index, index raf already open; return null on failure
  @Nullable
  static Grib1Collection readFromIndex(String name, RandomAccessFile raf,
      FeatureCollectionConfig config, org.slf4j.Logger logger) {

    Grib1CollectionBuilderFromIndex builder = new Grib1CollectionBuilderFromIndex(name, config, logger);
    if (!builder.readIndex(raf))
      return null;

    if (builder.gc.getFiles().size() == 0) {
      logger.warn("Grib1CollectionBuilderFromIndex {}: has no files, force recreate ", builder.gc.getName());
      return null;
    }

    return new Grib1Collection(builder.gc);
  }

  // read in the index, index raf already open; return null on failure
  @Nullable
  static GribCollectionMutable openMutableGCFromIndex(String name, RandomAccessFile raf, FeatureCollectionConfig config, org.slf4j.Logger logger) {

    Grib1CollectionBuilderFromIndex builder = new Grib1CollectionBuilderFromIndex(name, config, logger);
    if (!builder.readIndex(raf))
      return null;

    if (builder.gc.getFiles().size() == 0) {
      logger.warn("Grib1CollectionBuilderFromIndex {}: has no files, force recreate ", builder.gc.getName());
      return null;
    }

    return builder.gc;
  }

 ////////////////////////////////////////////////////////////////

  protected Grib1Customizer cust; // gets created in readIndex, after center etc is read in

  Grib1CollectionBuilderFromIndex(String name, FeatureCollectionConfig config,
      org.slf4j.Logger logger) {
    super( new GribCollectionMutable(name, null, config, true), config, logger);  // directory will be set in readFromIndex
  }

  protected int getVersion() {
    return Grib1CollectionWriter.version;
  }
  protected int getMinVersion() {
    return Grib1CollectionWriter.minVersion;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  protected String getMagicStart() {
    return Grib1CollectionWriter.MAGIC_START;
  }

  protected GribTables makeCustomizer()  throws IOException {
    Grib1ParamTables ptables = (config.gribConfig.paramTable != null) ? Grib1ParamTables.factory(config.gribConfig.paramTable) :
            Grib1ParamTables.factory(config.gribConfig.paramTablePath, config.gribConfig.lookupTablePath); // so an iosp message must be received before the open()
    this.cust = Grib1Customizer.factory(gc.center, gc.subcenter, gc.version, ptables);
    return cust;
  }

  protected String getLevelNameShort(int levelCode) {
    return cust.getLevelNameShort(levelCode);
  }

  @Override
  protected GribHorizCoordSystem readGds(GribCollectionProto.Gds p) {
    byte[] rawGds = null;
    Grib1Gds gds;
    int predefined = -1;
    if (p.getPredefinedGridDefinition() > 0) {
      predefined = p.getPredefinedGridDefinition();
      gds = ucar.nc2.grib.grib1.Grib1GdsPredefined.factory(gc.center, predefined);
    } else {
      rawGds = p.getGds().toByteArray();
      Grib1SectionGridDefinition gdss = new Grib1SectionGridDefinition(rawGds);
      gds = gdss.getGDS();
    }

    GdsHorizCoordSys hcs = gds.makeHorizCoordSys();
    String hcsName = (hcs == null) ? gds.getClass().getName() :  makeHorizCoordSysName(hcs);

    // check for user defined group names
    String desc = null;
    if (config.gribConfig.gdsNamer != null)
      desc = config.gribConfig.gdsNamer.get(gds.hashCode());
    if (desc == null)
      desc = (hcs == null) ? hcsName : hcs.makeDescription(); // default desc

    return new GribHorizCoordSystem(hcs, rawGds, gds, hcsName, desc, predefined);
  }

}
