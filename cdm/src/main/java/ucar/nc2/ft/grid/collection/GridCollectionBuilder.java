/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.grid.collection;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.inventory.partition.DirectoryPartition;
import thredds.inventory.partition.TimePartition;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.grid.impl.CoverageCSFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;

/**
 * Describe
 *
 * @author caron
 * @since 3/4/2015
 */
public class GridCollectionBuilder {

  public boolean updateGribCollection(FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    long start = System.currentTimeMillis();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = new CollectionSpecParser(config.spec, errlog);
    Path rootPath = Paths.get(specp.getRootDir());

    boolean changed = false;

    if (config.ptype == FeatureCollectionConfig.PartitionType.none) {

      try (CollectionAbstract dcm = new CollectionPathMatcher(config, specp, logger)) {
        for (MFile file : dcm.getFilesSorted()) {
          System.out.printf("%s == %s == ", file.getPath(), dcm.extractDate(file));

          try (NetcdfDataset ds = NetcdfDataset.openDataset(file.getPath())){
            System.out.printf("%s%n", CoverageCSFactory.describe(errlog, ds));
          }
        }
      }

    } /* else if (config.ptype == FeatureCollectionConfig.PartitionType.timePeriod) {

      try (TimePartition tp = new TimePartition(config, specp, logger)) {
        changed = updateTimePartition(isGrib1, tp, updateType, logger);
      }

    } /* else {

      // LOOK assume wantSubdirs makes it into a Partition. Isnt there something better ??
      if (specp.wantSubdirs()) {  // its a partition

        try (DirectoryPartition dpart = new DirectoryPartition(config, rootPath, true, new GribCdmIndex(logger), logger)) {
          dpart.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, config);
          changed = updateDirectoryCollectionRecurse(isGrib1, dpart, config, updateType, logger);
        }

      } else { // otherwise its a leaf directory
        changed = updateLeafCollection(isGrib1, config, updateType, true, logger, rootPath);
      }
    } */

    long took = System.currentTimeMillis() - start;
    logger.info("updateGribCollection {} changed {} took {} msecs", config.collectionName, changed, took);
    return changed;
  }

  public static void main(String[] args) throws IOException {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");

    FeatureCollectionConfig config = new FeatureCollectionConfig("RFC", "grid/CM2", FeatureCollectionType.GRID,
            "B:/CM2.1R/**/.*nc$", null, "#atmos.#yyyyMM", null, "none", null);

    GridCollectionBuilder builder = new GridCollectionBuilder();
    boolean changed = builder.updateGribCollection(config, CollectionUpdateType.always, logger);
    System.out.printf("changed = %s%n", changed);
  }


}
