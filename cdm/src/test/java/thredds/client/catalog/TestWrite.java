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
package thredds.client.catalog;

import org.junit.Assert;
import org.junit.Test;
import thredds.client.catalog.writer.CatalogXmlWriter;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.*;

public class TestWrite {
  static boolean debugCompare = true, debugCompareList = true;

  @Test
  public void testWrite1() throws IOException {
    String filename = "test1.xml";
    Catalog cat = TestClientCatalog.open(filename);
    assert cat != null;

    // create a file and write it out
    File tmpDir = new File(TestDir.temporaryLocalDataDir);
    if (!tmpDir.exists()) {
        boolean ret = tmpDir.mkdirs();
        if (!ret) System.out.println("Error creating directory");
    }

    File tmpFile = new File(tmpDir, filename + ".tmp");
    System.out.println(" output filename= "+tmpFile.getPath());

    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile));
      CatalogXmlWriter writer = new CatalogXmlWriter();
      writer.writeXML(cat, out);
      out.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert false;
    }

    // read it back in
    Catalog catR = TestClientCatalog.open("file:" + tmpFile.getPath());
    assert catR != null;

    compare( cat, catR);
  }

  private void compare(Catalog cat, Catalog catR) {
    List<Dataset> datasets = cat.getDatasets();
    List<Dataset> datasetsR = catR.getDatasets();

    Assert.assertEquals("different number of datasets", datasets.size(), datasetsR.size());

    int n = Math.min(datasets.size(), datasetsR.size());
    for (int i=0; i<n; i++) {
      compareDatasets( datasets.get(i),  datasetsR.get(i));
    }
  }

  private void compareDatasets(Dataset d, Dataset dR) {
    if (debugCompare) System.out.println(" compare datasets ("+d.getName()+") and ("+dR.getName()+")");
    compareList( d.getDocumentation(), dR.getDocumentation());
    compareList( d.getAccess(), dR.getAccess());
    // compareList( d.getMetadataOther(), dR.getMetadataOther());
    compareListVariables( d.getVariables(), dR.getVariables());
    compareListVariables( dR.getVariables(), d.getVariables());

    List<Dataset> datasets = d.getDatasets();
    List<Dataset> datasetsR = dR.getDatasets();

    for (int i=0; i<datasets.size(); i++) {
      compareDatasets( datasets.get(i), datasetsR.get(i));
    }

  }

  private void compareList(List d, List dR) {
    boolean ok = true;
    Iterator iter = d.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = dR.indexOf( item);
      if ( index < 0) {
        System.out.println("   cant find "+item.getClass().getName()+" "+item +" in output ");
        ok = false;
      } else if (debugCompareList) System.out.println("   item ok = ("+item+")");
    }

    iter = dR.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = d.indexOf( item);
      if( index < 0) {
        System.out.println("   cant find "+item.getClass().getName()+" "+item +" in input ");
        ok = false;
      } else if (debugCompareList) System.out.println("   itemR ok = ("+item+")");
    }

    assert ok;
  }

  private void compareListVariables(List<ThreddsMetadata.VariableGroup> d, List<ThreddsMetadata.VariableGroup> dR) {
    boolean ok = true;
    for (ThreddsMetadata.VariableGroup item : d) {
      int index = dR.indexOf(item);
      if (index < 0) {
        System.out.println("   cant find " + item.getClass().getName() + " " + item + " in output ");
        ok = false;
      }
      else if (debugCompareList) {
        ThreddsMetadata.VariableGroup item2 = dR.get(index);
        System.out.println("   Variables ok = (" + item + ") == (" + item2 + ")");
      }
    }

    assert ok;
  }

}
