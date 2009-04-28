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
package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.net.URI;

/**
 * Validates the contents of a {@link RemoteCatalogRequest} command object.
 *
 * More details in {@link RemoteCatalogServiceController}
 *
 * @author edavis
 * @since 4.0
 * @see RemoteCatalogServiceController
 * @see RemoteCatalogRequest
 */
public class RemoteCatalogRequestValidator implements Validator
{
  public RemoteCatalogRequestValidator() {}

  public boolean supports( Class clazz)
  {
    return RemoteCatalogRequest.class.equals( clazz );
  }

  public void validate( Object obj, Errors e)
  {
    RemoteCatalogRequest rcr = (RemoteCatalogRequest) obj;

    // Validate "catalogUri"
    URI catUri = rcr.getCatalogUri();
    ValidationUtils.rejectIfEmpty( e, "catalogUri", "catalogUri.empty", "No catalog URI given." );

    if ( catUri != null )
    {
      if ( ! catUri.isAbsolute() )
        e.rejectValue( "catalogUri", "catalogUri.notAbsolute",
                       "The \"catalogUri\" field must be an absolute URI." );
      if ( catUri.getScheme() != null
           && ! catUri.getScheme().equalsIgnoreCase( "HTTP" ))
        e.rejectValue( "catalogUri", "catalogUri.notHttpUri",
                       "The \"catalogUri\" field must be an HTTP URI.");
    }

    // Validate "command" - not empty
    ValidationUtils.rejectIfEmpty( e, "command", "command.empty" );

    // When command.equals( SHOW),
    // - validate "htmlView" - not empty
    if ( rcr.getCommand().equals( Command.SHOW ))
    {
      ValidationUtils.rejectIfEmpty( e, "htmlView", "htmlView.empty" );
      if ( ! rcr.isHtmlView() )
        e.rejectValue( "htmlView", "htmlView.falseForRemoteCatalogShow",
                       "A remote catalog is already available as XML." );
    }

    // When command.equals( SUBSET),
    // - validate "dataset" - not empty
    // - validate "htmlView" - not empty
    if ( rcr.getCommand().equals( Command.SUBSET ))
    {
      ValidationUtils.rejectIfEmpty( e, "dataset", "dataset.empty", "No dataset specified in SUBSET request." );
      ValidationUtils.rejectIfEmpty( e, "htmlView", "htmlView.empty" );
    }

    // When command.equals( VALIDATE),
    // - validate "verbose" - not empty
    if ( rcr.getCommand().equals( Command.VALIDATE ) )
      ValidationUtils.rejectIfEmpty( e, "verbose", "verbose.empty" );
  }
}