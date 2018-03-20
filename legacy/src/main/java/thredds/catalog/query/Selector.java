/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: Selector.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

import thredds.catalog.InvDocumentation;
import ucar.unidata.util.StringUtil2;

import java.util.*;

/**
 * Abstract class for DQC selector elements.
 *
 * @author john caron
 */

public abstract class Selector {
  protected List compound; // List<Selector> : only one is operable
  protected boolean isUsed; //

  protected String title, id, template;
  protected boolean required, multiple;
  protected InvDocumentation desc;

  protected Selector( ) { }

  /**
   * Construct from fields in XML catalog.
   * @param title : human displayable name
   * @param id : unique id
   * @param template : for the query string
   * @param required : true or false
   * @param multiple : true or false
   */
  protected Selector( String title, String id, String template, String required, String multiple )
  {
    this.title = title;
    this.id = id;
    this.template = template;
    this.required = (required == null) || !required.equals("false");
    this.multiple = (multiple != null) && multiple.equals("true");
  }

  public void setDescription( InvDocumentation desc) { this.desc = desc; }
  public InvDocumentation getDescription() { return desc; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getId() { return id; }
  public void setId( String id) { this.id = id; }

  public String getTemplate() { return template; }
  public void setTemplate (String template) { this.template = template; }

  public boolean isRequired() { return required; }
  public void setRequired( String required) {
     this.required = (required == null) || !required.equals("false");
  }

  public boolean isMultiple() { return multiple; }
  public void setMultiple( String multiple) {
    this.multiple = (multiple != null) && multiple.equals("true");
  }
  public String getSelectType() { return multiple ? "multiple" : "single"; }

  public void setCompoundSelectors(List compound) {
    this.compound = compound;
  }
  public boolean isUsed() { return isUsed; }
  public void setUsed (boolean isUsed) {
    this.isUsed = isUsed;
    if (isUsed && compound != null) {
      for (Object aCompound : compound) {
        Selector s = (Selector) aCompound;
        if (s != this)
          s.setUsed(false);
      }
    }
  }



  /////////////////////////////////////////////////////////////////////
  // forming the query string

  /**
   * Create the selector result string, and append.
   * @param sbuff append here
   * @param values list of selected values, each value is a pair (String, Object), where the
   *  String is name of the value, and the Object is the value itself. We use the toString()
   *  method on the object to get its String representation.
   */
  public void appendQuery( StringBuffer sbuff, ArrayList values) {
    if (template != null)
      appendQueryFromTemplate( sbuff, values);
    else
      appendQueryFromParamValue( sbuff, values);
  }

  private void appendQueryFromParamValue( StringBuffer sbuff, ArrayList choices) {
    for (int i=1; i<choices.size(); i+=2) {
      sbuff.append(getId());
      sbuff.append("=");
      sbuff.append(choices.get(i).toString());
      sbuff.append("&");
    }
  }

  private void appendQueryFromTemplate( StringBuffer sbuff, ArrayList choices) {
    StringBuilder templateBuff = new StringBuilder( template);

    for (int i=0; i<choices.size(); i+=2) {
      StringUtil2.substitute(templateBuff, choices.get(i).toString(), choices.get(i + 1).toString());
    }
    sbuff.append( templateBuff.toString());
  }

  /** Instances which have same id are equal.*/
  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof Selector)) return false;
     return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to be consistent with this equals.
   */
  public int hashCode() {
    if (null != getId())
      return getId().hashCode();
    return super.hashCode();
  }

}