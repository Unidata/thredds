/*
 * Copyright (c) 2006 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.ac.rdg.resc.ncwms.exceptions;

/**
 * This wraps a WmsException to ensure that the correct JSP is used to render
 * this exception object.  This is a bit of a hack: it might be better to have
 * a single WmsException class with a "version" property.  However, it is very
 * difficult to write a single JSP that can handle both 1.3.0 and 1.1.1 exceptions,
 * because the MIME types are different.  Therefore we create a new class for
 * 1.1.1 exceptions, which causes Spring to render the exception using a different
 * JSP.
 * @see web/WEB-INF/WMS-servlet.xml
 * @see WmsException
 * @see web/WEB-INF/jsp/displayWms1_1_1Exception.jsp
 * @author Jon
 */
public class Wms1_1_1Exception extends Exception
{
    private WmsException wmse;
    
    public Wms1_1_1Exception(WmsException wmse)
    {
        this.wmse = wmse;
    }
    
    public String getCode()
    {
        // Alters the code of the InvalidCrsException if necessary
        return wmse instanceof InvalidCrsException ? "InvalidSRS" : wmse.getCode();
    }
    
    @Override
    public String getMessage()
    {
        return wmse.getMessage();
    }
}
