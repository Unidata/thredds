/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

package thredds.tdm;

import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionManager;
import ucar.nc2.grib.collection.GribCdmIndex2;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 12/13/13
 */
public class Tdm {

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main(String[] args) throws IOException {
    long start = System.currentTimeMillis();

    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection("C:\\dev\\github\\thredds\\tds\\src\\test\\content\\thredds\\catalogGrib.xml#NDFD-CONUS-5km");
    if (config == null) return;

     /* Formatter errlog = new Formatter();
     Path topPath = Paths.get("B:/ndfd/200901/20090101");
     GribCollection gc = makeGribCollectionIndexOneDirectory(config, CollectionManager.Force.always, topPath, errlog);
     System.out.printf("%s%n", errlog);
     gc.close(); */


    //Path topPath = Paths.get("B:/ndfd/200906");
    // rewriteIndexesPartitionAll(config, topPath);
    //Grib2TimePartition tp = makeTimePartitionIndexOneDirectory(config, CollectionManager.Force.always, topPath);
    //tp.close();

    System.out.printf("name = %s%n", config.name);
    System.out.printf("spec = %s%n", config.spec);

    GribCdmIndex2.rewriteFilePartition(config, CollectionManager.Force.always, CollectionManager.Force.always);
    //rewriteDirectoryCollection(config, topPath, false);

    long took = System.currentTimeMillis() - start;
    System.out.printf("that all took %s msecs%n", took);
  }

}
