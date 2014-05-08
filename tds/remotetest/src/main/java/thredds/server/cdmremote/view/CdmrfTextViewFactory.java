/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.cdmremote.view;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.XSLTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thredds.server.cdmremote.params.CdmrfQueryBean;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.point.writer.FeatureDatasetPointXML;

/**
 * @author mhermida
 *
 */
public final class CdmrfTextViewFactory {

	private static final Logger log = LoggerFactory.getLogger(CdmrfTextViewFactory.class);
	
	private static CdmrfTextViewFactory INSTANCE; 
	
	private CdmrfTextViewFactory(){}
	
	public static final CdmrfTextViewFactory getInstance(){
		if(INSTANCE == null){
			INSTANCE = new CdmrfTextViewFactory();
		}
		
		return INSTANCE;
	}

	public String getFormViewForDataset(HttpServletRequest req, HttpServletResponse res, FeatureDatasetPoint fdp, String absPath, CdmrfQueryBean query) throws IOException {

		FeatureDatasetPointXML xmlWriter = new FeatureDatasetPointXML(fdp, absPath);

		CdmrfQueryBean.RequestType reqType = query.getRequestType();
		String infoString;

		try {

			if (reqType == CdmrfQueryBean.RequestType.capabilities) {
				Document doc = xmlWriter.getCapabilitiesDocument();
				XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
				infoString = fmt.outputString(doc);

			} else if (reqType == CdmrfQueryBean.RequestType.stations) {
				Document doc = xmlWriter.makeStationCollectionDocument(query.getLatLonRect(), query.getStnNames());
				XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
				infoString = fmt.outputString(doc);

			} else if (reqType == CdmrfQueryBean.RequestType.form) {
				String xslt = fdp.getFeatureType() == FeatureType.STATION ?  "ncssSobs.xsl" : "fmrcPoint.xsl";
				InputStream is = getXSLT(xslt);
				Document doc = xmlWriter.getCapabilitiesDocument();
				XSLTransformer transformer = new XSLTransformer(is);
				Document html = transformer.transform(doc);
				XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
				infoString = fmt.outputString(html);

			} else {
				return null;
			}

		} catch (Exception e) {
			log.error("SobsServlet internal error on "+fdp.getLocation(), e);
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SobsServlet internal error");
			return null;
		}

		return infoString;
	}
	
	  private InputStream getXSLT(String xslName) {

		    return getClass().getResourceAsStream("/resources/xsl/" + xslName);
		    
		  }	

}
