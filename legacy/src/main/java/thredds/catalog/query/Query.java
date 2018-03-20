/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Query.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

import java.net.URI;

/**
 * Implementation of a DQC query element.
 *
 * @author john caron
 */

public class Query {
  private String base;
  private URI uriResolved;
  private String construct;

   /**
    * Construct from fields in XML catalog.
    * @param base : base URL of the query
    * @param construct : How to construct the query (not used in 0.3).
    */
  public Query( String base, URI uriResolved, String construct) {
    this.base = base;
    this.uriResolved = uriResolved;
    this.construct = construct;
  }

  public String getBase() { return base; }
  public URI getUriResolved() { return uriResolved; }
  public String getConstruct() { return construct; } // for 0.2
  public String getReturns() { return "catalog"; } // for 0.2

    /** String representation */
  public String toString() {
    return "base="+base+" construct="+construct;
  }

  //////////////////////////////////////////////////////////////////////////////

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof Query)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getBase())
        result = 37*result + getBase().hashCode();
      if (null != getUriResolved())
        result = 37*result + getUriResolved().hashCode();
      if (null != getConstruct())
        result = 37*result + getConstruct().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8
}