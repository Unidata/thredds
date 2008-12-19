package thredds.server.controller;

import java.beans.PropertyEditorSupport;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceCommandEditor extends PropertyEditorSupport
{
  public CatalogServiceCommandEditor() {}
  

//  @Override
//  public String getAsText()
//  {
//    CatalogServiceRequest.Command command = (CatalogServiceRequest.Command) super.getValue();
//    return command.toString();
//  }
//
//  @Override
//  public void setAsText( String text )
//          throws IllegalArgumentException
//  {
//    CatalogServiceRequest.Command command = null;
//    try
//    {
//      command = CatalogServiceRequest.Command.getCommand( text );
//    }
//    catch ( IllegalArgumentException e )
//    {
//      try
//      {
//        command = CatalogServiceRequest.Command.valueOf( text );
//      }
//      catch ( IllegalArgumentException e1 )
//      {
//        command = null;
//      }
//    }
//    super.setValue( command );
//  }
}
