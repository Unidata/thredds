/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

/**
 * Messages to update a collection
 *
 * @author caron
 * @since 12/23/13
 */
public enum CollectionUpdateType {
  always,     // force new index creation, scanning files and directories as needed
  test,       // test if top index is up-to-date, and if collection has changed
  testIndexOnly,   // test only if top index is up-to-date, use it if exists and younger than data file.
  nocheck,    // if index exists, use it, otherwise create it
  never,      // only use existing, fail if doesnt already exist
  last        // LOOK not implemented: need an option to only examine last partition, for very large collection that change only in latest
}
