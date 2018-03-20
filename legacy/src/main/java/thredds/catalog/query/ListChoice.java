/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: ListChoice.java 48 2006-07-12 16:15:40Z caron $


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