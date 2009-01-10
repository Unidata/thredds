package uk.ac.rdg.resc.ncwms.metadata.xml;
import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

@Root(name="dataset")
class MetaDataset implements Serializable {
    
    @Attribute(required=false)
    private String id;
    
    @Attribute(required=false)
    private String lastModified;
    
    @Attribute(required=false)
    private String accessRequired;
    
    @Attribute(required=false)
    private String name;
    
    @Element(required=false)
    private Datafiles datafiles;
    
    @ElementList(inline=true, required=false)
    private List<Grid> grid;
    
    @ElementList(inline=true, required=false)
    private List<Variable> variable;
    
    @ElementList(inline=true, required=false)
    private List<IrregularAxis> irregularAxis;
    
    @ElementList(inline=true, required=false)
    private List<RegularAxis> regularAxis;
    
    
    public MetaDataset() {
    }
    
    public MetaDataset(String id,
            String lastModified,
            String accessRequired,
            String name,
            List<IrregularAxis> irregularAxis,
            List<RegularAxis> regularAxis,
            Datafiles datafiles,
            List<Grid> grid,
            List<Variable> variable) {        
        setId(id);
        setLastModified(lastModified);
        setAccessRequired(accessRequired);
        setName(name);
        setIrregularAxis(irregularAxis);
        setRegularAxis(regularAxis);
        setDatafiles(datafiles);
        setGrid(grid);
        setVariable(variable);
    }
    
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
    
    public String getLastModified() {
        return lastModified;
    }
    
    
    public void setAccessRequired(String accessRequired) {
        this.accessRequired = accessRequired;
    }
    
    public String getAccessRequired() {
        return accessRequired;
    }
    
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    
    public void setDatafiles(Datafiles datafiles) {
        this.datafiles = datafiles;
    }
    
    public Datafiles getDatafiles() {
        return datafiles;
    }
    
    
    public void setGrid(List<Grid> grid) {
        this.grid = grid;
    }
    
    public List<Grid> getGrid() {
        return grid;
    }
    
    
    public void setVariable(List<Variable> variable) {
        this.variable = variable;
    }
    
    public List<Variable> getVariable() {
        return variable;
    }
    
    public void setRegularAxis(List<RegularAxis> regular) {
        this.regularAxis = regular;
    }
    
    public List<RegularAxis> getRegularAxis() {
        return regularAxis;
    }
    
    public void setIrregularAxis(List<IrregularAxis> irregular) {
        this.irregularAxis = irregular;
    }
    
    public List<IrregularAxis> getIrregularAxis() {
        return irregularAxis;
    }
    
    
    public String toString() {
        String s = "id = " + id
                + "\nlastModified = " + lastModified
                + "\naccessRequired = " + accessRequired
                + "\nname = " + name
                + "\ngrid = " + collectionToString("grid", grid);
        return s;
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
