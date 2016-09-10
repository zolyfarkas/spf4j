package org.spf4j.junit;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.spf4j.stackmonitor.FastStackCollector;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author zoly
 */
public final class Spf4jRunListener extends RunListener {

  private final Sampler sampler = new Sampler(Integer.getInteger("spf4j.junit.sampleTimeMillis", 5),
          Integer.getInteger("spf4j.junit.dumpAfterMillis", Integer.MAX_VALUE),
          new FastStackCollector(true));

  private final File destinationFolder = new File(System.getProperty("spf4j.junit.destinationFolder",
          "target/junit-ssdump"));
  {
    if (!destinationFolder.mkdirs() && !destinationFolder.canWrite()) {
      throw new ExceptionInInitializerError("Unable to write to " + destinationFolder);
    }
  }

  @Override
  public void testFailure(final Failure failure)
          throws IOException, InterruptedException, ExecutionException, TimeoutException {
    sampler.stop();
    File dumpToFile = sampler.dumpToFile(new File(destinationFolder, failure.getTestHeader() + ".ssdump"));
    if (dumpToFile != null) {
      System.out.print("Profile saved to " + dumpToFile);
    }
  }

  @Override
  public void testFinished(final Description description)
          throws IOException, InterruptedException, ExecutionException, TimeoutException {
    sampler.stop();
    File dumpToFile = sampler.dumpToFile(new File(destinationFolder, description.getDisplayName() + ".ssdump"));
    if (dumpToFile != null) {
      System.out.print("Profile saved to " + dumpToFile);
    }
  }

  @Override
  public void testStarted(final Description description) {
    sampler.start();
  }

  @Override
  public String toString() {
    return "Spf4jRunListener{" + "sampler=" + sampler + '}';
  }

}
