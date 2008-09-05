package thredds.util;



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
    //path = StringUtils
    return path.startsWith( "alias/" );
    return false;
  }

  public String replacePathAlias( String path )
  {
    return null;
  }
}
