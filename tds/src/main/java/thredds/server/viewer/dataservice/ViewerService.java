package thredds.server.viewer.dataservice;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import thredds.catalog.InvDatasetImpl;
import thredds.servlet.Viewer;


public interface ViewerService {

	public List<Viewer>  getViewers();
	
	public Viewer getViewer(String viewer);
	
	public String getViewerTemplate(String template);
	
	public boolean registerViewer(Viewer v);
	
	public void showViewers(StringBuilder sbuff, InvDatasetImpl dataset, HttpServletRequest req);
	
}
