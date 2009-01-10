/*
 * Copyright (c) 2007 The University of Reading
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

/**
 * <p>Exceptions that can be thrown by ncWMS.  {@link uk.ac.rdg.resc.ncwms.exceptions.WmsException}s
 * and subclasses will be caught by the Spring framework and rendered using
 * the XML template <tt>web/WEB-INF/jsp/displayWmsException.jsp</tt>, which returns
 * the exception to the client in XML format, according to the WMS specification.</p>
 * 
 * <p>Similarly, 
 * {@link uk.ac.rdg.resc.ncwms.exceptions.MetadataException}s are rendered
 * in JSON format for the Godiva2 website by <tt>web/WEB-INF/jsp/displayMetadataException.jsp</tt>.</p>
 *
 * <p>Other exceptions represent internal bugs and are rendered using
 * <tt>web/WEB-INF/jsp/displayDefaultException.jsp</tt>.</p>
 * 
 * <p>The mapping of exception classes to JSP templates is done in the Spring
 * configuration file, <tt>web/WEB-INF/WMS-servlet.xml</tt> (look for the
 * <tt>exceptionResolver</tt> bean).</p>
 */
package uk.ac.rdg.resc.ncwms.exceptions;