/*
 * Copyright (c) 1998 - 2012. University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.controller;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.server.ncSubset.dataservice.NcssShowDatasetInfo;
import thredds.server.ncSubset.util.NcssRequestUtils;

@Controller
@Scope("request")
class DatasetInfoController extends AbstractNcssController{
	
	
	private boolean wantXML = false;
	private boolean showForm = false;
	private boolean showPointForm = false;
	
	static private final Logger log = LoggerFactory.getLogger(DatasetInfoController.class);
	
	@Autowired
	private NcssShowDatasetInfo ncssShowDatasetInfo;
	
	@RequestMapping(value = { "/ncss/grid/**/dataset.html", "/ncss/grid/**/dataset.xml","/ncss/grid/**/pointDataset.html" })
	void getDatasetDescription(HttpServletRequest req, HttpServletResponse res) throws Exception {

		//String pathInfo = requestPathInfo;


		String strResponse = ncssShowDatasetInfo.showForm(getGridDataset(), buildDatasetUrl(requestPathInfo), wantXML, showPointForm);

		res.setContentLength(strResponse.length());
		if (wantXML)
			res.setContentType("text/xml; charset=iso-8859-1");
		else
			res.setContentType("text/html; charset=UTF-8");

		PrintWriter pw = res.getWriter();
		pw.write(strResponse);
		pw.flush();
		res.flushBuffer();
	}
	
	private String buildDatasetUrl(String path) {
		return  NcssRequestUtils.getTdsContext().getContextPath() + servletPath + "/" + path;
	}

	/* (non-Javadoc)
	 * @see thredds.server.ncSubset.controller.AbstractNcssController#extractRequestPathInfo(java.lang.String)
	 */
	@Override
	String extractRequestPathInfo(String requestPathInfo) {

		// the forms and dataset description
		wantXML = requestPathInfo.endsWith("/dataset.xml");
		showForm = requestPathInfo.endsWith("/dataset.html");
		showPointForm = requestPathInfo.endsWith("/pointDataset.html");
		
		if (wantXML || showForm || showPointForm) {
			int len = requestPathInfo.length();
			if (wantXML)
				requestPathInfo = requestPathInfo.substring(0, len - 12);
			else if (showForm)
				requestPathInfo = requestPathInfo.substring(0, len - 13);
			else if (showPointForm)
				requestPathInfo = requestPathInfo.substring(0, len - 18);

			if (requestPathInfo.startsWith("/"))
				requestPathInfo = requestPathInfo.substring(1);

		}		
		

		this.requestPathInfo = requestPathInfo;
		
		return requestPathInfo;
	}


}
