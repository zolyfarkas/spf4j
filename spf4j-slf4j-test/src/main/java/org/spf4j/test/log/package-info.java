/**
 * spf4j test log backend for slf4j.
 *
 * This is an opinionated logging backend implementation with the following features:
 *
 *  - Readable and parse-able output.
 *  - Fast. (logging is no reason to have slow builds)
 *  - Ability to assert Logging behavior.
 *  - Fail unit tests that log an Error by default (if respective Error logs are not asserted against).
 *  - Make debug logs available on unit test failure. This helps performance a lot by not requiring you to run your
 *    unit tests with tons of debug info dumped to output all the time. But making it available when you actually need
 *    it (Unit test failure)
 *  - Easily change logging configuration via API.
 *  - Uncaught exceptions from other threads will fail your tests. You can assert them if they are expected.
 *  - No configurable format, It is the best format so everybody should be using it. Format will be evolved as needed.
 *
 *
 */
package org.spf4j.test.log;
