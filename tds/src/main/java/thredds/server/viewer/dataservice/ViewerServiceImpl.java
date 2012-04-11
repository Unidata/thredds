package thredds.server.viewer.dataservice;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import thredds.catalog.InvAccess;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvProperty;
import thredds.catalog.ServiceType;
import thredds.server.wms.Godiva2Viewer;
import thredds.servlet.Viewer;
import thredds.servlet.ViewerLinkProvider;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

@Service
public class ViewerServiceImpl implements ViewerService {

	private static Logger log = LoggerFactory.getLogger(ViewerService.class);

	private List<Viewer> viewers = new ArrayList<Viewer>();
	private HashMap<String, String> templates = new HashMap<String, String>();

	@Override
	public List<Viewer> getViewers() {

		return null;
	}

	@Override
	public Viewer getViewer(String viewer) {

		return null;
	}

	@Override
	public boolean registerViewer(Viewer v) {

		return viewers.add(v);
	}

	@Override
	public String getViewerTemplate(String path) {

		String template = templates.get(path);
		if (template != null)
			return template;

		try {
			template = IO.readFile(path);
		} catch (IOException ioe) {
			return null;
		}

		templates.put(path, template);
		return template;

	}
	
	@Override
	public void showViewers(StringBuilder sbuff, InvDatasetImpl dataset, HttpServletRequest req){

	    int count = 0;
	    for (Viewer viewer : viewers) {
	      if (viewer.isViewable(dataset)) count++;
	    }
	    if (count == 0) return;

	    sbuff.append("<h3>Viewers:</h3><ul>\r\n");

	    for (Viewer viewer : viewers)
	    {
	      if (viewer.isViewable(dataset))
	      {
	        if ( viewer instanceof ViewerLinkProvider )
	        {
	          List<ViewerLinkProvider.ViewerLink> sp = ( (ViewerLinkProvider) viewer ).getViewerLinks( dataset, req );
	          for ( ViewerLinkProvider.ViewerLink vl : sp )
	            if ( vl.getUrl() != null & !vl.getUrl().equals( "" ) )
	              sbuff.append( "<li><a href='" ).append( vl.getUrl() )
	                      .append( "'>" ).append( vl.getTitle() != null ? vl.getTitle() : vl.getUrl() )
	                      .append( "</a></li>\n" );

	        } else {
	          String viewerLinkHtml = viewer.getViewerLinkHtml( dataset, req );
	          if ( viewerLinkHtml != null )
	          {
	            sbuff.append( "  <li> " );
	            sbuff.append( viewerLinkHtml );
	            sbuff.append( "</li>\n" );
	          }
	        }
	      }
	    }
	    sbuff.append("</ul>\r\n");
	}

	@SuppressWarnings("unused")
	@PostConstruct
	private void registerViewers() {
		registerViewer(new Godiva2Viewer());
		registerViewer(new ToolsUI());
		registerViewer(new IDV());
		registerViewer(new StaticView());

	}

	// Viewers...
	// ToolsUI
	private static class ToolsUI implements Viewer {

		public boolean isViewable(InvDatasetImpl ds) {
			String id = ds.getID();
			return ((id != null) && ds.hasAccess());
		}

		public String getViewerLinkHtml(InvDatasetImpl ds,
				HttpServletRequest req) {
			String base = ds.getParentCatalog().getUriString();
			if (base.endsWith(".html"))
				base = base.substring(0, base.length() - 5) + ".xml";
			Formatter query = new Formatter();
			query.format("<a href='%s/view/ToolsUI.jnlp?", req.getContextPath());
			query.format(
					"catalog=%s&amp;dataset=%s'>NetCDF-Java ToolsUI (webstart)</a>",
					base, ds.getID());
			return query.toString();
		}
	}

	// IDV
	private static class IDV implements Viewer {

		public boolean isViewable(InvDatasetImpl ds) {
			InvAccess access = getOpendapAccess(ds);
			if (access == null)
				return false;

			FeatureType dt = ds.getDataType();
			if (dt != FeatureType.GRID)
				return false;
			return true;
		}

