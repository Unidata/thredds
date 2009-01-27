// $Id: Selector.java 48 2006-07-12 16:15:40Z caron $
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
 * Abstract class for DQC selector elements.
 *
 * @author john caron
 */

public abstract class Selector {
  protected ArrayList children = new ArrayList();
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
    this.required = (required == null) ? true : !required.equals("false");
    this.multiple = (multiple == null) ? false : multiple.equals("true");
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
     this.required = (required == null) ? true : !required.equals("false");
  }

  public boolean isMultiple() { return multiple; }
  public void setMultiple( String multiple) {
    this.multiple = (multiple == null) ? false : multiple.equals("true");
  }
  public String getSelectType() { return multiple ? "multiple" : "single"; }

  public void setCompoundSelectors(List compound) {
    this.compound = compound;
  }
  public boolean isUsed() { return isUsed; }
  public void setUsed (boolean isUsed) {
    this.isUsed = isUsed;
    if (isUsed && compound != null) {
      Iterator iter = compound.iterator();
      while (iter.hasNext()) {
        Selector s = (Selector) iter.next();
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
    StringBuffer templateBuff = new StringBuffer( template);

    for (int i=0; i<choices.size(); i+=2) {
      ucar.unidata.util.StringUtil.substitute( templateBuff,
            choices.get(i).toString(), choices.get(i+1).toString());
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