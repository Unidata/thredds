/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.viewer;

import java.util.Formatter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import thredds.client.catalog.Dataset;

public interface ViewerService {

	List<Viewer>  getViewers();
	
	Viewer getViewer(String viewer);
	
	String getViewerTemplate(String template);
	
	boolean registerViewer(Viewer v);
	
	void showViewers(Formatter sbuff, Dataset dataset, HttpServletRequest req);

	List<ViewerLinkProvider.ViewerLink> getViewerLinks(Dataset dataset, HttpServletRequest req);
	
}