		public String getViewerLinkHtml(InvDatasetImpl ds,
				HttpServletRequest req) {
			InvAccess access = getOpendapAccess(ds);

			URI dataURI = access.getStandardUri();
			if (!dataURI.isAbsolute()) {
				try {
					URI base = new URI(req.getRequestURL().toString());
					dataURI = base.resolve(dataURI);
					// System.out.println("Resolve URL with "+req.getRequestURL()+" got= "+dataURI.toString());
				} catch (URISyntaxException e) {
					log.error("Resolve URL with " + req.getRequestURL(), e);
				}
			}

			return "<a href='" + req.getContextPath() + "/view/idv.jnlp?url="
					+ dataURI.toString()
					+ "'>Integrated Data Viewer (IDV) (webstart)</a>";
		}

		private InvAccess getOpendapAccess(InvDatasetImpl ds) {
			InvAccess access = ds.getAccess(ServiceType.DODS);
			if (access == null)
				access = ds.getAccess(ServiceType.OPENDAP);
			return access;
		}
	}

	//StaticView
	private static class StaticView implements ViewerLinkProvider {

		private final String propertyNamePrefix = "viewer";

		public boolean isViewable(InvDatasetImpl ds) {
			return hasViewerProperties(ds);
		}

		public String getViewerLinkHtml(InvDatasetImpl ds,
				HttpServletRequest req) {
			List<ViewerLink> viewerLinks = getViewerLinks(ds, req);
			if (viewerLinks.isEmpty())
				return null;
			ViewerLink firstLink = viewerLinks.get(0);
			return "<a href='" + firstLink.getUrl() + "'>"
					+ firstLink.getTitle() + "</a>";
		}

		@Override
		public List<ViewerLink> getViewerLinks(InvDatasetImpl ds,
				HttpServletRequest req) {
			List<InvProperty> viewerProperties = findViewerProperties(ds);
			if (viewerProperties.isEmpty())
				return Collections.emptyList();
			List<ViewerLink> result = new ArrayList<ViewerLink>();
			for (InvProperty p : viewerProperties) {
				ViewerLink viewerLink = parseViewerPropertyValue(p.getName(),
						p.getValue(), ds, req);
				if (viewerLink != null)
					result.add(viewerLink);
			}
			return result;
		}

		private ViewerLink parseViewerPropertyValue(String viewerName,
				String viewerValue, InvDatasetImpl ds, HttpServletRequest req) {
			String viewerUrl;
			String viewerTitle;

			int lastCommaLocation = viewerValue.lastIndexOf(",");
			if (lastCommaLocation != -1) {
				viewerUrl = viewerValue.substring(0, lastCommaLocation);
				viewerTitle = viewerValue.substring(lastCommaLocation + 1);
				if (viewerUrl.equals(""))
					return null;
				if (viewerTitle.equals(""))
					viewerTitle = viewerName;
			} else {
				viewerUrl = viewerValue;
				viewerTitle = viewerName;
			}
			viewerUrl = StringUtil2.quoteHtmlContent(sub(viewerUrl, ds, req));

			ViewerLink viewerLink = new ViewerLink(viewerTitle, viewerUrl);
			return viewerLink;
		}

		private boolean hasViewerProperties(InvDatasetImpl ds) {
			for (InvProperty p : ds.getProperties())
				if (p.getName().startsWith(propertyNamePrefix))
					return true;

			return false;
		}

		private List<InvProperty> findViewerProperties(InvDatasetImpl ds) {
			List<InvProperty> result = new ArrayList<InvProperty>();
			for (InvProperty p : ds.getProperties())
				if (p.getName().startsWith(propertyNamePrefix))
					result.add(p);

			return result;
		}

		private String sub(String org, InvDatasetImpl ds, HttpServletRequest req) {
			List<InvAccess> access = ds.getAccess();
			if (access.size() == 0)
				return org;

			// look through all access for {serviceName}
			for (InvAccess acc : access) {
				String sname = "{" + acc.getService().getServiceType() + "}";
				if (org.indexOf(sname) >= 0)
					return StringUtil2.substitute(org, sname, acc
							.getStandardUri().toString());
			}

			String sname = "{url}";
			if ((org.indexOf(sname) >= 0) && (access.size() > 0)) {
				InvAccess acc = access.get(0); // just use the first one
				return StringUtil2.substitute(org, sname, acc.getStandardUri()
						.toString());
			}

			return org;
		}
	}

}
