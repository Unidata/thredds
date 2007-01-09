// $Id: SelectList.java 48 2006-07-12 16:15:40Z caron $
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
 * Implementation of a DQC list selector element.
 * A SelectList contains a list of Objects of type ListChoice.
 *
 * @author john caron
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