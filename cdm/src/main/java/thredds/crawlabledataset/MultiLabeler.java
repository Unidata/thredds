// $Id$
package thredds.crawlabledataset;

import java.util.List;
import java.util.Iterator;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 17, 2005 3:54:55 PM
 */
public class MultiLabeler implements CrawlableDatasetLabeler
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( MultiLabeler.class );

  private List labelerList;

  public MultiLabeler( List labelerList )
  {
    this.labelerList = labelerList;
  }

  public Object getConfigObject() { return null; }
  public List getLabelerList() { return labelerList; }

  public String getLabel( CrawlableDataset dataset )
  {
    String name;
    for ( Iterator it = labelerList.iterator(); it.hasNext(); )
    {
      CrawlableDatasetLabeler curNamer = (CrawlableDatasetLabeler) it.next();

      name = curNamer.getLabel( dataset );
      if ( name != null ) return name;
    }
    return null;
  }
}
/*
 * $Log: MultiLabeler.java,v $
 * Revision 1.2  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 *
 */