/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

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
  static public Grib2Collection readFromIndex(String name, RandomAccessFile raf, FeatureCollectionConfig config, org.slf4j.Logger logger) throws IOException {

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
  static public GribCollectionMutable openMutableGCFromIndex(String name, RandomAccessFile raf, FeatureCollectionConfig config, org.slf4j.Logger logger) throws IOException {

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

  protected Grib2Customizer cust; // gets created in readIndex, after center etc is read in

  protected Grib2CollectionBuilderFromIndex(String name, FeatureCollectionConfig config, org.slf4j.Logger logger) {
    super( new GribCollectionMutable(name, null, config, false), logger);  // directory will be set in readFromIndex
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
    this.cust = Grib2Customizer.factory(gc.center, gc.subcenter, gc.master, gc.local, gc.genProcessId);
    return this.cust;
  }

  protected String getLevelNameShort(int levelCode) {
    return cust.getLevelNameShort(levelCode);
  }


  @Override
  protected void readGds(GribCollectionProto.Gds p) {
    byte[] rawGds = p.getGds().toByteArray();
    Grib2SectionGridDefinition gdss = new Grib2SectionGridDefinition(rawGds);
    Grib2Gds gds = gdss.getGDS();
    gc.addHorizCoordSystem(gds.makeHorizCoordSys(), rawGds, gds, -1);
  }

}

