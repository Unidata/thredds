package thredds.server.controller;

/**
 * Contain the information used from a catalogService request.
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceRequest
{
  private String catalog = null;
  private boolean debug = false;
  private String command = null;
  private boolean htmlView = false;
  private String dataset = null;

  public static enum Command { SHOW, SUBSET, VALIDATE }

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

  public boolean isHtmlView()
  {
    return this.htmlView;
  }

  public void setHtmlView( boolean htmlView )
  {
    this.htmlView = htmlView;
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
