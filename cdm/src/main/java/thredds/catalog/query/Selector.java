// $Id: Selector.java,v 1.11 2005/05/19 23:43:45 caron Exp $
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
 * Abstract class for DQC selector elements.
 *
 * @author john caron
 * @version $Revision: 1.11 $ $Date: 2005/05/19 23:43:45 $
 */

public abstract class Selector {
  protected ArrayList children = new ArrayList();

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

/* Change History:
   $Log: Selector.java,v $
   Revision 1.11  2005/05/19 23:43:45  caron
   clean up javadoc

   Revision 1.10  2004/11/07 02:55:09  caron
   no message

   Revision 1.9  2004/09/25 00:09:43  caron
   add images, thredds tab

   Revision 1.8  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.7  2004/08/23 16:45:20  edavis
   Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).

   Revision 1.6  2004/06/19 00:45:43  caron
   redo nested select list

   Revision 1.5  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.4  2004/05/21 05:57:32  caron
   release 2.0b

   Revision 1.3  2004/05/11 23:30:31  caron
   release 2.0a
 */