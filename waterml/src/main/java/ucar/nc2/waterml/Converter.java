package ucar.nc2.waterml;

import net.opengis.gml.v_3_2_1.CodeWithAuthorityType;
import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import net.opengis.sampling.v_2_0_0.SFSamplingFeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/02/25.
 */
public abstract class Converter {
    public static JAXBElement<OMObservationType> convertToWaterML(FeatureDatasetPoint fdPoint) throws IOException {
        net.opengis.om.v_2_0_0.ObjectFactory omObjectFactory = new net.opengis.om.v_2_0_0.ObjectFactory();

        OMObservationType omObservationType = convertToOM_Observation(fdPoint);
        JAXBElement<OMObservationType> omObservationElem = omObjectFactory.createOMObservation(omObservationType);

        return omObservationElem;
    }

    // om:OM_Observation
    public static OMObservationType convertToOM_Observation(FeatureDatasetPoint fdPoint) throws IOException {
        net.opengis.om.v_2_0_0.ObjectFactory omObjectFactory = new net.opengis.om.v_2_0_0.ObjectFactory();

        OMObservationType omObservationType = omObjectFactory.createOMObservationType();

        // om:OM_Observation/gml:id
        String id = omObservationType.getClass().getSimpleName() + "." + "1";
        omObservationType.setId(id);

        List<FeatureCollection> featCollList = fdPoint.getPointFeatureCollectionList();
        assert featCollList.size() == 1 && featCollList.get(0) instanceof StationTimeSeriesFeatureCollection;

        StationTimeSeriesFeatureCollection stationFeatColl = (StationTimeSeriesFeatureCollection) featCollList.get(0);
        StationTimeSeriesFeature stationFeat = stationFeatColl.next();
        assert !stationFeat.hasNext();

        // om:OM_Observation/om:featureOfInterest
        FeaturePropertyType featurePropertyType = convertToFeaturePropertyType(stationFeat);
        omObservationType.setFeatureOfInterest(featurePropertyType);

        return omObservationType;
    }

    // om:OM_Observation/om:featureOfInterest
    public static FeaturePropertyType convertToFeaturePropertyType(StationTimeSeriesFeature stationFeat) {
        net.opengis.gml.v_3_2_1.ObjectFactory gmlObjectFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();

        FeaturePropertyType featurePropertyType = gmlObjectFactory.createFeaturePropertyType();

        // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
        JAXBElement<SFSamplingFeatureType> sfSamplingFeatureElem = convertToSFSamplingFeatureElem(stationFeat);
        featurePropertyType.setAbstractFeature(sfSamplingFeatureElem);

        return featurePropertyType;
    }

    // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType
    public static JAXBElement<SFSamplingFeatureType> convertToSFSamplingFeatureElem(StationTimeSeriesFeature stationFeat) {
        net.opengis.sampling.v_2_0_0.ObjectFactory samObjectFactory = new net.opengis.sampling.v_2_0_0.ObjectFactory();

        SFSamplingFeatureType sfSamplingFeatureType = samObjectFactory.createSFSamplingFeatureType();




        JAXBElement<SFSamplingFeatureType> sfSamplingFeatureElem =
                samObjectFactory.createSFSamplingFeature(sfSamplingFeatureType);

        return sfSamplingFeatureElem;
    }

    // om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:identifier
    public static CodeWithAuthorityType convertToIdentifier(StationTimeSeriesFeature stationFeat) {
        net.opengis.gml.v_3_2_1.ObjectFactory gmlObjectFactory = new net.opengis.gml.v_3_2_1.ObjectFactory();

        CodeWithAuthorityType identifier = gmlObjectFactory.createCodeWithAuthorityType();
        identifier.setValue(stationFeat.getName());

        return identifier;
    }


    private Converter() { }
}
