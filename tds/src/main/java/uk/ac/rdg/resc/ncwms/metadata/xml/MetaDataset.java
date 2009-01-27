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
