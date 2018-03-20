/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: GrADSDataServerDatasetSource.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.config;

import thredds.catalog.InvDataset;
import thredds.catalog.InvCatalog;

import java.util.List;

public class GrADSDataServerDatasetSource extends DatasetSource
{
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GrADSDataServerDatasetSource.class);

  public GrADSDataServerDatasetSource()
  {
    this.type = DatasetSourceType.getType( "GrADSDataServer");
  }

  // @todo Implement the GrADSDataServer type of DatasetSource.

  protected InvDataset createDataset( String datasetLocation, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "GrADSDataServerDatasetSource class not implemented."));
  }

  protected InvCatalog createSkeletonCatalog( String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "GrADSDataServerDatasetSource class not implemented."));
  }

  protected boolean isCollection( InvDataset dataset )
  {
    throw( new UnsupportedOperationException( "GrADSDataServerDatasetSource class not implemented."));
  }

  protected List expandThisLevel( InvDataset collectionDataset, String prefixUrlPath )
  {
    throw( new UnsupportedOperationException( "GrADSDataServerDatasetSource class not implemented."));
  }
}
