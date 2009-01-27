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
/*
 * IrregularAxis.java
 *
 * Created on 16 October 2007, 10:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.metadata.xml;

import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

/**
 *
 * @author ads
 */


@Root(name="irregularAxis")
class IrregularAxis implements Serializable {
    
    @Attribute(required=false)
    private String axisname;
    
    @Attribute(required=false)
    private String type;
    
    @Attribute(required=false)
    private String units;
    
    @Attribute(required=false)
    private String positive;
    
    @Attribute(required=false)
    private String value;
    
    /** Creates a new instance of IrregularAxis */
    public IrregularAxis() {
    }
    
    public IrregularAxis(String name,
            String type,
            String units,
            String positive,
            String value
            ) {
        
        setAxisName(name);
        setAxisType(type);
        setUnits(units);
        setPositive(positive);
        setValue(value);
    }
    
    public void  setAxisName(String name) {
        this.axisname = name;
    }
    
    public String getAxisName() {
        return axisname;
    }
    
    
    public void setAxisType(String type) {
        this.type = type;
    }
    
    public String getAxisType() {
        return type;
    }
    
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setPositive(String pos) {
        this.positive = pos;
    }
    
    public String getPositive() {
        return positive;
    }
    
    public void setValue(String val) {
        this.value = val;
    }
    
    public String getValue() {
        return value;
    }
}
