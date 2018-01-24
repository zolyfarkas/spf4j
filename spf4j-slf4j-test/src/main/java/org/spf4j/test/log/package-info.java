/**
 * spf4j test log backend for slf4j.
 *
 * This is an opinionated logging backend implementation with the following features:
 *
 * 1) Readable and parseable output.
 * 2) High Performance. (Your builds should not be slow due to your logging)
 * 3) Ability to assert Logging behavior.
 * 4) Fail unit tests that log an Error by default (if respective Error logs are not asserted against).
 * 5) Make debug logs available on unit test failure. This helps 2) a lot by not requiring you to run your unit tests
 * with tons of debug info dumped to output. But making it available when you actually need it (Unit test failure)
 * 6) Easily change logging configuration via API.
 *
 *
 */
package org.spf4j.test.log;
