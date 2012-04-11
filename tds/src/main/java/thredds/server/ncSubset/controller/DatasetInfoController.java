package thredds.server.ncSubset.controller;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.config.TdsContext;
import thredds.server.ncSubset.dataservice.NcssShowDatasetInfo;
import thredds.server.ncSubset.util.NcssRequestUtils;
import thredds.servlet.UsageLog;

@Controller
class DatasetInfoController extends AbstractNcssController{
	
	static private final Logger log = LoggerFactory.getLogger(DatasetInfoController.class);
	
	@Autowired
	private NcssShowDatasetInfo ncssShowDatasetInfo;
	
	//@Autowired
	//private TdsContext tdsContext;
	

	@RequestMapping(value = { "/ncss/grid/**/dataset.html", "/ncss/grid/**/dataset.xml","/ncss/grid/**/pointDataset.html" })
	void getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws Exception {

		//log.info("getDatasetDescription(): "+ UsageLog.setupRequestContext(req));

		String pathInfo = requestPathInfo;

		// the forms and dataset description
		boolean wantXML = pathInfo.endsWith("/dataset.xml");
		boolean showForm = pathInfo.endsWith("/dataset.html");
		boolean showPointForm = pathInfo.endsWith("/pointDataset.html");
		if (wantXML || showForm || showPointForm) {
			int len = pathInfo.length();
			if (wantXML)
				pathInfo = pathInfo.substring(0, len - 12);
			else if (showForm)
				pathInfo = pathInfo.substring(0, len - 13);
			else if (showPointForm)
				pathInfo = pathInfo.substring(0, len - 18);

			if (pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);

		}

		String strResponse = ncssShowDatasetInfo.showForm(getGridDataset(), buildDatasetUrl(pathInfo), wantXML, showPointForm);

		res.setContentLength(strResponse.length());
		if (wantXML)
			res.setContentType("text/xml; charset=iso-8859-1");
		else
			res.setContentType("text/html; charset=iso-8859-1");

		PrintWriter pw = res.getWriter();
		pw.write(strResponse);
		pw.flush();
		res.flushBuffer();

		log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, strResponse.length()));

	}
	
	private String buildDatasetUrl(String path) {
		return  NcssRequestUtils.getTdsContext().getContextPath() + servletPath + "/" + path;
	}	

}
