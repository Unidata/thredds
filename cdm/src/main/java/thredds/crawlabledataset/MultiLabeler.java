// $Id: MultiLabeler.java 63 2006-07-12 21:50:51Z edavis $
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
