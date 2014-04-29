package ucar.nc2.ogc2;

import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import org.n52.oxf.xmlbeans.parser.sosexample.OwsExceptionReport;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;

import java.io.File;
import java.io.IOException;

/**
 * Created by cwardgar on 2014/04/17.
 */
public class Foo {
    public static void main(String[] args) throws IOException, NoFactoryFoundException, XMLHandlingException, OwsExceptionReport {
        File pointFile = new File("C:/Users/cwardgar/Desktop/multiStationSingleVar.ncml");
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            VariableSimpleIF dataVar = fdPoint.getDataVariable("tmax");
            MarshallingUtil.marshalPointDataset(fdPoint, dataVar, System.out);
        } finally {
            fdPoint.close();
        }
    }
}
