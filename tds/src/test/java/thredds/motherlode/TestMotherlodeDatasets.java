/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.motherlode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.ThreddsMetadata;
import thredds.client.catalog.writer.CatalogCrawler;
import thredds.client.catalog.writer.DataFactory;
import ucar.httpservices.HTTPSession;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Run through the named catalogs, open a random dataset from each collection
 */
@RunWith(Parameterized.class)
@Category(NeedsExternalResource.class)
public class TestMotherlodeDatasets implements CatalogCrawler.Listener {
  public static String server = "http://"+TestDir.threddsTestServer+"/thredds";

  @Parameterized.Parameters(name="{0}")
 	public static Collection<Object[]> getTestParameters() {
 		return Arrays.asList(new Object[][]{
            {"/catalog/grib/NCEP/WW3/Global/catalog.xml", CatalogCrawler.Type.random_direct, false},
           // {"/idd/modelsFnmoc.xml", CatalogCrawler.Type.random_direct, false},
           // {"/modelsHrrr.xml", CatalogCrawler.Type.random_direct, false},
           // {"/testDatasets.xml", CatalogCrawler.Type.random_direct, false},
    });
 	}

  private String catUrl;
  private CatalogCrawler.Type type;
  private boolean skipDatasetScan = false;

  private DataFactory tdataFactory = new DataFactory();

  private PrintWriter out;
  private int countDatasets, countNoAccess, countNoOpen;
  private boolean verbose = true;
  private boolean compareCdm = false;
  private boolean checkUnknown = false;
  private boolean checkGroups = true;

  @Before
  public void init() throws IOException {
      DataFactory.setPreferCdm(true);
      HTTPSession.setGlobalUserAgent("TestMotherlodeDatasets");
  }

  public TestMotherlodeDatasets(String catURL, CatalogCrawler.Type type, boolean skipDatasetScan) throws IOException {
    this.catUrl = server + catURL;
    this.type = type;
    this.skipDatasetScan = skipDatasetScan;
    this.out = new PrintWriter( System.out);
  }

  private class FilterDataset implements CatalogCrawler.Filter {
    @Override
    public boolean skipAll(Dataset ds) {
      return skipDatasetScan && (ds instanceof CatalogRef) && ds.findProperty("DatasetScan") != null;
    }
  }

  @Test
  public void crawl() throws IOException {
    out.println("Read " + catUrl);
    countDatasets = 0;
    countNoAccess = 0;
    countNoOpen = 0;
    int countCatRefs = 0;
    CatalogCrawler crawler = new CatalogCrawler(type, 0, new FilterDataset(), this);
    long start = System.currentTimeMillis();
    try {
      countCatRefs = crawler.crawl(catUrl, null, verbose ? out : null, null);

    } finally {
      int took = (int) (System.currentTimeMillis() - start) / 1000;

      out.println("***Done " + catUrl + " took = " + took + " secs\n" +
              "   datasets=" + countDatasets + " no access=" + countNoAccess + " open failed=" + countNoOpen + " total catalogs=" + countCatRefs);
      out.close();
    }

    assert countNoOpen == 0;
    assert crawler.getNumReadFailures() == 0 :
            String.format("Failed to read %s catalogs.", crawler.getNumReadFailures());
  }

  @Override
  public void getDataset(Dataset ds, Object context) {
    countDatasets++;

    // Formatter log = new Formatter();
    try {
      DataFactory.Result threddsData = tdataFactory.openFeatureDataset(ds, null);

    // try (NetcdfDataset ncd = tdataFactory.openDataset(ds, false, null, log)) {

      if (threddsData.fatalError) {
        out.printf("**** failed= %s err=%s%n", ds.getName(), threddsData.errLog);
        countNoAccess++;

      } else if (threddsData.featureType == FeatureType.GRID) {
        GridDataset gds = (GridDataset) threddsData.featureDataset;
        java.util.List<GridDatatype> grids =  gds.getGrids();
        int n = grids.size();
        if (n == 0)
          out.printf("  # Grids == 0 id = %s%n", ds.getId());
        else if (verbose)
          out.printf("     OK ngrids=%d %s%n", n, gds.getLocationURI());

        ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
        if (gc == null)
          out.printf("   GeospatialCoverage NULL id = %s%n", ds.getId());

        if (compareCdm)
          compareCdm(ds, gds.getNetcdfDataset());

        if (checkUnknown) {
          for (GridDatatype vs : grids) {
            if (vs.getDescription().contains("Unknown"))
              out.printf("  %s == %s%n", vs.getFullName(), vs.getDescription());
          }
        }

        if (checkGroups) {
          NetcdfFile nc = gds.getNetcdfFile();
          Group root = nc.getRootGroup();
          if (root.getGroups().size() > 0) {
             out.printf("  GROUPS in %s%n", gds.getLocation());
            for (Group g : root.getGroups()) System.out.printf("%s%n", g.getShortName());
          }
        }
      }

    } catch (Exception e) {
      out.println("**** failed= " + ds.getName());
      e.printStackTrace();
      
      countNoOpen++;
    }

  }

  private void compareCdm(Dataset ds, NetcdfDataset dods) {
    NetcdfDataset cdm = null;
    try {
      DataFactory.setPreferCdm(true);
      Formatter log = new Formatter();
      cdm = tdataFactory.openDataset(ds, false, null, log);

      if (cdm == null) {
        out.println("**** failed= " + ds.getName() + " err=" + log);
        countNoAccess++;

      } else {
         // compareFiles(NetcdfFile org, NetcdfFile copy, boolean _compareData, boolean _showCompare, boolean _showEach)
        cdm.enhance();
        CompareNetcdf2.compareFiles(dods, cdm,  new Formatter(System.out), false, false, false);
      }

    } catch (Exception e) {
      out.println("**** failed to open cdm dataset = " + ds.getName());
      e.printStackTrace();

      countNoOpen++;
    } finally {
      DataFactory.setPreferCdm(false);
      if (cdm != null) try {
        cdm.close();
      } catch (IOException e) {
      }
    }
  }

  public boolean getCatalogRef(CatalogRef dd, Object context) {
    return true;
  }

}
