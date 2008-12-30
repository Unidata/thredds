package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceRequestValidator implements Validator
{
  public CatalogServiceRequestValidator() {}

  public boolean supports( Class clazz)
  {
    return CatalogServiceRequest.class.equals( clazz );
  }

  public void validate( Object obj, Errors e)
  {
    CatalogServiceRequest csr = (CatalogServiceRequest) obj;
    ValidationUtils.rejectIfEmpty( e, "catalog", "catalog.empty" );
    if ( csr.getCommand() == null )
      e.rejectValue( "command", "command.epmty" );
    if ( csr.getCommand().equals( CatalogServiceRequest.Command.SUBSET ))
      ValidationUtils.rejectIfEmpty( e, "dataset", "dataset.empty" );
  }
}
