/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import org.junit.Test;
import thredds.catalog.parser.jdom.FeatureCollectionReader;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.List;

/**
 * Tests whether a feature collection config that includes gdshash actually
 * correctly remaps variables onto a common grid. This is addressing a problem
 * with the NDFD that silently cropped in and back out.
 *
 * @author rmay
 * @since 11/18/2014
 */
public class TestGribIndexCreation {

  //@Test
  public void testGdsHashChange() throws IOException {
    String dataDir = TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/";
    FeatureCollectionConfig config = FeatureCollectionReader
            .readFeatureCollection(dataDir +
                    "/config.xml#NDFD-CONUS_5km_conduit");
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("test");
    GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always,
            logger);

      // Open the index file
      NetcdfFile f = NetcdfFile.open(dataDir +
              "NDFD_CONUS_5km_conduit_20141114_1300.grib2.ncx2");

      // Check that we have no groups other than the root
      List<Group> groups = f.getRootGroup().getGroups();
      assert groups.size() == 0;

      List<Variable> vars = f.getRootGroup().getVariables();
      assert vars.size() == 35;
  }
}
