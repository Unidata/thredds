package thredds.server.controller;

import thredds.catalog.InvCatalogImpl;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceRequest
{
  private InvCatalogImpl catalog;
  private boolean debug;
  private Command command;
  private View view;
  private Identifier identifier;

  public enum Command { SUBSET, VALIDATE }
  public enum View { HTML, XML }

  public static class Identifier
  {
    private String id;
    public String getId() { return id; }
    public void setId( String id) { this.id = id; }
  }

  public CatalogServiceRequest() {}

  public InvCatalogImpl getCatalog()
  {
    return catalog;
  }

  public void setCatalog( InvCatalogImpl catalog )
  {
    this.catalog = catalog;
  }

  public boolean isDebug()
  {
    return debug;
  }

  public void setDebug( boolean debug )
  {
    this.debug = debug;
  }

  public Command getCommand()
  {
    return command;
  }

  public void setCommand( Command command )
  {
    this.command = command;
  }

  public View getView()
  {
    return view;
  }

  public void setView( View view )
  {
    this.view = view;
  }

  public Identifier getDatasetId()
  {
    return identifier;
  }

  public void setDatasetId( Identifier identifier )
  {
    this.identifier = identifier;
  }
}
