/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.tds.idd;

import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateType;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class CatalogValidityTestUtils {
  private CatalogValidityTestUtils() {
  }

  public static Catalog assertCatalogIsAccessibleAndValid(String catalogUrl) throws IOException {
    assertNotNull("Null catalog URL not allowed.", catalogUrl);

    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromLocation(catalogUrl, null);
    assert !builder.hasFatalError();
    assertCatalogIsValid(cat, catalogUrl);

    return cat;
  }

  public static Catalog assertCatalogIsAccessibleValidAndNotExpired(String catalogUrl) throws IOException {
    Catalog catalog = assertCatalogIsAccessibleAndValid(catalogUrl);

    String msg = getMessageIfCatalogIsExpired(catalog, catalogUrl);
    assertNull(msg, msg);

    return catalog;
  }

  public static boolean checkIfCatalogTreeIsAccessibleValidAndNotExpired(String catalogUrl,
                                                                         StringBuilder log,
                                                                         boolean onlyRelativeUrls) throws IOException {
    Catalog catalog = assertCatalogIsAccessibleValidAndNotExpired(catalogUrl);

    boolean ok = true;
    List<CatalogRef> catRefList = findAllCatRefsInDatasetTree(catalog.getDatasets(), log, onlyRelativeUrls);

    for (CatalogRef catRef : catRefList)
      ok &= checkIfCatalogTreeIsAccessibleValidAndNotExpired(catRef.getURI().toString(), log, onlyRelativeUrls);

    return ok;
  }


  private static void assertCatalogIsValid(Catalog catalog, String catalogUrl) {
    assertNotNull("Null catalog URL not allowed.", catalogUrl);
    assertNotNull("Null catalog [" + catalogUrl + "].", catalog);
  }

  private static String getMessageIfCatalogIsExpired(Catalog catalog, String catalogUrl) {
    CalendarDate expiresDateType = catalog.getExpires();
    if (expiresDateType != null) {
      if (expiresDateType.getMillis() < System.currentTimeMillis()) {
        return "Expired catalog [" + catalogUrl + "]: " + expiresDateType + ".";
      }
    }
    return null;
  }

  public static List<CatalogRef> findAllCatRefsInDatasetTree(List<Dataset> datasets, StringBuilder log, boolean onlyRelativeUrls) {
    List<CatalogRef> catRefList = new ArrayList<>();
    for (Dataset curDs : datasets) {
      if (curDs instanceof CatalogRef) {
        CatalogRef catRef = (CatalogRef) curDs;
        String name = catRef.getName();
        String href = catRef.getXlinkHref();
        URI uri;
        try {
          uri = new URI(href);
        } catch (URISyntaxException e) {
          log.append(log.length() > 0 ? "\n" : "")
                  .append("***WARN - CatalogRef [").append(name)
                  .append("] with bad HREF [").append(href).append("] ");
          continue;
        }
        if (onlyRelativeUrls && uri.isAbsolute())
          continue;

        catRefList.add(catRef);
        continue;
      }

      if (curDs.hasNestedDatasets())
        catRefList.addAll(findAllCatRefsInDatasetTree(curDs.getDatasets(), log, onlyRelativeUrls));
    }
    return catRefList;
  }

}
