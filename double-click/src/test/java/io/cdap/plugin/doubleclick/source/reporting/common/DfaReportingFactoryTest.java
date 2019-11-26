package io.cdap.plugin.doubleclick.source.reporting.common;

import com.google.api.services.dfareporting.Dfareporting;
import io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSource;
import org.junit.Assert;
import org.junit.Test;

public class DfaReportingFactoryTest {

  @Test
  public void shouldInitializeDFAReporting() {
    //when
    Dfareporting instance = DfaReportingFactory.getInstance();

    //then
    Assert.assertNotNull(instance);
    Assert.assertEquals(DoubleClickReportingBatchSource.NAME, instance.getApplicationName());
  }
}
