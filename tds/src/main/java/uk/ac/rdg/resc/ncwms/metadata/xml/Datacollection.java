package uk.ac.rdg.resc.ncwms.metadata.xml;
import java.io.Serializable;
import org.simpleframework.xml.*;
import java.util.List;
import java.util.Iterator;

@Root
class Datacollection implements Serializable {

	@ElementList(inline=true, required=false)
	private List<MetaDataset> dataset;

	public Datacollection() {
	}

	public Datacollection(List<MetaDataset> dataset) {

		setDataset(dataset);
	}


	public void setDataset(List<MetaDataset> dataset) {
		this.dataset = dataset;
	}

	public List<MetaDataset> getDataset() {
		return dataset;
	}


	public String toString() {
		String s = "dataset = " + collectionToString("dataset", dataset);
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
