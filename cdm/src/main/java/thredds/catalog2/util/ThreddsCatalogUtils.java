package thredds.catalog2.util;

import ucar.nc2.units.DateType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.TimeDuration;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog2.builder.ThreddsMetadataBuilder;

import java.text.ParseException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsCatalogUtils
{
    private ThreddsCatalogUtils() {}

    public static DateType datePointToDateType( ThreddsMetadata.DatePoint datePoint )
            throws ParseException
    {
        return new DateType( datePoint.getDate(), datePoint.getDateFormat(), datePoint.getType());
    }

    public static DateType datePointBuilderToDateType( ThreddsMetadataBuilder.DatePointBuilder datePoint )
            throws ParseException
    {
        return new DateType( datePoint.getDate(), datePoint.getDateFormat(), datePoint.getType());
    }

    public static DateRange dateRangeToNcDateRange( ThreddsMetadata.DateRange dateRange )
            throws ParseException
    {
        DateType startDate = new DateType( dateRange.getStartDate(), dateRange.getStartDateFormat(), null );
        DateType endDate = new DateType( dateRange.getEndDate(), dateRange.getEndDateFormat(), null );
        return new DateRange( startDate, endDate, new TimeDuration( dateRange.getDuration()), null );
    }

    public static DateRange dateRangeBuilderToNcDateRange( ThreddsMetadataBuilder.DateRangeBuilder dateRange )
            throws ParseException
    {
        DateType startDate = new DateType( dateRange.getStartDate(), dateRange.getStartDateFormat(), null );
        DateType endDate = new DateType( dateRange.getEndDate(), dateRange.getEndDateFormat(), null );
        return new DateRange( startDate, endDate, new TimeDuration( dateRange.getDuration()), null );
    }
}
