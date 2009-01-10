/*
 * FileDetails.java
 *
 * Created on 17 October 2007, 10:21
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
@Root(name="filedetails")
class FileDetails implements Serializable{
    
    @Attribute(required=false)
    private String location;
    
    @Attribute(required=false)
    private String startdate;
    
    @Attribute(required=false)
    private String start;
    
    @Attribute(required=false)
    private String stride;
    
    @Attribute(required=false)
    private String count;
    
    /** Creates a new instance of FileDetails */
    public FileDetails() {
    }
    
    public FileDetails(String loca,
            String startdate,
            String start,
            String stride,
            String count
            ) {
        
        setLocation(loca);
        setStartDate(startdate);
        setStart(start);
        setStride(stride);
        setCount(count);
    }
    
    public void  setLocation(String loca) {
        this.location = loca;
    }
    
    public String getLocation() {
        return location;
    }
    
    
    public void setStartDate(String startdate) {
        this.startdate = startdate;
    }
    
    public String getStartDate() {
        return startdate;
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
