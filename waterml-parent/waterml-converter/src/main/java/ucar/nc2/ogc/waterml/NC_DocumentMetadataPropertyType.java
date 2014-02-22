package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.DocumentMetadataPropertyType;
import net.opengis.waterml.v_2_0_1.DocumentMetadataType;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_DocumentMetadataPropertyType {
    // wml2:Collection/wml2:metadata
    public static DocumentMetadataPropertyType createMetadata() {
        DocumentMetadataPropertyType metadata = Factories.WATERML.createDocumentMetadataPropertyType();

        // wml:DocumentMetdata
        DocumentMetadataType documentMetadata = NC_DocumentMetadataType.createDocumentMetadata();
        metadata.setDocumentMetadata(documentMetadata);

        return metadata;
    }

    private NC_DocumentMetadataPropertyType() { }
}
