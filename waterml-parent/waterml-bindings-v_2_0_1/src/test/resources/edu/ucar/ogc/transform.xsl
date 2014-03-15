<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform
        version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:gml="http://www.opengis.net/gml/3.2">
    <!-- The XSLT identity transform. -->
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="node() | @*"/>
        </xsl:copy>
    </xsl:template>

    <!-- Do not copy attributes, located anywhere, with the name "xlink:type". -->
    <xsl:template match="//@xlink:type"/>

    <!-- Do not copy elements with an "xsi:nil='true'" attribute. -->
    <xsl:template match="//*[@xsi:nil=true()]"/>
</xsl:transform>
