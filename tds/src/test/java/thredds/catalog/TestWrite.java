package thredds.catalog;

import junit.framework.*;
import java.io.*;
import java.util.*;

/** sanity check: does it read? */

public class TestWrite extends TestCase {
  static boolean debugCompare = true, debugCompareList = true;

  public TestWrite( String name) {
    super(name);
  }

  public void testWrite() {
    // testWrite( "test0.xml");
    testWrite( "test1.xml");
    /* testWrite( "test2.xml");
    testWrite( "catalogDev.xml");
    testWrite( "TestInherit.1.0.xml"); //
    testWrite( "Example1.0rc7.xml"); //
    testWrite( "TestHarvest.xml"); //
    testWrite( "catgen1.0.xml"); //

    testWrite( "TestInherit.0.6.xml"); //
    testWrite( "InvCatalog.0.6.xml");  //
    testWrite( "testMetadata.xml");  // */
  }

  public void testWrite(String filename) {
    InvCatalogImpl cat = (InvCatalogImpl) TestCatalogAll.open( filename, true);

    // create a file and write it out
    //File tmpDir = new File(TestAll.tmpDir);
    //tmpDir.mkdir();
    String fileOutName = TestCatalogAll.dataDir+filename+".tmp";
    System.out.println(" output filename= "+fileOutName);
    try {
      OutputStream out = new BufferedOutputStream( new FileOutputStream( fileOutName));
      cat.writeXML( out);
      out.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert false;
    }

    // read it back in
    String urlR = "file:/"+fileOutName;
    InvCatalogImpl catR = (InvCatalogImpl) TestCatalogAll.openAbsolute( urlR, true);

    if (!cat.equals( catR)) {
      System.out.println("cat = "+cat.hashCode()+" catR= "+catR.hashCode()+" not equals");
      compare( cat, catR);
    }
    compare( cat, catR);
    assert cat.equals( catR);
  }

  public void compare(InvCatalogImpl cat, InvCatalogImpl catR) {
    List datasets = cat.getDatasets();
    List datasetsR = catR.getDatasets();

    for (int i=0; i<datasets.size(); i++) {
      InvDatasetImpl dd = (InvDatasetImpl) datasets.get(i);
      compareDatasets( dd, (InvDatasetImpl) datasetsR.get(i));
    }
  }

  public void compareDatasets(InvDatasetImpl d, InvDatasetImpl dR) {
    if (debugCompare) System.out.println(" compare datasets ("+d+") and ("+dR+")");
    /* compareList( d.getDocumentation(), dR.getDocumentation());
    compareList( d.getAccess(), dR.getAccess()); */
    compareList( d.getMetadata(), dR.getMetadata());
    // compareListVariables( d.getVariables(), dR.getVariables()); */

    List datasets = d.getDatasets();
    List datasetsR = dR.getDatasets();

    for (int i=0; i<datasets.size(); i++) {
      InvDatasetImpl dd = (InvDatasetImpl) datasets.get(i);
      InvDatasetImpl ddR = (InvDatasetImpl) datasetsR.get(i);
      compareDatasets( dd, ddR);
    }
    assert d.equals(dR) : "**("+d.getID()+") not equal ("+dR.getID()+")";
  }


  public void compareList(List d, List dR) {
    Iterator iter = d.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = dR.indexOf( item);
      if( index < 0) System.out.println("   cant find "+item.getClass().getName()+" "+item +" in output ");
      else if (debugCompareList) System.out.println("   item ok = ("+item+")");
    }

    iter = dR.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = d.indexOf( item);
      if( index < 0) System.out.println("   cant find "+item.getClass().getName()+" "+item +" in input ");
      else if (debugCompareList) System.out.println("   itemR ok = ("+item+")");
    }
  }

  public void compareListVariables(List d, List dR) {
    Iterator iter = d.iterator();
    while (iter.hasNext()) {
      ThreddsMetadata.Variables item = (ThreddsMetadata.Variables) iter.next();
      int index = dR.indexOf( item);
      if( index < 0) System.out.println("   cant find "+item.getClass().getName()+" "+item +" in output ");
      else if (debugCompareList) {
        ThreddsMetadata.Variables item2 = (ThreddsMetadata.Variables) dR.get( index);
        System.out.println("   Variables ok = ("+item+") == ("+item2+")");
      }
    }

    iter = dR.iterator();
    while (iter.hasNext()) {
      ThreddsMetadata.Variables item = (ThreddsMetadata.Variables) iter.next();
      int index = d.indexOf( item);
      if( index < 0) System.out.println("   cant find "+item.getClass().getName()+" "+item +" in input ");
      //else if (debugCompareList) System.out.println("   itemR ok = ("+item+")");
    }
  }


  public void check(String url) {
    InvCatalogImpl cat = (InvCatalogImpl) TestCatalogAll.open( url, true);

    // read it back in
    String urlR = url + ".out.xml";
    InvCatalogImpl catR = (InvCatalogImpl) TestCatalogAll.open( urlR, true);

    boolean e = cat.equals( catR);
    System.out.println("cat = "+cat.hashCode()+" catR= "+catR.hashCode()+" equals = "+e);
    assert cat.equals( catR);
  }

  public static void main(String[] args) {
    TestWrite t = new TestWrite("dummy");
    t.testWrite( "testCatgenOrg.xml");

    /*TestAll.dataDir = "C:/data/catalogs/catgen/";
    t.testWrite( "catgen.org.xml");

    InvCatalogImpl catOrg = (InvCatalogImpl) TestAll.open( "catgen.org.xml", true);
    InvCatalogImpl catNew = (InvCatalogImpl) TestAll.open( "catgen.new.xml", true);
    t.compare( catOrg, catNew); */
  }
}
