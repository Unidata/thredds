package thredds.catalog2.builder;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class BuildException extends Exception
{
  //ToDo private final List<ThreddsBuilder> badBuilders;

  public BuildException()
  {
    super();
  }

  public BuildException( String message)
  {
    super( message);
  }

  public BuildException( String message, Throwable cause)
  {
    super( message, cause);
  }

  public BuildException( Throwable cause)
  {
    super( cause);
  }
}
