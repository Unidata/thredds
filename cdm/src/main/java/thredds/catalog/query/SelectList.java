// $Id: SelectList.java,v 1.5 2004/08/23 16:45:19 edavis Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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
 * Implementation of a DQC list selector element.
 * A SelectList contains a list of Objects of type ListChoice.
 *
 * @author john caron
 * @version $Revision: 1.5 $ $Date: 2004/08/23 16:45:19 $
 */

public class SelectList extends Selector {
  private ArrayList choices = new ArrayList();
  private SelectList firstNestedSelector = null;

  public SelectList( ) {
    super();
  }

  public SelectList( String label, String id, String template, String required, String multiple) {
    super( label, id, template, required, multiple);
  }

  public void addChoice( ListChoice c) {
    choices.add(c);
    if ((firstNestedSelector == null) && (c.getNestedSelectors().size() > 0))
      firstNestedSelector = (SelectList) c.getNestedSelectors().get(0);
  }

  public boolean hasNestedSelectors() { return firstNestedSelector != null; }
  public SelectList getFirstNestedSelector() { return firstNestedSelector; }

  public ArrayList getChoices() { return choices; }
  public int getSize() { return choices.size(); }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof SelectList)) return false;
    return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (getTitle() != null)
        result = 37*result + getTitle().hashCode();
      if (getId() != null)
        result = 37*result + getId().hashCode();
      if (getTemplate() != null)
        result = 37*result + getTemplate().hashCode();
      if (isRequired()) result++;
      if (isMultiple()) result++;

      result = 37*result + getChoices().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

}

/* Change History:
   $Log: SelectList.java,v $
   Revision 1.5  2004/08/23 16:45:19  edavis
   Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).

   Revision 1.4  2004/06/19 00:45:43  caron
   redo nested select list

   Revision 1.3  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.2  2004/05/21 05:57:32  caron
   release 2.0b

   Revision 1.1  2004/05/11 23:30:30  caron
   release 2.0a
 */