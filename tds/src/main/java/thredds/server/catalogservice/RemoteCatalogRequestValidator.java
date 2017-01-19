/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;

import java.net.URI;
import java.net.URISyntaxException;

public class RemoteCatalogRequestValidator implements Validator {

  public boolean supports(Class clazz) {
    return RemoteCatalogRequest.class.equals(clazz);
  }

  public void validate(Object obj, Errors errs) {
    RemoteCatalogRequest rcr = (RemoteCatalogRequest) obj;

      try {
          URI catUri = new URI(rcr.getCatalog());
          if (!catUri.isAbsolute())
              errs.rejectValue("catalogUri", "catalogUri.notAbsolute", "The catalog parameter must be an absolute URI.");
          if ( catUri.getScheme() != null
                  && ! catUri.getScheme().equalsIgnoreCase( "HTTP" )
                  && ! catUri.getScheme().equalsIgnoreCase( "HTTPS" ))
              errs.rejectValue( "catalogUri", "catalogUri.notHttpUri",
                      "The \"catalogUri\" field must be an HTTP|HTTPS URI.");
          rcr.setCatalogUri(catUri);

      } catch (URISyntaxException e) {
          errs.rejectValue("catalog", "catalogUri.notAbsolute", "catalog parameter is not a valid URI");
      }

    if (rcr.getDataset() != null)
      rcr.setCommand(RemoteCatalogRequest.Command.SUBSET);
    else if (rcr.getCommand() == null)
      rcr.setCommand(RemoteCatalogRequest.Command.SHOW);
  }
}
