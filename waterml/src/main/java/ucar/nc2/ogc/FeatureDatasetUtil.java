package ucar.nc2.ogc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDataset;

import java.util.List;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class FeatureDatasetUtil {
    private static final Logger logger = LoggerFactory.getLogger(FeatureDatasetUtil.class);

    public static VariableSimpleIF getOnlyDataVariable(FeatureDataset dataset) {
        List<VariableSimpleIF> dataVars = dataset.getDataVariables();
        assert !dataVars.isEmpty() : String.format("%s has no data variables. That can't happen, right?", dataset);

        if (dataVars.size() > 1) {
            logger.warn(String.format(
                    "PointFeature has more than one data variable (%s). Only using the first.", dataVars));
        }

        return dataVars.get(0);
    }

    private FeatureDatasetUtil() { }
}
