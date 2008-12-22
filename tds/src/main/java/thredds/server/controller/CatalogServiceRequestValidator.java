package thredds.server.controller;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.util.StringUtils;

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
    ValidationUtils.rejectIfEmpty( e, "catalog", "catalog.empty" );
    // "debug" "command" "htmlView" "dataset"
//    Object value = e.getFieldValue( "command" );
//    if ( value == null || !StringUtils.hasLength( value.toString() ) )
//    {
//      e.rejectValue( field, errorCode, errorArgs, defaultMessage );
//
//    }
  }
}
