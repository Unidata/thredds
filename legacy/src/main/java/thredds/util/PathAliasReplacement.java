/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

/**
 * Alloc macro substitution, and "aliasing" for paths
 *
 * @author edavis
 * @since 4.0
 */
public interface PathAliasReplacement {
  boolean containsPathAlias( String path );
  String replacePathAlias( String path );
  String replaceIfMatch( String path );
}
