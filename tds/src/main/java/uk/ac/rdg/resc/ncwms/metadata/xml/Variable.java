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
 * Variable.java
 *
 * Created on 04 October 2007, 15:51
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

@Root
class Variable implements Serializable {
    
    @Attribute(required=false)
    private String name;
    
    @Attribute(required=false)
    private String grid_name;
    private Grid grid = null;
    
    @Attribute(required=false)
    private String datatype;
    
    @Attribute(required=false)
    private String valid_min;
    
    @Attribute(required=false)
    private String valid_max;
    
    @Attribute(required=false)
    private String units;
    
    @Attribute(required=false)
    private String internalName;
    
    @Attribute(required=false)
    private String fileset;
    
    
    /** Creates a new instance of Variable */
    public Variable() {
    }
    
    public Variable(String name,
            String validmin,
            String validmax,
            String gridName,
            String datatype,
            String units,
            String internalname,
            String fileset
            ) {
        
        setName(name);
        setValidMin(validmin);
        setValidMax(validmax);
        setGridName(gridName);
        setDataType(datatype);
        setUnits(units);
        setInternalName(internalname);
        setFileset(fileset);
    }
    
    
    public void validate() {
        // find the Grid object and
    }
    
    public void  setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void  setValidMin(String val) {
        this.valid_min = val;
    }
    
    public String getValidMin() {
        return valid_min;
    }
    public void  setValidMax(String val) {
        this.valid_max = val;
    }
    
    public String getValidMax() {
        return valid_max;
    }
    
    public void setGridName(String gridName) {
        this.grid_name = gridName;
    }
    
    public String getGridName() {
        return grid_name;
    }
    
    
    public void setDataType(String datatype) {
        this.datatype = datatype;
    }
    
    public String getDataType() {
        return datatype;
    }
    
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setInternalName(String internalname) {
        this.internalName = internalname;
    }
    
    public String getInternalName() {
        return internalName;
    }
    
    public void setFileset(String fileset) {
        this.fileset = fileset;
    }
    
    public String getFileset() {
        return fileset;
    }
    
    
}
