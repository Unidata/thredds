/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: CatalogRefExpander.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen;

/**
 * Indicates whether a particular catalogRef should be converted to a container dataset.
 *
 * @author edavis
 * @since Dec 6, 2005 2:48:11 PM
 */
public interface CatalogRefExpander
{
  public boolean expandCatalogRef( InvCrawlablePair catRefInfo );
}
