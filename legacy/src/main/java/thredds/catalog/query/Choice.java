/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Choice.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

/**
 * Abstraction of a choice a user can make.
 *
 * @author john caron
 */

public interface Choice {
  public String toString(); // human display
  public String getValue(); // value for the query
}