/*
 * Copyright (c) 2010 The University of Reading
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

package thredds.server.wms;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.ncwms.controller.AbstractMetadataController;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.LayerFactory;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @author Jon Blower
 */
class ThreddsMetadataController extends AbstractMetadataController
{
    private final ThreddsDataset dataset;
    private final ThreddsServerConfig serverConfig;

    public ThreddsMetadataController(LayerFactory layerFactory,
            ThreddsServerConfig serverConfig, ThreddsDataset dataset)
    {
        super(layerFactory);
        this.serverConfig = serverConfig;
        this.dataset = dataset;
    }
    
    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy.
     */
    @Override
    protected ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry)
        throws Exception
    {
        // This method is designed to reuse the existing JSP code from ncWMS.
        // Hence we create a map containing only one dataset.
        Map<String, ThreddsDataset> allDatasets = new HashMap<String, ThreddsDataset>();
        allDatasets.put(this.dataset.getId(), this.dataset);
        String menu = "default";
        usageLogEntry.setMenu(menu);
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("serverTitle", this.serverConfig.getTitle());
        models.put("datasets", allDatasets);
        return new ModelAndView(menu + "Menu", models);
    }
    
}
