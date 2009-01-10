package uk.ac.rdg.resc.ncwms.metadata.xml;
import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

@Root
class Datafiles implements Serializable {
    
    @Attribute(required=false)
    private String fileformat;
    
    @Attribute(required=false)
    private String units;
    
    @Attribute(required=false)
    private String timeunits;
    
    @Attribute(required=false)
    private String root;
    
    @ElementList(inline=true, required=false)
    private List<FileDetails> fileDetails;
    
    public Datafiles() {
    }
    
    public Datafiles(
            String fileformat,
            String units,
            String timeunits,
            String root,
            List<FileDetails> fileDetails
            ) {
        
        setFileFormat(fileformat);
        setUnits(units);
        setRoot(root);
        setFileDetails(fileDetails);
    }
    
    public void setFileFormat(String format) {
        this.fileformat = format;
    }
    
    public String getFileFormat() {
        return fileformat;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getUnits() {
        return units;
    }
    
    
    public void setRoot(String root) {
        this.root = root;
    }
    
    public String getRoot() {
        return root;
    }
    
    public void setFileDetails( List<FileDetails> fileDetails) {
        this.fileDetails = fileDetails;
    }
    
    public List<FileDetails> getFileDetails() {
        return fileDetails;
    }
    
    private String collectionToString(String objName, List objCollection) {
        if (objCollection == null) return "";
        String s = "\n{";
        Iterator iObj = objCollection.iterator();
        int i = 0;
        while (iObj.hasNext()) {
            s += objName + "[" + (i++) + "]=" + iObj.next() + "\n";
        }
        s += "}";
        return s;
    }
    
}
