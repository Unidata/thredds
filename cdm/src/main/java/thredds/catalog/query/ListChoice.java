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

import thredds.catalog.InvDocumentation;
import java.util.*;

/**
 * Implementation of a thredds query choice element.
 *
 * @author john caron
 * @version $Revision$ $Date$
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

/* Change History:
   $Log: ListChoice.java,v $
   Revision 1.2  2004/06/19 00:45:42  caron
   redo nested select list

   Revision 1.1  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.4  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.3  2004/05/11 23:30:29  caron
   release 2.0a

   Revision 1.2  2004/02/20 00:49:52  caron
   1.3 changes

 */