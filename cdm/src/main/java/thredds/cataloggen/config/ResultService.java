// $Id: ResultService.java,v 1.8 2005/05/31 16:12:10 caron Exp $

package thredds.cataloggen.config;

import thredds.catalog.*;

import java.util.Iterator;

/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 * @author Ethan Davis
 * @version 1.0
 */

public class ResultService extends InvService
{
  //
  private String accessPointHeader = null;

  private boolean isValid = true;
  private StringBuffer log = new StringBuffer();

  /**
   * Constructor
   *
   * @param name
   * @param serviceType
   * @param base
   * @param suffix
   * @param accessPointHeader
   */
  public ResultService( String name, ServiceType serviceType,
                        String base, String suffix,
                        String accessPointHeader)
  {
    super( name, serviceType.toString(), base, suffix, null);
    this.accessPointHeader = accessPointHeader;
  }

  /**
   * Copy constructor.
   * @param service
   */
  protected ResultService( ResultService service)
  {
    this( service, service.getAccessPointHeader());
  }

  public ResultService( InvService service, String accessPointHeader)
  {
    super( service.getName(), service.getServiceType().toString(), service.getBase(),
           service.getSuffix(), service.getDescription());

    for ( Iterator it = service.getProperties().iterator(); it.hasNext(); )
    {
      InvProperty prop = (InvProperty) it.next();
      addProperty( new InvProperty( prop.getName(), prop.getValue())); // LOOK JC changed
    }

    if ( service.getServiceType() == ServiceType.COMPOUND)
    {
      for ( Iterator it = service.getServices().iterator(); it.hasNext(); )
      {
        addService( (InvService) it.next() ); // LOOK JC changed
      }
    }
    this.accessPointHeader = accessPointHeader;

  }

  /**
   * Return a String containing the accessPointHeader attribute text
   *
   * @return String accessPointHeader attribute text
   */
  public String getAccessPointHeader() { return( this.accessPointHeader); }

  public void setAccessPointHeader( String accessPointHeader)
  {
    this.accessPointHeader = accessPointHeader;
  }

  /**
   * Validate this ResultService object. Return true if valid, false if invalid.
   *
   * @param out StringBuffer with validation messages.
   * @return boolean true if valid, false if invalid
   */
  protected boolean validate( StringBuffer out)
  {
    this.isValid = true;

    // If log from construction has content, append to validation output msg.
    if (this.log.length() > 0) {
      out.append( this.log);
    }

    // Check that 'accessPointHeader' attribute is not null.
    if ( this.getAccessPointHeader() == null)
    {
      this.isValid = false;
      out.append( " ** ResultService (1): a null 'accessPointHeader' is invalid.");
    }

    return( this.isValid);
  }

  /** string representation */
  public String toString()
  {
    return( "ResultService[" + super.toString() +
            " accessPointHeader:<" + this.getAccessPointHeader() + ">]");
  }
}
/*
 * $Log: ResultService.java,v $
 * Revision 1.8  2005/05/31 16:12:10  caron
 * delegate directory scanning to CatalogHandler
 *
 * Revision 1.7  2005/04/27 21:34:09  caron
 * cleanup DirectoryScanner, InvDatasetScan
 *
 * Revision 1.6  2005/03/21 23:04:52  edavis
 * Update CatalogGen.main() so that all the catalogs referenced by catalogRefs
 * in the generated catalogs are in turn generated.
 *
 * Revision 1.5  2004/12/15 17:51:03  edavis
 * Changes to clean up ResultService. Changes to add a server title to DirectoryScanner (becomes the title of the top-level dataset).
 *
 * Revision 1.4  2004/06/12 02:03:01  caron
 * invservice can have ddesc
 *
 * Revision 1.3  2004/06/03 20:21:41  edavis
 * Modify for changes to the InvService constructors. Remove main() method
 * which was used for testing.
 *
 * Revision 1.2  2004/05/11 20:38:46  edavis
 * Update for changes to thredds.catalog object model (still InvCat 0.6).
 * Start adding some logging statements.
 *
 * Revision 1.1.1.1  2002/12/11 22:27:54  edavis
 * CatGen into reorged thredds CVS repository.
 *
 */