// $Id: DqcConvertIF.java,v 1.3 2006/01/17 01:46:51 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package thredds.catalog.query;

/**
 * Converts XML DOM to DQC Objects.
 *
 * @see DqcFactory
 * @author John Caron
 * @version $Id: DqcConvertIF.java,v 1.3 2006/01/17 01:46:51 caron Exp $
 */

public interface DqcConvertIF {

  public QueryCapability parseXML( DqcFactory fac, org.jdom.Document domDoc, java.net.URI docURI) throws java.io.IOException;

  public void writeXML(QueryCapability qc, java.io.OutputStream os) throws java.io.IOException;

}

/* Change History:
   $Log: DqcConvertIF.java,v $
   Revision 1.3  2006/01/17 01:46:51  caron
   use jdom instead of dom everywhere

   Revision 1.2  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.1  2004/05/11 23:30:29  caron
   release 2.0a

 */