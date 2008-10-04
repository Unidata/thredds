/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.bufrtables;

import org.springframework.web.servlet.view.xslt.AbstractXsltView;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;

import javax.xml.transform.Source;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import ucar.bufr.Message;

/**
 * Create an XML doc out of the list of BUFR messages.
 * This is the return of the validation request.
 *
 * @author caron
 * @since Oct 3, 2008
 */
public class BtXsltView extends AbstractXsltView {

}