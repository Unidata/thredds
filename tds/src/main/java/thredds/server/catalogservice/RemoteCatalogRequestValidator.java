package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.FieldError;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
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
    ValidationUtils.rejectIfEmpty( e, "catalogUri", "catalogUri.empty" );

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
      ValidationUtils.rejectIfEmpty( e, "dataset", "dataset.empty" );
      ValidationUtils.rejectIfEmpty( e, "htmlView", "htmlView.empty" );
    }

    // When command.equals( VALIDATE),
    // - validate "verbose" - not empty
    if ( rcr.getCommand().equals( Command.VALIDATE ) )
      ValidationUtils.rejectIfEmpty( e, "verbose", "verbose.empty" );
  }
}