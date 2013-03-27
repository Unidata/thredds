package ucar.nc2.util.log;

/**
 * Interface for generating org.slf4j.Logger objects.
 * Allows us to keep log4j dependencies out of the cdm
 *
 * @author caron
 * @since 3/27/13
 */
public interface LoggerFactory {

   public org.slf4j.Logger getLogger(String name);

}
