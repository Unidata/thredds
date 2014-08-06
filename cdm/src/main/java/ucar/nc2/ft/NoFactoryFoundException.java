package ucar.nc2.ft;

/**
 * Indicates that no suitable {@link ucar.nc2.ft.FeatureDatasetFactory factory} could be found to open a
 * {@link ucar.nc2.ft.FeatureDataset feature dataset}.
 */
public class NoFactoryFoundException extends Exception {
    /**
     * Constructs a new NoFactoryFoundException with {@code null} as its detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to {@link #initCause}.
     */
    public NoFactoryFoundException() {
        super();
    }

    /**
     * Constructs a new NoFactoryFoundException with the specified detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *                {@link #getMessage()} method.
     */
    public NoFactoryFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new NoFactoryFoundException with the specified cause and a detail message of
     * {@code (cause == null ? null : cause.toString())} (which typically contains the class and detail message of
     * {@code cause}). This constructor is useful for NoFactoryFoundExceptions that are little more than wrappers
     * for other Throwables.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
     *              value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public NoFactoryFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new NoFactoryFoundException with the specified detail message and cause. Note that the detail
     * message associated with {@code cause} is <i>not</i> automatically incorporated in this
     * NoFactoryFoundException's detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *                (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public NoFactoryFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
