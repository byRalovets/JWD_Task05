package by.ralovets.epamcourse.dao.pool.exception;

/**
 * @author Anton Ralovets
 * @version 1.0
 */
public class ConnectionPoolException extends Exception {

    public ConnectionPoolException() {
        super();
    }

    public ConnectionPoolException(String message) {
        super(message);
    }
}
