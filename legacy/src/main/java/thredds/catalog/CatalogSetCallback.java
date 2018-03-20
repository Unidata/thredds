/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

/**
 * Allows asynchronous reading of a catalog.
 * When the catalog is read, setCatalog() is called, else failed() is called.
 */
public interface CatalogSetCallback {

  /**
   * Called when the catalog is done being read.
   * @param catalog the catalog that was just read in.
   */
  public void setCatalog(InvCatalogImpl catalog);

  /**
   * Called if the catalog reading fails
   */
  public void failed();
}