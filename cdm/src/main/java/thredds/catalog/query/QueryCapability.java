// $Id: QueryCapability.java 48 2006-07-12 16:15:40Z caron $
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
 */

public class QueryCapability {

  private String createFrom, name, version;
  private Query query;
  private ArrayList selectors = new ArrayList(); // Selector
  private ArrayList uniqueSelectors = new ArrayList(); // no duplicate ids
  private ArrayList userInterfaces = new ArrayList();
  private Selector ss = null;

  private boolean fatalError = false;
  private StringBuffer errLog = new StringBuffer();

  // package private
  QueryCapability() { }

   /**
    * Construct from fields in XML catalog.
    * @param urlString the DQC document URI
    * @param name name of DQC document
    * @param version version string
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