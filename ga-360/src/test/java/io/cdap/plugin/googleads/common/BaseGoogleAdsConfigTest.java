/*
 * Copyright © 2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.plugin.googleads.common;

import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.googleads.source.single.BatchSourceGoogleAdsConfig;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class BaseGoogleAdsConfigTest {


  @Test
  public void testValidateAuthorisation() throws OAuthException, ValidationException {
    //setup mocks
    BatchSourceGoogleAdsConfig config = new BatchSourceGoogleAdsConfig("test");
    MockFailureCollector failureCollector = new MockFailureCollector();
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);
    doReturn(null).when(googleAdsHelper).getAdWordsSession(config);

    //test
    config.validateAuthorization(failureCollector, googleAdsHelper);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks
    doThrow(new OAuthException("Error occurred")).when(googleAdsHelper).getAdWordsSession(config);
    //test failure
    config.validateAuthorization(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());
    //setup mocks failure
    doThrow(new ValidationException("Error occurred", "invalid")).when(googleAdsHelper).getAdWordsSession(config);
    //test
    config.validateAuthorization(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(2, failureCollector.getValidationFailures().size());
  }

  @Test
  public void testValidateDateRange() {
    //setup mocks
    BatchSourceGoogleAdsConfig config = new BatchSourceGoogleAdsConfig("test");
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
}
