/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: BooleanCatalogRefExpander.java 63 2006-07-12 21:50:51Z edavis $
package thredds.cataloggen.catalogrefexpander;

import thredds.cataloggen.CatalogRefExpander;
import thredds.cataloggen.InvCrawlablePair;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 6, 2005 3:42:55 PM
 */
public class BooleanCatalogRefExpander implements CatalogRefExpander
{


  private boolean expandAll = false;

  public BooleanCatalogRefExpander( boolean expandAll )
  {
    this.expandAll = expandAll;
  }

  public boolean expandCatalogRef( InvCrawlablePair catRefInfo )
  {
    return expandAll;  
  }
}
