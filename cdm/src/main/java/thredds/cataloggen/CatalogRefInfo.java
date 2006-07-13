// $Id: CatalogRefInfo.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

import thredds.catalog.InvDataset;
import thredds.cataloggen.config.DatasetSource;

/**
 * Information about a CatalogRef that can be used to generate the referenced catalog.
 */
public class CatalogRefInfo
{
  private String title;
  private String fileName;
  private InvDataset accessPointDataset;
  private DatasetSource datasetSource;

  public CatalogRefInfo( String title, String fileName, InvDataset accessPointDataset, DatasetSource dsSource )
  {
    if ( title == null || fileName == null || accessPointDataset == null || dsSource == null)
      throw new IllegalArgumentException( "Null arguments not allowed.");
    this.title = title;
    this.fileName = fileName;
    this.accessPointDataset = accessPointDataset;
    this.datasetSource = dsSource;
  }

  public String getTitle()
  {
    return title;
  }

  public String getFileName()
  {
    return fileName;
  }

  public InvDataset getAccessPointDataset()
  {
    return accessPointDataset;
  }

  public DatasetSource getDatasetSource()
  {
    return datasetSource;
  }
}
