
package org.spf4j.base;

/**
 *
 * @author zoly
 */
public final class TestException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public TestException() {
    }

    public TestException(final String message) {
        super(message);
    }

    public TestException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TestException(final Throwable cause) {
        super(cause);
    }

    public TestException(final String message, final Throwable cause,
            final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    
    
}
