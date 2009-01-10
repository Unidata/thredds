/*
 * RegularAxis.java
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
@Root(name="regularAxis")
class RegularAxis {
    
    @Attribute(required=false)
    private String axisname;
    
    @Attribute(required=false)
    private String type;
    
    @Attribute(required=false)
    private String units;
    
    @Attribute(required=false)
    private String start;
    
    @Attribute(required=false)
    private String stride;
    
    @Attribute(required=false)
    private String count;
    
    /** Creates a new instance of RegularAxis */
    public RegularAxis() {
    }
    
    public RegularAxis(String name,
            String type,
            String units,
            String start,
            String stride,
            String count
            ) {
        
        setAxisName(name);
        setAxisType(type);
        setUnits(units);
        setStart(start);
        setStride(stride);
        setCount(count);
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
    
    public void setStart(String start) {
        this.start = start;
    }
    
    public String getStart() {
        return start;
    }
    
    public void setStride(String stride) {
        this.stride = stride;
    }
    
    public String getStride() {
        return stride;
    }
    
    public void setCount(String count) {
        this.count = count;
    }
    
    public String getCount() {
        return count;
    }
    
}
