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
// $Id: DatasetSorter.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.config;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDatasetScan;

/**
 * A description
 *
 * @author edavis
 * @since Apr 20, 2005 17:02:08 PM
 */
public class DatasetSorter
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetSorter.class);

  // Location(s) in catalog heirarchy wher this sorter should be applied.
  // E.g., "NCEP models/Eta 211", "models/eta_211", "*", "models/*/A" -- must be dataset names
  private String targetLevel;
  private boolean increasingOrder = true; // Default order is increasing.
  private String type = "lexigraphic"; // only lex for now
  private String attributeName;

  private java.util.Comparator invDatasetComparator;

  /**
   * Sort datasets into lexigraphic order by dataset name in either increasing
   * (increasingOrder=true) or decreasing (increasingOrder=false) order.
   *
   * @param increasingOrder if true, datasets are sorted into increasing order, otherwise decreasing order.
   */
  public DatasetSorter( boolean increasingOrder )
  {
    this.increasingOrder = increasingOrder;
    this.invDatasetComparator = new java.util.Comparator()
    {
      public int compare( Object obj1, Object obj2 )
      {
        InvDataset ds1 = (InvDataset) obj1;
        InvDataset ds2 = (InvDataset) obj2;

        int compareVal = ds1.getName().compareTo( ds2.getName() );

        return ( DatasetSorter.this.increasingOrder ? compareVal : -compareVal );
      }
    };
  }

  /**
   * Sort datasets according to the given java.util.Comparator.
   *
   * @param invDatasetComparator a java.util.Comparator that compares InvDataset objects.
   */
  public DatasetSorter( java.util.Comparator invDatasetComparator)
  {
    this.invDatasetComparator = invDatasetComparator;
  }

  public void sortDatasets( InvDataset collectionDs )
  {
    this.sortDatasets( collectionDs.getDatasets() );
  }

  public void sortDatasets( java.util.List datasets)
  {
    java.util.Collections.sort( datasets, invDatasetComparator );
  }

  public void sortNestedDatasets( InvDataset collectionDs )
  {
    // Sort grand children datasets.
    InvDataset curDs = null;
    for ( java.util.Iterator dsIter = collectionDs.getDatasets().iterator(); dsIter.hasNext(); )
    {
      curDs = (InvDataset) dsIter.next();
      // Do not dereference catalogRef or datasetScan.
      if ( ! curDs.getClass().equals( InvCatalogRef.class )
           && ! curDs.getClass().equals( InvDatasetScan.class))
      // @todo Move this test into abstract InvDataset.remoteDataset()?? 
      {
        this.sortDatasets( curDs);
      }
    }

    // Sort child datasets.
    logger.debug( "sortDatasets(): sort the datasets contained by dataset ({})", collectionDs.getName() );
    this.sortDatasets( collectionDs.getDatasets());

    return;
  }

}
