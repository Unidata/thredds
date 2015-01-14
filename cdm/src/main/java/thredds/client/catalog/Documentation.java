/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog;

import net.jcip.annotations.Immutable;
import ucar.nc2.constants.CDM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Client catalog documentation element
 *
 * @author caron
 * @since 1/9/2015
 */
@Immutable
public class Documentation {
  private final String href, title, type, inlineContent;
  private final URI uri; // resolved

   /**
    * Constructor.
    *
    * @param href          : href of documentation, may be null.
    * @param uri           : absolute URL, or null
    * @param title         : Xlink title, may be null.
    * @param type          : user-defined InvDocumentation type
    * @param inlineContent : optional inline contents.
    */
   public Documentation(String href, URI uri, String title, String type, String inlineContent) {
     this.href = href;
     this.uri = uri;
     this.type = type;
     this.inlineContent = inlineContent;

     if (title != null)
       this.title = title;
     else if (uri != null)
       this.title = uri.toString();
     else
       this.title = null;
   }

   public String getType() {
     return type;
   }

   public boolean hasXlink() {
     return uri != null;
   }

   /**
    * if its a XLink, get the absolute URI
    * @return the XLink URI, else null
    */
   public URI getURI() {
     return uri;
   }

   /**
    * if its a XLink, get the title, to display the link to the user.
    * @return the XLink title, else null
    */
   public String getXlinkTitle() {
     return title;
   }

   /**
    * if its a XLink, get the href, to display the link to the user.
    * @return the XLink href, or null
    */
   public String getXlinkHref() {
     return href;
   }

   /**
    * Get inline content as a string, else null if there is none
    * @return  inline content as a string, else null
    */
   public String getInlineContent() {
     if (uri == null) return "";

     URL url;
     try {
       url = uri.toURL();
     } catch (MalformedURLException e) {
       return e.getMessage();
     }

     try (InputStream is = url.openStream()) {
       ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());

       // copy to string
       byte[] buffer = new byte[1024];
       while (true) {
         int bytesRead = is.read(buffer);
         if (bytesRead == -1) break;
         os.write(buffer, 0, bytesRead);
       }
       return new String(os.toByteArray(), CDM.utf8Charset);

     } catch (IOException ioe) {
       return ioe.getMessage();
     }

   }

   /**
    * string representation
    */
   public String toString() {
     if (hasXlink())
       return "<" + uri + "> <" + title + "> <" + type + ">";
     else
       return "<" + inlineContent + ">";
   }



}
