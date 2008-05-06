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
