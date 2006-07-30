package thredds.catalog;

import junit.framework.*;
import java.util.*;


public class TestDatasetFmrc extends TestCase {

  public TestDatasetFmrc( String name) {
    super(name);
  }

  public void testRead() {
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
