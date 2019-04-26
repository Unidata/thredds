/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import javax.annotation.Nullable;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.unidata.io.RandomAccessFile;

/**
 * Build a GribCollection object for Grib-2 files. Only from ncx files.
 * No updating, no nuthin.
 * Data file is not opened.
 *
 * @author caron
 * @since 11/9/13
 */
class Grib2CollectionBuilderFromIndex extends GribCollectionBuilderFromIndex {

  // read in the index, index raf already open; return null on failure
  @Nullable
  static Grib2Collection readFromIndex(String name, RandomAccessFile raf,
      FeatureCollectionConfig config, org.slf4j.Logger logger) {

    Grib2CollectionBuilderFromIndex builder = new Grib2CollectionBuilderFromIndex(name, config, logger);
    if (!builder.readIndex(raf))
      return null;

    if (builder.gc.getFiles().size() == 0) {
      logger.warn("Grib2CollectionBuilderFromIndex {}: has no files, force recreate ", builder.gc.getName());
      return null;
    }

    return new Grib2Collection(builder.gc);
  }


  // read in the index, index raf already open; return null on failure
  @Nullable
  static GribCollectionMutable openMutableGCFromIndex(String name, RandomAccessFile raf,
      FeatureCollectionConfig config, org.slf4j.Logger logger) {

    Grib2CollectionBuilderFromIndex builder = new Grib2CollectionBuilderFromIndex(name, config, logger);
    if (!builder.readIndex(raf))
      return null;

    if (builder.gc.getFiles().size() == 0) {
      logger.warn("Grib2CollectionBuilderFromIndex {}: has no files, force recreate ", builder.gc.getName());
      return null;
    }

    return builder.gc;
  }


  ////////////////////////////////////////////////////////////////

  protected Grib2Tables cust; // gets created in readIndex, after center etc is read in

  Grib2CollectionBuilderFromIndex(String name, FeatureCollectionConfig config,
      org.slf4j.Logger logger) {
    super( new GribCollectionMutable(name, null, config, false), config, logger);  // directory will be set in readFromIndex
  }

  protected int getVersion() {
    return Grib2CollectionWriter.version;
  }

  protected int getMinVersion() {
    return Grib2CollectionWriter.minVersion;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // reading

  protected String getMagicStart() {
    return Grib2CollectionWriter.MAGIC_START;
  }

  protected GribTables makeCustomizer() {
    this.cust = Grib2Tables.factory(gc.center, gc.subcenter, gc.master, gc.local, gc.genProcessId);
    return this.cust;
  }

  protected String getLevelNameShort(int levelCode) {
    return cust.getLevelNameShort(levelCode);
  }

  @Override
  protected GribHorizCoordSystem readGds(GribCollectionProto.Gds p) {
    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    GdsHorizCoordSys hcs = gds.makeHorizCoordSys();

    String hcsName = makeHorizCoordSysName(hcs);

    // check for user defined group names
    String desc = null;
    if (config.gribConfig.gdsNamer != null)
      desc = config.gribConfig.gdsNamer.get(gds.hashCode());
    if (desc == null) desc = hcs.makeDescription(); // default desc

    return new GribHorizCoordSystem(hcs, rawGds, gds, hcsName, desc, -1);
  }

}

