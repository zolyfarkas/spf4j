/**
 * This package contains operation retry utilities.
 *
 * Why yet another package?
 *
 * 1) No Exception lost + ability to propagate checked exceptions (Sync mode only).
 * 2) Retry operation can be different from original operation. (redirect, fallback, etc...)
 * 3) The retry operation can be executed with delay which can be a function of the response or exception.
 * 4) Timeouts are core functionality.
 *
 */
package org.spf4j.failsafe;
