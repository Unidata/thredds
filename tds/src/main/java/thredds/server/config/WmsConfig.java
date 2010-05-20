package thredds.server.config;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class WmsConfig
{
  private boolean allow;
  private boolean allowRemote;
  private String paletteLocationDir;
  private int maxImageWidth;
  private int maxImageHeight;

  public boolean isAllow() {
    return allow;
  }

  public void setAllow( boolean allow ) {
    this.allow = allow;
  }

  public boolean isAllowRemote() {
    return allowRemote;
  }

  public void setAllowRemote( boolean allowRemote ) {
    this.allowRemote = allowRemote;
  }

  public String getPaletteLocationDir() {
    return paletteLocationDir;
  }

  public void setPaletteLocationDir( String paletteLocationDir ) {
    this.paletteLocationDir = paletteLocationDir;
  }

  public int getMaxImageWidth() {
    return maxImageWidth;
  }

  public void setMaxImageWidth( int maxImageWidth ) {
    this.maxImageWidth = maxImageWidth;
  }

  public int getMaxImageHeight() {
    return maxImageHeight;
  }

  public void setMaxImageHeight( int maxImageHeight ) {
    this.maxImageHeight = maxImageHeight;
  }
}
