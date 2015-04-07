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
package ucar.nc2.ft.cover.collection;

import org.slf4j.Logger;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.cover.CoverageCS;
import ucar.nc2.ft.cover.impl.CoverageCSFactory;
import ucar.nc2.ft.cover.impl.CoverageDatasetImpl;
import ucar.nc2.ft.cover.impl.CoverageIndexWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 3/4/2015
 */
public class GridCollectionBuilder {

  public boolean updateGribCollection(FeatureCollectionConfig config, CollectionUpdateType updateType, Logger logger) throws IOException {

    long start = System.currentTimeMillis();

    List<CoverageDatasetImpl> collection = new ArrayList<>();

    Formatter errlog = new Formatter();
    CollectionSpecParser specp = config.getCollectionSpecParser(errlog);
    System.out.printf("specp=%s%n", specp);
    boolean changed = false;

    try (CollectionAbstract dcm = new CollectionPathMatcher(config, specp, logger)) {
      for (MFile file : dcm.getFilesSorted()) {
        System.out.printf(" %s == %s == ", file.getPath(), dcm.extractDate(file));

        try (NetcdfDataset ds = NetcdfDataset.openDataset(file.getPath())){
          System.out.printf("%s%n", CoverageCSFactory.describe(errlog, ds));
          CoverageDatasetImpl cds = new CoverageDatasetImpl(ds, errlog);
          if (cds.getType() == null) {
            System.out.printf(" **Error classifying: %s%n", errlog);
          } else if (cds.getType() != CoverageCS.Type.Grid) {
            System.out.printf(" **NOT A GRID %s%n", ds.getLocation());
          } else {
            collection.add(cds);
          }
        }
      }
    }

    if (collection.size() > 0) {
      writeIndex(collection.get(0));
    }

    long took = System.currentTimeMillis() - start;
    logger.info("updateGribCollection {} changed {} took {} msecs", config.collectionName, changed, took);
    return changed;
  }

  private void writeIndex(CoverageDatasetImpl cds) throws IOException {
    CoverageIndexWriter writer = new CoverageIndexWriter();
    File idxFile = new File("C:/temp/testCovIndex.ncx3");
    writer.writeIndex("name", idxFile, null, cds);
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
