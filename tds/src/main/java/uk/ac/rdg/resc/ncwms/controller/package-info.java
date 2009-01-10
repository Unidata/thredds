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
 * This package contains the Controllers of ncWMS, which are the classes that
 * handle user requests (these are the main entry points to the ncWMS application
 * from the point of view of the end user).  The {@link uk.ac.rdg.resc.ncwms.controller.WmsController}
 * handles the requests for WMS operations (GetCapabilities, GetMap, GetFeatureInfo).
 * The {@link uk.ac.rdg.resc.ncwms.controller.AdminController} handles the administrative
 * web application.  The {@link uk.ac.rdg.resc.ncwms.controller.MetadataController}
 * handles requests for metadata from the Godiva2 website (for which the Capabilities
 * document is not suitable).  The {@link uk.ac.rdg.resc.ncwms.controller.FrontPageController}
 * handles requests for the ncWMS "Front Page", which is a simple diagnostic page that
 * provides links to the Capabilities document, the Godiva2 application, the admin
 * application and sample GetMap and GetFeatureInfo requests.
 */
package uk.ac.rdg.resc.ncwms.controller;