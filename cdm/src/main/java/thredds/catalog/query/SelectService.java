// $Id: SelectService.java 48 2006-07-12 16:15:40Z caron $
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
