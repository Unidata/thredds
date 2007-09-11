package thredds.wcs.v1_1_0;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Represents a WCS 1.1.0 Exception and includes the code, locator, and
 * textMessages used in an OWS Exception Report.
 *
 * @author edavis
 * @since Sep 5, 2007 10:38:17 AM
 */
public class WcsException extends Exception
{
  public enum Code
  {
    OperationNotSupported,
    MissingParameterValue,
    InvalidParameterValue,
    VersionNegotiationFailed, // GetCapabilities only
    InvalidUpdateSequence,
    NoApplicableCode,
    UnsupportedCombination,   // GetCoverage only
    NotEnoughStorage          // GetCoverage only
  }

  private Code code;
  private String locator;
  private List<String> textMessages;

  public WcsException()
  {
    super();
    this.code = Code.NoApplicableCode;
    this.locator = null;
    this.textMessages = Collections.emptyList();
  }

  public WcsException( String message )
  {
    super( message );
    this.code = Code.NoApplicableCode;
    this.locator = null;
    this.textMessages = Collections.singletonList( message);
  }

  public WcsException( String message, Throwable cause )
  {
    super( message, cause );
    this.code = Code.NoApplicableCode;
    this.locator = null;
    this.textMessages = Collections.singletonList( message );
  }

  public WcsException( Throwable cause )
  {
    super( cause );
    this.code = Code.NoApplicableCode;
    this.locator = null;
    this.textMessages = Collections.singletonList( cause.getMessage() );
  }

  public WcsException( Code code, String locator, List<String> messages )
  {
    super( messages.get(0));
    this.code = code;
    this.locator = locator;
    this.textMessages = new ArrayList<String>( messages.size());
    Collections.copy( this.textMessages, messages);
  }

  public WcsException( Code code, String locator, String message )
  {
    super( message);
    this.code = code;
    this.locator = locator;
    this.textMessages = Collections.singletonList( message);
  }

  public Code getCode() { return code; }
  public String getLocator() { return locator; }
  public List<String> getTextMessages() { return Collections.unmodifiableList( textMessages ); }
}
