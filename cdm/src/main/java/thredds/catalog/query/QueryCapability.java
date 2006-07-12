// $Id$
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

import java.util.*;

/**
 * Implementation of a thredds DQC object.
 *
 * @author john caron
 * @version $Revision$ $Date$
 */

public class QueryCapability {

  private String createFrom, name, version;
  private Query query;
  private ArrayList selectors = new ArrayList(); // Selector
  private ArrayList uniqueSelectors = new ArrayList();
  private ArrayList userInterfaces = new ArrayList();
  private Selector ss = null;

  private boolean fatalError = false;
  private StringBuffer errLog = new StringBuffer();

  // package private
  QueryCapability() { }

   /**
    * Construct from fields in XML catalog.
    * @param name
    */
  public QueryCapability( String urlString, String name, String version) {
    this.createFrom = urlString;
    this.name = name;
    this.version = version;
  }

  /**
   * Append an error message to the message log. Call check() to get the log when
   *  everything is done.
   * @param message append this message to log
   * @param fatal true if this is a fatal error.
   */
  public void appendErrorMessage( String message, boolean fatal) {
    errLog.append( message);
    errLog.append( "\n");
    fatalError = fatalError || fatal;
  }

  public String getErrorMessages() { return errLog.toString(); }
  public boolean hasFatalError() { return fatalError; }

  public void addSelector( Selector s) { selectors.add(s); }
  public void setQuery( Query q) { this.query = q; }

  public String getName() { return name; }
  public String getVersion() { return version; }
  public Query getQuery() { return query; }
  public ArrayList getSelectors() { return selectors; }
  public ArrayList getAllUniqueSelectors() { return uniqueSelectors; }

  public String getCreateFrom() { return( this.createFrom ); }

  /** Get ServiceSelector */
  public Selector getServiceSelector() { return ss; }
  /** Set ServiceSelector */
  public void setServiceSelector( Selector ss) { this.ss = ss; }

  public void addUserInterface( Object s) { userInterfaces.add(s); }
  public ArrayList getUserInterfaces( ) { return userInterfaces; }

     /** String representation */
  public String toString() {
    return "name:<"+name+">";
  }

  public void addUniqueSelector( Selector s) {
    if (!uniqueSelectors.contains(s))
      uniqueSelectors.add(s);
    selectors.add( s);
  }

  //////////////////////////////////////////////////////////////////////////////

  /* boolean validate(StringBuffer out, boolean show) {
    boolean isValid = true;
    return isValid;
  } */

   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof QueryCapability)) return false;
     return o.hashCode() == this.hashCode();
  }
  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getQuery().hashCode();
      result = 37*result + getSelectors().hashCode();

      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}

/* Change History:
   $Log: QueryCapability.java,v $
   Revision 1.8  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.7  2004/08/23 16:45:19  edavis
   Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).

   Revision 1.6  2004/06/19 00:45:42  caron
   redo nested select list

   Revision 1.5  2004/06/12 04:12:43  caron
   *** empty log message ***

   Revision 1.4  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.3  2004/05/11 23:30:30  caron
   release 2.0a

   Revision 1.2  2004/02/20 00:49:52  caron
   1.3 changes

 */