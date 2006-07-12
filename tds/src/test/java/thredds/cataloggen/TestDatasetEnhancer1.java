// $Id$
package thredds.cataloggen;

import junit.framework.*;
import thredds.catalog.InvDatasetImpl;

/**
 * A description
 *
 * @author edavis
 * @since Jun 15, 2005T12:18:21 PM
 */
public class TestDatasetEnhancer1 extends TestCase
{
  private DatasetEnhancer1 me;

  public TestDatasetEnhancer1( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testAddTimeCoverage()
  {
    String matchPattern = "([0-9][0-9][0-9][0-9])([0-9][0-9])([0-9][0-9])([0-9][0-9])";
    String substitutionPattern = "$1-$2-$3T$4:00:00";
    String duration = "60 hours";

    DatasetEnhancer1.DatasetMetadataAdder adder = new DatasetEnhancer1.AddTimeCoverageModels( matchPattern, substitutionPattern, duration);
    InvDatasetImpl dataset = new InvDatasetImpl( null, "2005061512_NAM.wmo" );
    adder.addMetadata( dataset);

    String expectedDateText = "2005-06-15T12:00:00";
    String dateText = dataset.getTimeCoverage().getStart().getText();
    assertTrue( "Date text <" + dateText + "> not as expected <" + expectedDateText + ">.",
                dateText.equals( expectedDateText ) );
  }

  public void testAddId()
  {
    DatasetEnhancer1.DatasetMetadataAdder adder = new DatasetEnhancer1.AddId( "baseId" );
    InvDatasetImpl dataset = new InvDatasetImpl( null, "2005061512_NAM.wmo" );
    adder.addMetadata( dataset );

    String expectedId = "baseId/2005061512_NAM.wmo";
    String id = dataset.getID();
    assertTrue( "ID <" + id + "> not as expected <" + expectedId + ">.",
                id.equals( expectedId ) );
  }

}

/*
 * $Log: TestDatasetEnhancer1.java,v $
 * Revision 1.2  2006/01/20 02:08:26  caron
 * switch to using slf4j for logging facade
 *
 * Revision 1.1  2005/12/16 23:19:38  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.2  2005/12/06 19:39:21  edavis
 * Last CatalogBuilder/CrawlableDataset changes before start using in InvDatasetScan.
 *
 * Revision 1.1  2005/06/24 22:00:58  edavis
 * Write DatasetEnhancer1 to allow adding metadata to datasets.
 * Implement DatasetEnhancers for adding timeCoverage and for
 * adding ID to datasets. Also fix DatasetFilter so that 1) if
 * no filter is applicable for collection datasets, allow all
 * collection datasets and 2) if no filter is applicable for
 * atomic datasets, allow all atomic datasets.
 *
 */