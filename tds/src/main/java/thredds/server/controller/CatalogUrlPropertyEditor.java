package thredds.server.controller;

import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogFactory;

import java.beans.PropertyEditorSupport;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogUrlPropertyEditor extends PropertyEditorSupport
{
  private InvCatalogFactory fac;
  private boolean validate;
  public CatalogUrlPropertyEditor()
  {
    super();
    fac = null;
  }

  public boolean isValidate()
  {
    return validate;
  }

  public void setValidate( boolean validate )
  {
    this.validate = validate;
    this.fac = InvCatalogFactory.getDefaultFactory( true );
  }

  @Override
  public String getAsText()
  {
    InvCatalogImpl cat = (InvCatalogImpl) super.getValue();
    return super.getAsText();
  }

  @Override
  public void setAsText( String text )
          throws IllegalArgumentException
  {
    super.setAsText( text );
  }
}
