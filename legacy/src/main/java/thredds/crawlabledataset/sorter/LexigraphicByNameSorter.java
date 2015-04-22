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
package thredds.crawlabledataset.sorter;

import thredds.crawlabledataset.CrawlableDatasetSorter;
import thredds.crawlabledataset.CrawlableDataset;

import java.util.List;
import java.util.Collections;
import java.util.Comparator;

// ToDo Spell class name correctly (also in datasetScan/sort/lexigraphicByName) - LexicographicByNameSorter.
/**
 * Sort CrawlableDataset lists lexicographically on the dataset name.
 *
 * @author edavis
 * @since Nov 18, 2005 4:26:38 PM
 */
public class LexigraphicByNameSorter implements CrawlableDatasetSorter
{


  private boolean increasingOrder = false;
  private Comparator<CrawlableDataset> comparator;

  public LexigraphicByNameSorter( boolean increasingOrder )
  {
    this.increasingOrder = increasingOrder;
    this.comparator = new java.util.Comparator<CrawlableDataset>()
    {
      public int compare( CrawlableDataset crDs1, CrawlableDataset crDs2 )
      {
        int compareVal = crDs1.getName().compareTo( crDs2.getName() );

        return ( LexigraphicByNameSorter.this.increasingOrder ? compareVal : -compareVal );
      }
    };
  }

  public Object getConfigObject() { return null; }
  
  public boolean isIncreasing() { return increasingOrder; }

  public void sort( List<CrawlableDataset> datasetList )
  {
    Collections.sort( datasetList, this.comparator );
  }
}
