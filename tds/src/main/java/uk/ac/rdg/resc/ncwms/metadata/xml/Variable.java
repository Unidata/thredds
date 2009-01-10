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
