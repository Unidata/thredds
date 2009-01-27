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

/** sanity check: does it read? */

public class TestRead extends TestCase {

  public TestRead( String name) {
    super(name);
  }

  public void testRead() {
    //testRead( TestCatalogAll.open( "InvCatalog.0.6.xml", true));
    //testRead( TestCatalogAll.open( "enhancedCat.xml", false));
    //testRead( TestCatalogAll.open( "InvCatalogBadDTD.xml", true));
    //testRead( TestCatalogAll.open( "TestAlias.xml", true));
    testRead( TestCatalogAll.open( "DatasetFmrc.xml", true));
  }

  public void testRead(InvCatalog cat) {
    assert cat.getDataset() instanceof InvDatasetImpl;

    Iterator datasets = cat.getDatasets().iterator();
    while (datasets.hasNext()) {
      InvDatasetImpl dd = (InvDatasetImpl) datasets.next();
      testDatasets( dd);
    }
  }

  public void testDatasets(InvDatasetImpl d) {
    testAccess( d);
    testProperty( d);
    testDocs( d);
    testMetadata( d);
    testContributors( d);
    testKeywords( d);
    testProjects( d);
    testPublishers( d);
    testVariables( d);

    Iterator datasets = d.getDatasets().iterator();
    while (datasets.hasNext()) {
      InvDatasetImpl dd = (InvDatasetImpl) datasets.next();
      testDatasets( dd);
    }

  }

   public void testAccess(InvDatasetImpl d) {
    Iterator access = d.getAccess().iterator();
    while (access.hasNext()) {
      InvAccessImpl a = (InvAccessImpl) access.next();
      assert a.getService() != null;
      assert a.getUrlPath() != null;
      assert a.getDataset().equals(d);
      testService( a.getService());
    }
  }

   public void testProperty(InvDatasetImpl d) {
    try {

      Iterator iter = d.getProperties().iterator();
      while (iter.hasNext()) {
        InvProperty a = (InvProperty) iter.next();
      }

    } catch (Throwable t) {
      t.printStackTrace();
      assert false;
    }
  }

   public void testDocs(InvDatasetImpl d) {
    Iterator iter = d.getDocumentation().iterator();
    while (iter.hasNext()) {
      InvDocumentation a = (InvDocumentation) iter.next();
    }
  }


  public void testService(InvService s) {
      List l = s.getServices();
      if (s.getServiceType() == ServiceType.COMPOUND)
        assert l.size() > 0;
      else
        assert l.size() == 0;
  }


  public void testMetadata(InvDatasetImpl d) {
    Iterator mdata = d.getMetadata().iterator();
    while (mdata.hasNext()) {
      InvMetadata m = (InvMetadata) mdata.next();
    }
  }

  public void testContributors(InvDatasetImpl d) {
    Iterator data = d.getContributors().iterator();
    while (data.hasNext()) {
      ThreddsMetadata.Contributor m = (ThreddsMetadata.Contributor) data.next();
    }
  }

  public void testKeywords(InvDatasetImpl d) {
    Iterator data = d.getKeywords().iterator();
    while (data.hasNext()) {
      ThreddsMetadata.Vocab m = (ThreddsMetadata.Vocab) data.next();
    }
  }

  public void testProjects(InvDatasetImpl d) {
    Iterator data = d.getProjects().iterator();
    while (data.hasNext()) {
      ThreddsMetadata.Vocab m = (ThreddsMetadata.Vocab) data.next();
    }
  }

  public void testPublishers(InvDatasetImpl d) {
    Iterator data = d.getPublishers().iterator();
    while (data.hasNext()) {
      ThreddsMetadata.Source m = (ThreddsMetadata.Source) data.next();
    }
  }

  public void testVariables(InvDatasetImpl d) {
    Iterator data = d.getVariables().iterator();
    while (data.hasNext()) {
      ThreddsMetadata.Variables m = (ThreddsMetadata.Variables) data.next();
    }
  }


}
