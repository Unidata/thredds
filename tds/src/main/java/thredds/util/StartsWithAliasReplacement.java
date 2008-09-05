package thredds.util;

import org.springframework.util.StringUtils;


/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StartsWithAliasReplacement
        implements PathAliasReplacement
{
  private final String alias;
  private final String replacementString;

  public StartsWithAliasReplacement( String alias, String replacementString )
  {
    this.alias = alias;
    this.replacementString = replacementString;
  }

  public boolean containsPathAlias( String path )
  {
    path = StringUtils.cleanPath( path );
    return path.startsWith( "alias/" );
  }

  public String replacePathAlias( String path )
  {
    path = StringUtils.cleanPath( path );
    return null;
  }
}
