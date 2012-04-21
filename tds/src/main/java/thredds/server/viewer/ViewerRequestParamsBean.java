package thredds.server.viewer;

import javax.validation.constraints.NotNull;

public class ViewerRequestParamsBean {
	
	@NotNull
	private String viewer;
	
	private String dataset;
	
	private String catalog;
	
	private String url;
	
	public String getViewer() {
		return viewer;
	}
	public void setViewer(String viewer) {
		this.viewer = viewer;
	}
	public String getDataset() {
		return dataset;
	}
	public void setDataset(String dataset) {
		this.dataset = dataset;
	}
	public String getCatalog() {
		return catalog;
	}
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	
	public void setUrl(String url){
		this.url = url;
	}

	public String getUrl(){
		return this.url;
	}

}
