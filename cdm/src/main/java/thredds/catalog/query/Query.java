// $Id: Query.java,v 1.7 2004/09/24 03:26:29 caron Exp $
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

package thredds.catalog.query;

import java.net.URI;

/**
 * Implementation of a DQC query element.
 *
 * @author john caron
 * @version $Revision: 1.7 $ $Date: 2004/09/24 03:26:29 $
 */

public class Query {
  private String base;
  private URI uriResolved;
  private String construct;
  private StringBuffer log = new StringBuffer();

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

  boolean validate(StringBuffer out, boolean show) {
    return true;
  }

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

/* Change History:
   $Log: Query.java,v $
   Revision 1.7  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.6  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.5  2004/06/09 00:27:27  caron
   version 2.0a release; cleanup javadoc

   Revision 1.4  2004/05/11 23:30:30  caron
   release 2.0a

   Revision 1.3  2004/02/20 00:49:52  caron
   1.3 changes
 */