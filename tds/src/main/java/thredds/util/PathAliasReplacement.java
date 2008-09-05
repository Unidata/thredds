package thredds.util;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface PathAliasReplacement
{
  public boolean containsPathAlias( String path );
  public String replacePathAlias( String path );
}
