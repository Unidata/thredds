package thredds.server.catalogservice;

/**
 * Contain the information used from a catalogService request.
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogServiceRequest
{
  private String catalog;
  private boolean verbose;
  private Command command;
  private boolean htmlView;
  private String dataset;

  public CatalogServiceRequest() { }

  public String getCatalog() { return catalog; }
  public void setCatalog( String catalog ) { this.catalog = catalog; }

  public boolean isVerbose() { return verbose; }
  public void setVerbose( boolean verbose ) { this.verbose = verbose; }

  public Command getCommand() { return command; }
  public void setCommand( Command command ) { this.command = command; }

  public boolean isHtmlView() { return this.htmlView; }
  public void setHtmlView( boolean htmlView ) { this.htmlView = htmlView; }

  public String getDataset() { return dataset; }
  public void setDataset( String dataset ) { this.dataset = dataset; }
}
