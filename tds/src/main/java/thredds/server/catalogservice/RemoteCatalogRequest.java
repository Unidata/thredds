package thredds.server.catalogservice;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class RemoteCatalogRequest
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( LocalCatalogRequest.class );

  private URI catalogUri;
  private Command command;
  private String dataset;
  private boolean verbose;
  private boolean htmlView;

  public URI getCatalogUri() { return catalogUri; }
  public void setCatalogUri( URI catalogUri ) { this.catalogUri = catalogUri; }

  public Command getCommand() { return command; }
  public void setCommand( Command command ) { this.command = command; }

  public String getDataset() { return dataset; }
  public void setDataset( String dataset ) { this.dataset = dataset; }

  public boolean isVerbose() { return verbose; }
  public void setVerbose( boolean verbose ) { this.verbose = verbose; }

  public boolean isHtmlView() { return htmlView; }
  public void setHtmlView( boolean htmlView ) { this.htmlView = htmlView; }
}