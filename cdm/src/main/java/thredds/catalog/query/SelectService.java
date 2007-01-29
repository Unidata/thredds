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
 * @author john caron */

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


  public void addServiceChoice( String service, String title, String dataFormat, String returns, String value) {
    choices.add( new ServiceChoice(service, title, dataFormat, returns, value));
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
    private String service, title, dataFormat, returns, value;
    ServiceChoice( String service, String title, String dataFormat, String returns, String value) {
      this.service = service;
      this.title = title;
      this.dataFormat = dataFormat;
      this.returns = returns;
      this.value = value;
    }
    public String getService() { return service; }
    public String getTitle() { return title; }
    public String getDataFormat() { return dataFormat; }
    public String getReturns() { return returns; }

    // Choice
    public String toString() { return title != null ? title : service; }
    public String getValue2() { return value; }
    public String getValue() { return value != null ? value : service; }

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
        if (getReturns() != null)
          result = 37*result + getReturns().hashCode();
        if (getValue() != null)
          result = 37*result + getValue().hashCode();
        hashCode = result;
      }
      return hashCode;
    }
    private volatile int hashCode = 0;

  }

}
