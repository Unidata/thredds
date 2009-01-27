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
package thredds.catalog;

import junit.framework.*;
import java.util.*;

/** Test catalog read JUnit framework. */

public class TestCatalogReference extends TestCase {
  private static boolean showValidation = false;

  public TestCatalogReference( String name) {
    super(name);
  }

  String urlString = "testCatref.xml";

  public void testResolve() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    StringBuilder buff = new StringBuilder();
    boolean isValid = cat.check(buff, false);
    System.out.println("catalog <" + cat.getName() + "> " + (isValid ? "is" : "is not") + " valid");
    System.out.println(" validation output=\n" + buff);
    try {
      cat.writeXML(System.out);
    } catch (java.io.IOException ioe) { assert false; }

    assert getCatrefURI( cat.getDatasets(), "catref").equals(TestCatalogAll.makeFilepath("test2.xml")) : getCatrefURI( cat.getDatasets(), "catref");
    assert getCatrefNestedURI( cat, "top", "catref-nested").equals(TestCatalogAll.makeFilepath("test0.xml")) : getCatrefNestedURI( cat, "top", "catref-nested");
  }

  private InvCatalogRef getCatrefNested(InvCatalogImpl cat, String id, String catName) {
    InvDataset ds = cat.findDatasetByID(id);
    assert ds != null;
    return getCatref( ds.getDatasets(), catName);
  }

  private InvCatalogRef getCatref(List list, String name) {
    for (int i=0; i<list.size(); i++) {
      InvDataset elem = (InvDataset) list.get(i);
      // System.out.println("elemname= "+elem.getName());
      if (elem.getName().equals(name)) {
        assert elem instanceof InvCatalogRef;
        InvCatalogRef catref = (InvCatalogRef) elem;
        System.out.println(name+" = "+catref.getXlinkHref()+" == "+catref.getURI());
        return catref;
      }
    }
    return null;
  }

  private String getCatrefURI(List list, String name) {
    InvCatalogRef catref = getCatref( list, name);
    if (catref != null)
      return catref.getURI().toString();
    return null;
  }

  private String getCatrefNestedURI(InvCatalogImpl cat, String id, String catName) {
    return getCatrefNested( cat, id, catName).getURI().toString();
  }


  public void testDeferredRead() {
    InvCatalogImpl cat = TestCatalogAll.open(urlString, true);

    InvCatalogRef catref = getCatref( cat.getDatasets(), "catref");
    assert ( !catref.isRead());

    catref = getCatrefNested( cat, "top", "catref-nested");
    assert ( !catref.isRead());
  }



}
