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

package thredds.server.wms.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * LayerSettings for a certain dataset path
 * @author Jon
 */
class DatasetPathSettings {
    
    private final String pathSpec;
    private final Pattern pathSpecPattern;
    private final LayerSettings defaultSettings;
    private final Map<String, LayerSettings> settingsPerVariable;

    public DatasetPathSettings(Element el) throws WmsConfigException
    {
        this.pathSpec = el.getAttributeValue("pathSpec");
        this.defaultSettings = new LayerSettings(el.getChild("pathDefaults"));
        this.settingsPerVariable = new HashMap<String, LayerSettings>();

        try
        {
            // We create a new GlobCompiler object each time to ensure thread safety
            // (the jakarta-oro docs for Perl5Compiler, on which the GlobCompiler is
            // based, say that this should be done.)
            GlobCompiler globCompiler = new GlobCompiler();

            // We create the pattern with the READ_ONLY option to ensure that the
            // pattern is thread safe (see the javadocs for GlobCompiler.compile()).
            this.pathSpecPattern = globCompiler.compile(this.pathSpec, GlobCompiler.READ_ONLY_MASK);

            @SuppressWarnings("unchecked")
            List<Element> variablesList =
                (List<Element>)XPath.selectNodes(el, "variables/variable");
            for (Element variableEl : variablesList)
            {
                String varId = variableEl.getAttributeValue("id");
                LayerSettings varSettings = new LayerSettings(variableEl);
                this.settingsPerVariable.put(varId, varSettings);
            }
        }
        catch(Exception e)
        {
            throw new WmsConfigException(e);
        }
    }

    public String getPathSpec() { return this.pathSpec; }

    /**
     * Returns true if the given string matches the {@link #getPathSpec() url pattern}
     * for this dataset.
     */
    public boolean pathSpecMatches(String s)
    {
        // We create a new matcher per invocation for thread safety
        return new Perl5Matcher().matches(s, this.pathSpecPattern);
    }

    public LayerSettings getDefaultSettings() { return this.defaultSettings; }

    public Map<String, LayerSettings> getSettingsPerVariable() {
        return settingsPerVariable;
    }

}
