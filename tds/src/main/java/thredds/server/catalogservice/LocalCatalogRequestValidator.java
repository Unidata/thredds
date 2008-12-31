package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.FieldError;

import java.net.URI;

import thredds.util.StringValidateEncodeUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class LocalCatalogRequestValidator implements Validator
{
  public LocalCatalogRequestValidator() {}

  private boolean htmlView = false;
  public boolean isHtmlView() { return htmlView; }
  public void setHtmlView( boolean htmlView ) { this.htmlView = htmlView; }

  public boolean supports( Class clazz)
  {
    return LocalCatalogRequest.class.equals( clazz );
  }

  public void validate( Object obj, Errors e)
  {
    LocalCatalogRequest rcr = (LocalCatalogRequest) obj;

    // Validate "path"
    String path = rcr.getPath();
    ValidationUtils.rejectIfEmpty( e, "path", "path.empty" );
    // ToDo move this back into a javax.servlet.Filter
    if ( ! StringValidateEncodeUtils.validPath( path ) )
      e.rejectValue( "path", "path.notValidPath",
                     "The \"path\" field must be a valid path." );
    if ( path != null)
    {
      if ( this.htmlView && ! path.endsWith( ".html"))
        e.rejectValue( "path", "path.notHmlRequest",
                       "The \"path\" field must end in \".html\"." );
      else if ( ! this.htmlView && ! path.endsWith( ".xml"))
        e.rejectValue( "path", "path.notXmlRequest",
                       "The \"path\" field must end in \".xml\"." );
    }

    // Validate "command" - not empty
    ValidationUtils.rejectIfEmpty( e, "command", "command.empty" );
    if ( rcr.getCommand().equals( Command.VALIDATE ))
      e.rejectValue( "command", "command.invalidRequest.VALIDATE",
                     "The \"command\" field may not be VALIDATE." );

    // When command.equals( SUBSET),
    // - validate "dataset" - not empty
    if ( rcr.getCommand().equals( Command.SUBSET ))
      ValidationUtils.rejectIfEmpty( e, "dataset", "dataset.empty" );
  }
}