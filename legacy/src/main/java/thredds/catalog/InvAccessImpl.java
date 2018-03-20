/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

/**
 * Concrete access element.
 * @see InvAccess
 *
 * @author john caron
 */

public class InvAccessImpl extends InvAccess {

  private StringBuilder log = new StringBuilder();

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
   * @param dataFormatName  optional
   * @param dataSize optional
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
        log.append("**InvAccess in (").append(dataset.getFullName()).append("):cannot declare both service (").append(serviceName).append(")" + " and serviceType <").append(typeName).append(">\n");
      else {
        this.service = new InvService("", typeName, "", "", null); // "anonymous" service
        //if ((url != null) && !url.isAbsolute())
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
        log.append("**InvAccess in (").append(dataset.getFullName()).append("): has unknown service named (").append(serviceName).append(")\n");
    }

      // check urlPath is ok
    try {
      new java.net.URI(urlPath);
    } catch (java.net.URISyntaxException e) {
      log.append("**InvAccess in (").append(dataset.getFullName()).append("):\n" + "   urlPath= ").append(urlPath).append(")\n  URISyntaxException=").append(e.getMessage());
    }

    return true;
  }

    /** String representation */
  public String toString() {
    return "service:("+service.getName()+") path:("+urlPath+")";
  }

    /** @return the service name if specified */
  public String getServiceName() { return serviceName; }
    /** @return the service type name if specified */
  public String getServiceType() { return serviceTypeName; }
    /** @return the data format type name if specified */
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

  boolean check(StringBuilder out, boolean show) {
    boolean isValid = true;

    if (log.length() > 0) {
      isValid = false;
      out.append( log);
    }

    if (getService() == null) {
      out.append("**InvAccess in (").append(dataset.getFullName()).append("): with urlPath= (").append(urlPath).append(") has no valid service\n");
      isValid = false;
    }

    else if (getStandardUrlName() == null) {
      out.append("**InvAccess in (").append(dataset.getFullName()).append("): with urlPath= (").append(urlPath).append(") has invalid URL\n");
      isValid = false;
    }

    if (show) System.out.println("   access "+urlPath+" valid = "+isValid);

    return isValid;
  }


}