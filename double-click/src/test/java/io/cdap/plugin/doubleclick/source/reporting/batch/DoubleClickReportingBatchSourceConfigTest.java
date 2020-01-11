package io.cdap.plugin.doubleclick.source.reporting.batch;

import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSourceConfig.REPORT_NAME;
import static io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSourceConfig.REPORT_TYPE;

public class DoubleClickReportingBatchSourceConfigTest {

  private MockFailureCollector failureCollector;

  @Before
  public void setUp() {
    failureCollector = new MockFailureCollector();
  }

  @Test
  public void testValidateFieldsCaseCorrectFields() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    config.accessToken = "token";
    config.applicationId = "123";
    config.useExistingReport = "true";
    config.reportId = "123345";

    //when
    config.validate(failureCollector);

    //then
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());
  }

  @Test
  public void testValidateFieldsCaseEmptyFields() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");

    //when
    config.validate(failureCollector);

    //then
    Assert.assertEquals(6, failureCollector.getValidationFailures().size());
  }

  @Test
  public void testGetMetricsListCaseNotEmpty() {
    //given
    String givenMetric1 = "metric1";
    String givenMetric2 = "metric2";
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    config.metricsList = String.join(",", Arrays.asList(givenMetric1, givenMetric2));

    //when
    List<String> metricsList = config.getMetricsList();

    //then
    Assert.assertTrue(metricsList.contains(givenMetric1));
    Assert.assertTrue(metricsList.contains(givenMetric2));
  }

  @Test
  public void testGetMetricsListCaseEmpty() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    config.metricsList = "";

    //when
    List<String> metricsList = config.getMetricsList();

    //then
    Assert.assertTrue(metricsList.isEmpty());
  }

  @Test
  public void testGetMetricsListCaseNull() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");

    //when
    List<String> metricsList = config.getMetricsList();

    //then
    Assert.assertTrue(metricsList.isEmpty());
  }

  @Test
  public void testGetDimensionsListCaseNotEmpty() {
    //given
    String givenMetric1 = "dimension1";
    String givenMetric2 = "dimension2";
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    config.dimensionsList = String.join(",", Arrays.asList(givenMetric1, givenMetric2));

    //when
    List<String> dimensionsList = config.getDimensionsList();

    //then
    Assert.assertTrue(dimensionsList.contains(givenMetric1));
    Assert.assertTrue(dimensionsList.contains(givenMetric2));
  }

  @Test
  public void testGetDimensionsListCaseEmpty() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    config.dimensionsList = "";

    //when
    List<String> dimensionsList = config.getDimensionsList();

    //then
    Assert.assertTrue(dimensionsList.isEmpty());
  }

  @Test
  public void testGetDimensionsListCaseNull() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");

    //when
    List<String> dimensionsList = config.getDimensionsList();

    //then
    Assert.assertTrue(dimensionsList.isEmpty());
  }

  @Test
  public void testNewReportNameCaseCreateReport() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    MockFailureCollector failureCollector = new MockFailureCollector();
    config.useExistingReport = "false";
    config.reportType = "reportType";

    //when
    config.validate(failureCollector);

    boolean isStartDateFailure = failureCollector.getValidationFailures().stream()
      .map(ValidationFailure::getCauses)
      .flatMap(Collection::stream)
      .anyMatch(cause -> cause.getAttributes().containsValue(REPORT_NAME));

    //then
    Assert.assertTrue(isStartDateFailure);
  }

  @Test
  public void testNewReportTypeCaseCreateReport() {
    //given
    DoubleClickReportingBatchSourceConfig config = new DoubleClickReportingBatchSourceConfig("ref");
    MockFailureCollector failureCollector = new MockFailureCollector();
    config.useExistingReport = "false";
    config.reportName = "reportName";

    //when
    config.validate(failureCollector);

    boolean isStartDateFailure = failureCollector.getValidationFailures().stream()
      .map(ValidationFailure::getCauses)
      .flatMap(Collection::stream)
      .anyMatch(cause -> cause.getAttributes().containsValue(REPORT_TYPE));

    //then
    Assert.assertTrue(isStartDateFailure);
  }

}
