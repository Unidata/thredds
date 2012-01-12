/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.thredds.server;

import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.crawl.CatalogCrawler;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.thredds.ThreddsDataFactory;

import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 11/2/11
 */
public class TestGrib implements CatalogCrawler.Listener {
  private boolean debug = false;

  protected void scan(String catalogUrl) throws IOException {
    CatalogCrawler crawler = new CatalogCrawler(CatalogCrawler.USE_ALL_DIRECT, false, this);
    long start = System.currentTimeMillis();
    try {
      crawler.crawl(catalogUrl, null, null, null);
    } finally {
      long took = (System.currentTimeMillis() - start);
      if (debug) System.out.format("***Done " + catalogUrl + " took = " + took + " msecs\n");
    }
  }

  @Override
  public void getDataset(InvDataset ds, Object context) {
    System.out.printf(" -----------%n%s%n",ds);
    if (ds.hasAccess()) {
      ThreddsDataFactory fac = new ThreddsDataFactory();
      GridDataset gds = null;
      try {
        ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

        if (dataResult == null) return;
        if (dataResult.fatalError) {
          System.out.printf(" FATAL = %s%n", dataResult.errLog);
          return;
        }
        if (dataResult.featureDataset == null) return;

        gds = (GridDataset) dataResult.featureDataset;
        System.out.printf("OPEN GridDataset%s%n", gds.getLocationURI());
        for (GridDatatype gd : gds.getGrids()) {
          System.out.printf("  %s%n", gd.getName());
        }

      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (gds != null) try {
          gds.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  @Override
  public boolean getCatalogRef(InvCatalogRef dd, Object context) {
    return true;
  }

  public static void main(String arg[]) throws IOException {
    TestGrib test = new TestGrib();
    test.scan("http://localhost:8080/thredds/catalog/cdmUnitTest/formats/grib2/catalog.xml");
  }

}
