/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.cataloggen.config;

/**
 * Enumeration of CatalogGen DatasetNamer types.
 *
 * @author Ethan Davis
 */
enum DatasetNamerType
{
  REGULAR_EXPRESSION( "RegExp" ),
  DODS_ATTRIBUTE( "DodsAttrib" );

  private String altId;

  private DatasetNamerType( String altId ) {
    this.altId = altId;
  }

  public String toString() {
    return this.altId;
  }

  public static DatasetNamerType getType( String altId )
  {
    if ( altId == null )
      return null;

    for ( DatasetNamerType curType : DatasetNamerType.values() )
    {
      if ( curType.altId.equals( altId ) )
        return curType;
    }
    return null;
  }
}
