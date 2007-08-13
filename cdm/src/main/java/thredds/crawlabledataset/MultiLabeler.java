// $Id: MultiLabeler.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

import java.util.List;

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

  private List<CrawlableDatasetLabeler> labelerList;

  public MultiLabeler( List<CrawlableDatasetLabeler> labelerList )
  {
    this.labelerList = labelerList;
  }

  public Object getConfigObject() { return null; }
  public List<CrawlableDatasetLabeler> getLabelerList() { return labelerList; }

  public String getLabel( CrawlableDataset dataset )
  {
    String name;
    for ( CrawlableDatasetLabeler curNamer: labelerList )
    {
      name = curNamer.getLabel( dataset );
      if ( name != null ) return name;
    }
    return null;
  }
}
