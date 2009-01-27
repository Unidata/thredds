// $Id: ListChoice.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package thredds.catalog.query;

import thredds.catalog.InvDocumentation;
import java.util.*;

/**
 * Implementation of a thredds query choice element.
 *
 * @author john caron
 */

public class ListChoice implements Choice {
  private Selector parent;
  private String name, value;
  private ArrayList nestedSelectors = new ArrayList();
  private InvDocumentation desc;

   /**
    * Construct from fields in XML catalog.
    * @param parent parent selector
    * @param name choice name - display to user
    * @param value choice value - send to server
    */
  public ListChoice( Selector parent, String name, String value, String description) {
    this.parent = parent;
    this.name = name;
    this.value = value;

    if (description != null) {
      setDescription( new InvDocumentation(null, null, null, null, description));
    }
  }

  public String getName() { return name; }
  public String getValue() { return value; }
  public Selector getParentSelector() { return parent; }
  public String getTemplate() { return parent.getTemplate(); }

  public void addNestedSelector( SelectList s) { nestedSelectors.add(s); }
  public ArrayList getNestedSelectors() { return nestedSelectors; }
  public boolean hasNestedSelectors() { return nestedSelectors.size() > 0; }

  public void setDescription( InvDocumentation desc) { this.desc = desc; }
  public InvDocumentation getDescription() { return desc; }

  public String toString() { return name; }

  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof ListChoice)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37*result + getName().hashCode();
      result = 37*result + getValue().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (getDescription() != null)
        result = 37*result + getDescription().hashCode();
      if (hasNestedSelectors())
        result = 37*result + getNestedSelectors().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8


}