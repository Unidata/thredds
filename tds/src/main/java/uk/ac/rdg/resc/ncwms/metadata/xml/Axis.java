/*
 * Axis.java
 *
 * Created on 12 October 2007, 13:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.metadata.xml;

import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

@Root(name="axis")
class Axis {
    
    @Attribute(required=false)
    private String axistype;
    
    @Attribute(required=false)
    private String axisname;
    
    public Axis(){
        
    }
    
    public Axis(String type,
            String name
            ) {
        
        setType(type);
        setName(name);
    }
    
    public void  setType(String type) {
        this.axistype = type;
    }
    
    public String getType() {
        return axistype;
    }
    
    
    public void  setName(String name) {
        this.axisname = name;
    }
    
    public String getName() {
        return axisname;
    }
    
    
}
