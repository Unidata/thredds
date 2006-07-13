// $Id: SelectService.java 48 2006-07-12 16:15:40Z caron $
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
 * Implementation of a DQC service selector element.
 * A SelectService contains a list of Objects of type ServiceChoice.
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */

public class SelectService extends Selector {
  private ArrayList choices = new ArrayList();

   /**
    * Construct from fields in XML catalog.
    * @see Selector
    */
  public SelectService(String id, String title) {
    setId( id);
    setTitle( title);
  }


  public void addServiceChoice( String service, String title, String dataFormat) {
    choices.add( new ServiceChoice(service, title, dataFormat));
  }
  public ArrayList getChoices() { return choices; }
  public int getSize() { return choices.size(); }

  /** SelectService with same values are equal */
  public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof SelectService)) return false;
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
      //result = 37*result + getServiceChoices().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0;

  public class ServiceChoice implements Choice {
    private String service, title, dataFormat;
    ServiceChoice( String service, String title, String dataFormat) {
      this.service = service;
      this.title = title;
      this.dataFormat = dataFormat;
    }
    public String getService() { return service; }
    public String getTitle() { return title; }
    public String getDataFormat() { return dataFormat; }

    // Choice
    public String toString() { return title != null ? title : service; }
    public String getValue() { return service; }

    /** SelectService with same values are equal */
    public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof ServiceChoice)) return false;
       return o.hashCode() == this.hashCode();
    }
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        if (getService() != null)
          result = 37*result + getService().hashCode();
        if (getTitle() != null)
          result = 37*result + getTitle().hashCode();
        if (getDataFormat() != null)
          result = 37*result + getDataFormat().hashCode();
        hashCode = result;
      }
      return hashCode;
    }
    private volatile int hashCode = 0;

  }

}

/* Change History:
   $Log: SelectService.java,v $
   Revision 1.6  2004/06/19 00:45:43  caron
   redo nested select list

   Revision 1.5  2004/06/18 21:54:26  caron
   update dqc 0.3

   Revision 1.4  2004/06/12 04:12:43  caron
   *** empty log message ***

   Revision 1.3  2004/06/12 02:01:10  caron
   dqc 0.3

   Revision 1.2  2004/05/21 05:57:32  caron
   release 2.0b

   Revision 1.1  2004/05/11 23:30:30  caron
   release 2.0a
 */