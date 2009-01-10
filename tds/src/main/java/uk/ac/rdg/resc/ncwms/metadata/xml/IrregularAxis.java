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
