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
package thredds.client.catalog;

import org.junit.Assert;
import org.junit.Test;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.List;

/**
 * Unit tests for client catalogs
 *
 * @author caron
 * @since 1/16/2015
 */
public class TestClientCatalog {
  
  static public String makeUrlFromFragment(String catFrag) {
    return "file:" + TestDir.cdmLocalTestDataDir + "thredds/catalog/" + catFrag;    
  }
  
 static public Catalog open(String urlString) throws IOException {
   if (!urlString.startsWith("file:")) urlString = makeUrlFromFragment(urlString);
    System.out.printf("Open %s%n", urlString);
    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromLocation(urlString);
    if (builder.hasFatalError()) {
      System.out.printf("ERRORS %s%n", builder.getErrorMessage());
      assert false;
      return null;
    } else {
      String mess = builder.getErrorMessage();
      if (mess.length() > 0)
        System.out.printf(" parse Messages = %s%n", builder.getErrorMessage());
    }
   return cat;
  }

  public static String makeFilepath(String catalogName) {
    return makeFilepath() + catalogName;
  }

  public static String makeFilepath() {
    return "file:"+dataDir;
  }

  public static String dataDir = TestDir.cdmLocalTestDataDir + "thredds/catalog/";

  /////////////////////////////////
  
  private final String urlString = "testCatref.xml";

  @Test
  public void testResolve() throws IOException{
    Catalog cat = open(urlString);
    Assert.assertEquals("catrefURI", makeFilepath("test2.xml"), getCatrefURI( cat.getDatasets(), "catref"));

    String catrefURIn = getCatrefNestedURI(cat, "top", "catref-nested");
    assert catrefURIn.equals(makeFilepath("test0.xml")) :catrefURIn;
  }

  private CatalogRef getCatrefNested(Catalog cat, String id, String catName) {
    Dataset ds = cat.findDatasetByID(id);
    assert ds != null;
    return getCatref( ds.getDatasets(), catName);
  }

  private CatalogRef getCatref(List<Dataset> list, String name) {
    for (Dataset ds : list) {
      if (ds.getName().equals(name)) {
        assert ds instanceof CatalogRef;
        CatalogRef catref = (CatalogRef) ds;
        System.out.println(name+" = "+catref.getXlinkHref()+" == "+catref.getURI());
        return catref;
      }
    }
    return null;
  }

  private String getCatrefURI(List<Dataset> list, String name) {
    CatalogRef catref = getCatref( list, name);
    if (catref != null)
      return catref.getURI().toString();
    return null;
  }

  private String getCatrefNestedURI(Catalog cat, String id, String catName) {
    return getCatrefNested( cat, id, catName).getURI().toString();
  }


  @Test
  public void testDeferredRead() throws IOException {
    Catalog cat = open(urlString);

    CatalogRef catref = getCatref( cat.getDatasets(), "catref");
    assert ( !catref.isRead());

    catref = getCatrefNested( cat, "top", "catref-nested");
    assert ( !catref.isRead());
  }

  ////////////////////////

  @Test
  public void testNested() throws IOException {
    Catalog cat = open("nestedServices.xml");

    Dataset ds = cat.findDatasetByID("top");
    assert ds != null;
    assert ds.getServiceDefault() != null : ds.getID();

    ds = cat.findDatasetByID("nest1");
    assert ds != null;
    assert ds.getServiceDefault() != null  : ds.getID();

    ds = cat.findDatasetByID("nest2");
    assert ds != null;
    assert ds.getServiceDefault() != null  : ds.getID();


    System.out.printf("OK%n");
 }

  ////////////////////////////


}
