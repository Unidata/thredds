package thredds.server.controller;

import java.beans.PropertyEditorSupport;

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

  public static enum Command { SHOW, SUBSET, VALIDATE }
  public static class CommandEditor extends PropertyEditorSupport
  {
    public CommandEditor() { super(); }

    public String getAsText()
    {
      Command c = (Command) super.getValue();
      return c.toString();
    }

    public void setAsText( String text ) throws IllegalArgumentException
    {
      if ( text == null || text.equals( "" ))
      {
        super.setValue( Command.SHOW );
        return;
      }
      Command c = Command.valueOf( text.toUpperCase() );
      super.setValue( c );
    }
  }

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
