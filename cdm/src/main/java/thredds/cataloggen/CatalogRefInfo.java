// $Id: CatalogRefInfo.java,v 1.2 2005/03/21 23:04:52 edavis Exp $
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

/*
 * $Log: CatalogRefInfo.java,v $
 * Revision 1.2  2005/03/21 23:04:52  edavis
 * Update CatalogGen.main() so that all the catalogs referenced by catalogRefs
 * in the generated catalogs are in turn generated.
 *
 * Revision 1.1  2004/12/29 21:53:21  edavis
 * Added catalogRef generation capability to DatasetSource: 1) a catalogRef
 * is generated for all accepted collection datasets; 2) once a DatasetSource
 * is expanded, information about each catalogRef is available. Added tests
 * for new catalogRef generation capability.
 *
 * Revision 1.1  2004/12/14 22:47:22  edavis
 * Add simple interface to thredds.cataloggen and continue adding catalogRef capabilities.
 *
 * Revision 1.1  2004/11/30 23:58:56  edavis
 * Initial attempt at a way to keep track of a group of catalogs contected by CatalogRefs.
 *
 */