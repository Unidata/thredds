package thredds.server.catalogservice;

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
  private InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
  
  public CatalogUrlPropertyEditor()
  {
    super();
    fac = null;
  }

  @Override
  public String getAsText()
  {
    InvCatalogImpl cat = (InvCatalogImpl) super.getValue();
    return cat.getBaseURI().toString();
  }

  @Override
  public void setAsText( String text )
          throws IllegalArgumentException
  {
    if ( text == null )
      throw new IllegalArgumentException( "Catalog URL must not be null.");
    InvCatalogImpl cat = this.fac.readXML( text );
    super.setValue( cat );
  }
}
