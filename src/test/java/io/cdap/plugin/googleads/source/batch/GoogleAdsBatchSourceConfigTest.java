package io.cdap.plugin.googleads.source.batch;

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
    GoogleAdsBatchSourceConfig config = new GoogleAdsBatchSourceConfig("test");
    config.reportType = "KEYWORDS_PERFORMANCE_REPORT";
    config.reportFields = "test1,test2";
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);
    List<String> fields = new ArrayList<>();
    fields.add("test1");
    fields.add("test2");
    fields.add("test3");

    doReturn(fields).when(googleAdsHelper).getAllFields(config);
    MockFailureCollector failureCollector = new MockFailureCollector();
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks failure
    config.reportFields = "test1,test4";
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());

    //setup mocks failure
    config.reportType = "invalid";
    config.reportFields = "test1,test2"; //valid
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(2, failureCollector.getValidationFailures().size());
  }

  @Test
  public void testGetSchema() throws OAuthException, RemoteException, ValidationException {
    //setup mocks
    GoogleAdsBatchSourceConfig config = new GoogleAdsBatchSourceConfig("test");
    config.reportFields = "test1,test2";

    MockFailureCollector failureCollector = new MockFailureCollector();
    //test
    Schema schema = config.getSchema();
    //assert
    Schema expectedSchema = Schema.recordOf("TestSchema",
      Schema.Field.of("test1", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
      Schema.Field.of("test2", Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    Assert.assertTrue(schema.isCompatible(expectedSchema));
  }
}