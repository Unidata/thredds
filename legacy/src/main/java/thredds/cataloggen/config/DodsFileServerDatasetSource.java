/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.cataloggen.config;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalog;

import java.util.List;

public class DodsFileServerDatasetSource  extends DatasetSource
{
  //private static Log log = LogFactory.getLog( DodsFileServerDatasetSource.class);
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DodsFileServerDatasetSource.class);

  public DodsFileServerDatasetSource()
  {
    this.type = DatasetSourceType.getType( "DodsFileServer");
  }

  // @todo Implement the DodsFileServer type of DatasetSource.
  protected InvDataset createDataset( String datasetLocation, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected InvCatalog createSkeletonCatalog( String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected boolean isCollection( InvDataset dataset )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

  protected List expandThisLevel( InvDataset collectionDataset, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "DodsFileServerDatasetSource class not implemented."));
  }

}
