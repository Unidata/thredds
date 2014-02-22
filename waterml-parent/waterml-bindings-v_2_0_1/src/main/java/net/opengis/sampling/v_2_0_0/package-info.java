@XmlSchema(
        namespace = "http://www.opengis.net/sampling/2.0",
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "sam",   namespaceURI = "http://www.opengis.net/sampling/2.0"),
                @XmlNs(prefix = "xlink", namespaceURI = "http://www.w3.org/1999/xlink"),
                @XmlNs(prefix = "xsi",   namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
        },
        location = "http://www.opengis.net/sampling/2.0 http://schemas.opengis.net/sampling/2.0/samplingFeature.xsd"
)
package net.opengis.sampling.v_2_0_0;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;
