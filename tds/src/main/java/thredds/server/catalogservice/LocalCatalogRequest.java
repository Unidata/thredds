package thredds.server.catalogservice;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class LocalCatalogRequest
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( LocalCatalogRequest.class );

  private String path;
  private Command command;
  private String dataset;

  public String getPath() { return path; }
  public void setPath( String path ) { this.path = path; }

  public Command getCommand() { return command; }
  public void setCommand( Command command ) { this.command = command; }

  public String getDataset() { return dataset; }
  public void setDataset( String dataset ) { this.dataset = dataset; }
}
