/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.wcs.v1_1_0;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Represents a WCS 1.1.0 Exception and includes the code, locator, and
 * textMessages used in an OWS Exception Report.
 *
 * @author edavis
 * @since 4.0
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
    this.textMessages = new ArrayList<String>( messages);
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
