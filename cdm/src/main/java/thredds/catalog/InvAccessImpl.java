// $Id: InvAccessImpl.java,v 1.9 2004/06/09 00:27:24 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog;

/**
 * Concrete access element.
 * @see InvAccess
 *
 * @author john caron
 * @version $Revision: 1.9 $ $Date: 2004/06/09 00:27:24 $
 */

public class InvAccessImpl extends InvAccess {

  private StringBuffer log = new StringBuffer();

  protected String serviceName;
  private String serviceTypeName, dataFormatName;

  /**
   * Construct from an existing InvService.
   * @param dataset : the containing dataset.
   * @param urlPath : the urlPath.
   * @param service : reference to an existing service.
   */
  public InvAccessImpl( InvDataset dataset, String urlPath, InvService service) {
    this.dataset = dataset;
    this.urlPath = urlPath.trim();
    this.service = service;
  }

  // for Aggregation.FileAccess
  protected InvAccessImpl( InvDataset dataset, String urlPath) {
    this.dataset = dataset;
    this.urlPath = urlPath.trim();
  }

  /**
   * Construct from fields in XML catalog. Either serviceName or typeName must be specified, not both.
   *
   * @param dataset : the containing dataset.
   * @param urlPath : the urlPath.
   * @param serviceName : name of an existing service, or null.
   * @param typeName : ServiceType name or null. If specified, this creates an anonymous
   *   Service (one that is only used by this access element).
   */
  public InvAccessImpl( InvDataset dataset, String urlPath, String serviceName, String typeName,
                        String dataFormatName, double dataSize) {
    this.dataset = dataset;
    this.urlPath = urlPath;
    this.serviceName = serviceName;
    this.serviceTypeName = typeName;
    this.dataFormatName = dataFormatName;
    this.dataSize = dataSize;

    // may define an anonymous service
    if (typeName != null) {
      if (serviceName != null)
        log.append("**InvAccess in ("+dataset.getFullName()+"):cannot declare both service ("+serviceName+")"+
          " and serviceType <"+typeName+">\n");
      else {
        this.service = new InvService("", typeName, "", "", null); // "anonymous" service
        //if ((uri != null) && !uri.isAbsolute())
        //  log.append("  **InvAccess (4) in ("+dataset.getFullName()+"): urlPath must be absolute ("+urlPath+") for anonymous service\n");
      }
    }

  }

  /*** Finish constructing after all elements have been added.
   * @return true if successful.
   **/
  public boolean finish() {
    if (serviceName != null) {
      this.service = dataset.findService(serviceName);
      if (this.service == null)
        log.append("**InvAccess in ("+dataset.getFullName()+"): has unknown service named ("+ serviceName+")\n");
    }

      // check urlPath is ok
    java.net.URI uri = null;
    try {
      uri = new java.net.URI(urlPath);
    } catch (java.net.URISyntaxException e) {
      log.append("**InvAccess in ("+dataset.getFullName()+"):\n"+
        "   urlPath= "+urlPath+")\n  URISyntaxException="+e.getMessage());
    }

    return true;
  }

    /** String representation */
  public String toString() {
    return "service:("+service.getName()+") path:("+urlPath+")";
  }

    /** Get the service name if specified */
  public String getServiceName() { return serviceName; }
    /** Get the service type name if specified */
  public String getServiceType() { return serviceTypeName; }
    /** Get the data format type name if specified */
  public String getDataFormatName() { return dataFormatName; }

  public void setSize( double dataSize) { this.dataSize = dataSize; }

  /** InvAccessImpl elements with same values are equal. */
  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof InvAccessImpl)) return false;
     return o.hashCode() == this.hashCode();
  }
  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (urlPath != null)
        result = 37*result + urlPath.hashCode();
      if (service != null)
        result = 37*result + service.hashCode();
      if (hasDataSize())
        result = 37*result + (int) getDataSize();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

  //////////////////////////////////////////////////////////////////////////////

  boolean check(StringBuffer out, boolean show) {
    boolean isValid = true;

    if (log.length() > 0) {
      isValid = false;
      out.append( log);
    }

    if (getService() == null) {
      out.append("**InvAccess in ("+dataset.getFullName()+"): with urlPath= ("+urlPath+
        ") has no valid service\n");
      isValid = false;
    }

    else if (getStandardUrlName() == null) {
      out.append("**InvAccess in ("+dataset.getFullName()+"): with urlPath= ("+urlPath+
        ") has invalid URL\n");
      isValid = false;
    }

    if (show) System.out.println("   access "+urlPath+" valid = "+isValid);

    return isValid;
  }


}