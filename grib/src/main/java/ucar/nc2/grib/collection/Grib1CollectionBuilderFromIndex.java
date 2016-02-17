/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.collection;

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
 * Grib1 specific reading ncx2 Index file
 *
 * @author caron
 * @since 2/20/14
 */
public class Grib1CollectionBuilderFromIndex extends GribCollectionBuilderFromIndex {

  // read in the index, index raf already open; return null on failure
  static public Grib1Collection readFromIndex(String name, RandomAccessFile raf, FeatureCollectionConfig config, org.slf4j.Logger logger) throws IOException {

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
  static GribCollectionMutable openMutableGCFromIndex(String name, RandomAccessFile raf, FeatureCollectionConfig config, org.slf4j.Logger logger) throws IOException {

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

  protected Grib1CollectionBuilderFromIndex(String name, FeatureCollectionConfig config, org.slf4j.Logger logger) {
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
