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
  private String catalog = null;
  private boolean debug = false;
  private String command = null;
  private String view = null;
  private String dataset = null;

//  public enum Command
//  {
//    SUBSET(), VALIDATE();
//    private String altName;
//    Command() { this.altName = this.name().toLowerCase(); }
//    public String toString() { return this.altName; }
//
//    public static Command getCommand( String command )
//    {
//      for ( Command curSection : Command.values() )
//        if ( curSection.altName.equals(  command ) )
//          return curSection;
//      throw new IllegalArgumentException( "No such instance [" + command + "]." );
//    }
//  }
//  public enum View { HTML, XML }
//
//  public static class Identifier
//  {
//    private String id;
//    public String getId() { return id; }
//    public void setId( String id) { this.id = id; }
//  }

  public CatalogServiceRequest() { }

  public String getCatalog()
  {
    return catalog;
  }

  public void setCatalog( String catalog )
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

  public String getCommand()
  {
    return command;
  }

  public void setCommand( String command )
  {
    this.command = command;
  }

  public String getView()
  {
    return view;
  }

  public void setView( String view )
  {
    this.view = view;
  }

  public String getDataset()
  {
    return dataset;
  }

  public void setDataset( String dataset )
  {
    this.dataset = dataset;
  }
}
