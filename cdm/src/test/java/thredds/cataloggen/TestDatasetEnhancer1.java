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
// $Id: TestDatasetEnhancer1.java,v 1.2 2006/01/20 02:08:26 caron Exp $
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