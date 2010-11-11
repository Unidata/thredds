/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.wms;

import org.joda.time.DateTime;
import thredds.server.config.TdsContext;
import thredds.servlet.*;
import uk.ac.rdg.resc.ncwms.controller.ServerConfig;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link uk.ac.rdg.resc.ncwms.controller.ServerConfig} for a THREDDS Data Server.
 * This is injected by Spring into the {@link ThreddsWmsController} to provide
 * access to data and metadata.
 *
 * @author Jon
 */
public class ThreddsServerConfig implements ServerConfig
{
  private org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );

  private TdsContext tdsContext;

  private String defaultPaletteLocation;

  private ThreddsServerConfig() {}

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void setDefaultPaletteLocation( String defaultPaletteDirectory) {
    this.defaultPaletteLocation = defaultPaletteDirectory;
  }

  @Override
  public File getPaletteFilesLocation( ServletContext context )
  {
    if ( this.getUserConfigPaletteLocationDir() != null
         && ! this.getUserConfigPaletteLocationDir().trim().equals("") )
    {
      File userConfigPaletteDir = new File( this.getUserConfigPaletteLocationDir());
      if ( userConfigPaletteDir.isAbsolute())
      {
        if ( userConfigPaletteDir.exists() && userConfigPaletteDir.isDirectory())
          return userConfigPaletteDir;
      }
      else
      {
        userConfigPaletteDir = this.tdsContext.getConfigFileSource().getFile( this.getUserConfigPaletteLocationDir() );
        if ( userConfigPaletteDir != null && userConfigPaletteDir.exists() && userConfigPaletteDir.isDirectory() )
          return userConfigPaletteDir;
      }

      String msg = "User configured palette files location [" + this.getUserConfigPaletteLocationDir()
                   + "] not available. Using default location [" + this.defaultPaletteLocation + "].";
      logServerStartup.warn( msg.toString() );
    }
    else
    {
      String msg = "No user configured palette files location. Using default location [" + this.defaultPaletteLocation + "].";
      logServerStartup.debug( msg.toString() );
    }

    if ( this.defaultPaletteLocation != null && ! this.defaultPaletteLocation.trim().equals( "" ) )
    {
      File defaultPaletteDir = new File( context.getRealPath( this.defaultPaletteLocation ) );
      if ( defaultPaletteDir != null && defaultPaletteDir.exists() && defaultPaletteDir.isDirectory() )
        return defaultPaletteDir;
    }

    this.setAllow( false);
    this.setAllowRemote( false);
    logServerStartup.error( "Default palette location not available.\n**** Disabling WMS - Check palette configuration: "
                            + UsageLog.closingMessageNonRequestContext() );
    return null;
  }

  public boolean isAllow() {
    return this.tdsContext.getWmsConfig().isAllow();
  }

  public void setAllow( boolean allow) {
    this.tdsContext.getWmsConfig().setAllow( allow );
  }

  public boolean isAllowRemote() {
    return this.tdsContext.getWmsConfig().isAllowRemote();
  }

  public void setAllowRemote( boolean allowRemote) {
    this.tdsContext.getWmsConfig().setAllowRemote( allowRemote );
  }

  private String getUserConfigPaletteLocationDir() {
    return this.tdsContext.getWmsConfig().getPaletteLocationDir();
  }

  public TdsContext getTdsContext() { return this.tdsContext; }

  /**
     * Returns the current time.  THREDDS servers don't cache their metadata
     * so the datasets could change at any time.  This effectively means that
     * clients should not cache Capabilities documents from THREDDS servers for
     * any "significant" period of time, to prevent inconsistencies between
     * client and server.
     */
    @Override
    public DateTime getLastUpdateTime() {
        return new DateTime();
    }

    @Override
    public String getTitle() {
      return this.tdsContext.getServerInfo().getName();
    }

    @Override
    public int getMaxImageWidth() {
      return this.tdsContext.getWmsConfig().getMaxImageWidth();
    }

    @Override
    public int getMaxImageHeight() {
      return this.tdsContext.getWmsConfig().getMaxImageHeight();
    }

    @Override
    public String getAbstract() {
      return this.tdsContext.getServerInfo().getSummary();
    }

    @Override
    public Set<String> getKeywords() {
      String[] keysArray = this.tdsContext.getServerInfo().getKeywords().split( ",\\s*" );
      // preserves iteration order
      Set<String> keywords = new LinkedHashSet<String>( keysArray.length );
      keywords.addAll( Arrays.asList( keysArray ) );
      return keywords;
    }

    @Override
    public String getServiceProviderUrl() {
      return this.tdsContext.getServerInfo().getHostInstitutionWebSite();
    }

    @Override
    public String getContactName() {
      return this.tdsContext.getServerInfo().getContactName();
    }

    @Override
    public String getContactOrganization() {
      return this.tdsContext.getServerInfo().getContactOrganization();
    }

    @Override
    public String getContactTelephone() {
      return this.tdsContext.getServerInfo().getContactPhone();
    }

    @Override
    public String getContactEmail() {
      return this.tdsContext.getServerInfo().getContactEmail();
    }
}