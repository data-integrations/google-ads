package io.cdap.plugin.googleads.source.batch;

import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import org.junit.Assert;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class GoogleAdsBatchSourceConfigTest {


  public static GoogleAdsBatchSourceConfig getTestConfig(String refreshToken, String clientId, String clientSecret,
                                                         String developerToken, String clientCustomerId) {
    GoogleAdsBatchSourceConfig config = spy(new GoogleAdsBatchSourceConfig("test"));
    List<String> fields = new ArrayList<>();
    fields.add("AccountCurrencyCode");
    fields.add("AccountDescriptiveName");
    fields.add("AccountTimeZone");
    doReturn(ReportDefinitionReportType.KEYWORDS_PERFORMANCE_REPORT).when(config).getReportType();

    doReturn(fields).when(config).getReportFields();
    config.startDate = "LAST_30_DAYS";
    config.endDate = "TODAY";
    config.refreshToken = refreshToken;
    config.clientId = clientId;
    config.clientSecret = clientSecret;
    config.developerToken = developerToken;
    config.clientCustomerId = clientCustomerId;
    config.includeReportSummary = true;
    config.useRawEnumValues = true;
    config.includeZeroImpressions = true;
    return config;
  }

  @Test
  public void testValidateAuthorisation() throws OAuthException, ValidationException {
    //setup mocks
    GoogleAdsBatchSourceConfig config = new GoogleAdsBatchSourceConfig("test");
    MockFailureCollector failureCollector = new MockFailureCollector();
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);
    doReturn(null).when(googleAdsHelper).getAdWordsSession(config);

    //test
    config.validateAuthorisation(failureCollector, googleAdsHelper);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks
    doThrow(new OAuthException("Error occurred")).when(googleAdsHelper).getAdWordsSession(config);
    //test failure
    config.validateAuthorisation(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());
    //setup mocks failure
    doThrow(new ValidationException("Error occurred", "invalid")).when(googleAdsHelper).getAdWordsSession(config);
    //test
    config.validateAuthorisation(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(2, failureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateDateRange() {
    //setup mocks
    GoogleAdsBatchSourceConfig config = new GoogleAdsBatchSourceConfig("test");
    config.startDate = "20190302";
    config.endDate = "20190305";
    MockFailureCollector failureCollector = new MockFailureCollector();
    //test
    config.validateDateRange(failureCollector);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks failure
    config.startDate = "LAST_30_DAYS";
    config.endDate = "TODAY";
    //test
    config.validateDateRange(failureCollector);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks failure
    config.startDate = "20190303";
    config.endDate = "20190301";
    //test
    config.validateDateRange(failureCollector);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());

    //setup mocks failure
    config.startDate = "201s90303";
    config.endDate = "20190307";
    //test
    config.validateDateRange(failureCollector);
    //assert
    Assert.assertEquals(2, failureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateReportTypeAndFields() throws OAuthException, RemoteException, ValidationException {
    //setup mocks
    GoogleAdsBatchSourceConfig config = spy(new GoogleAdsBatchSourceConfig("test"));
    config.reportType = "KEYWORDS_PERFORMANCE_REPORT";
    config.reportFields = "test1,test2";
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);

    ReportDefinitionField reportDefinitionField1 = new ReportDefinitionField();
    reportDefinitionField1.setFieldName("test1");
    ReportDefinitionField reportDefinitionField2 = new ReportDefinitionField();
    reportDefinitionField2.setFieldName("test2");
    ReportDefinitionField reportDefinitionField3 = new ReportDefinitionField();
    reportDefinitionField3.setFieldName("test3");
    reportDefinitionField3.setExclusiveFields(new String[]{"test4"});
    ReportDefinitionField reportDefinitionField4 = new ReportDefinitionField();
    reportDefinitionField4.setFieldName("test4");
    reportDefinitionField4.setExclusiveFields(new String[]{"test3"});
    ReportDefinitionField[] fields = new ReportDefinitionField[]{reportDefinitionField1,
      reportDefinitionField2,
      reportDefinitionField3,
      reportDefinitionField4};

    doReturn(fields).when(googleAdsHelper).getReportDefinitionFields(config);
    MockFailureCollector failureCollector = new MockFailureCollector();
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks failure
    config.reportFields = "test1,test5";
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());
    Assert.assertEquals("Invalid Field test5", failureCollector.getValidationFailures().get(0).getMessage());
    failureCollector.getValidationFailures().clear();


    //setup mocks failure
    config.reportFields = "test3,test4,test4,invalid"; //valid
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(3, failureCollector.getValidationFailures().size());

    Assert.assertEquals("reportFields contains duplicates",
                        failureCollector.getValidationFailures().get(0).getMessage());
    Assert.assertEquals("Field test4 conflict with test3",
                        failureCollector.getValidationFailures().get(1).getMessage());
    Assert.assertEquals("Invalid Field invalid",
                        failureCollector.getValidationFailures().get(2).getMessage());
    failureCollector.getValidationFailures().clear();

    //setup mocks failure
    config.reportType = "invalid";
    config.reportFields = "test1,test2"; //valid
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());

    Assert.assertEquals("Invalid reportDefinitionReportType",
                        failureCollector.getValidationFailures().get(0).getMessage());
    failureCollector.getValidationFailures().clear();
  }

  @Test
  public void testGetSchema() throws OAuthException, RemoteException, ValidationException {
    //setup mocks
    GoogleAdsBatchSourceConfig config = new GoogleAdsBatchSourceConfig("test");
    config.reportFields = "test1,test2";
    //test
    Schema schema = config.getSchema();
    //assert
    Schema expectedSchema = Schema.recordOf("TestSchema",
      Schema.Field.of("test1", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
      Schema.Field.of("test2", Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    Assert.assertTrue(schema.isCompatible(expectedSchema));
  }
}
