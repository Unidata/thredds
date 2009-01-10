package uk.ac.rdg.resc.ncwms.metadata.xml;

import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

@Root
class Grid implements Serializable {
    
    @Attribute(required=false)
    private String name;
    
    @ElementList(parent="axis", inline=true, required=false)
    private List<Axis> axis;
    
    public Grid() {
    }
    
    public Grid(String name,
            List<Axis> axis) {
        
        setName(name);
        setAxis(axis);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setAxis(List<Axis> axis) {
        this.axis = axis;
    }
    
    public List<Axis> getAxis() {
        return axis;
    }
}
