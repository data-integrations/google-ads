package io.cdap.plugin.googleads.common;

import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.spy;

public class ReportPresetHelperTest {

  private static String refreshToken;
  private static String clientId;
  private static String clientSecret;
  private static String developerToken;
  private static String clientCustomerId;

  @BeforeClass
  public static void setupTestClass() throws Exception {
    // initialize api
    refreshToken = System.getProperty("gads.refresh.token");
    if (Strings.isNullOrEmpty(refreshToken)) {
      throw new IllegalArgumentException("gads.refresh.token system property must not be empty.");
    }
    clientId = System.getProperty("gads.client.id");
    if (Strings.isNullOrEmpty(clientId)) {
      throw new IllegalArgumentException("gads.client.id system property must not be empty.");
    }
    clientSecret = System.getProperty("gads.client.secret");
    if (Strings.isNullOrEmpty(clientSecret)) {
      throw new IllegalArgumentException("gads.client.secret system property must not be empty.");
    }
    developerToken = System.getProperty("gads.developer.token");
    if (Strings.isNullOrEmpty(developerToken)) {
      throw new IllegalArgumentException("gads.developer.token system property must not be empty.");
    }
    clientCustomerId = System.getProperty("gads.customer.id");
    if (Strings.isNullOrEmpty(clientCustomerId)) {
      throw new IllegalArgumentException("gads.customer.id system property must not be empty.");
    }
  }


  @Test
  public void testPresets() throws Exception {
    //setup mocks
    BaseGoogleAdsConfig config = spy(new BaseGoogleAdsConfig("test"));

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

    ReportPresetHelper presetHelper = new ReportPresetHelper();
    for (Map.Entry<String, ReportPreset> entry : presetHelper.getReportPresets().entrySet()) {
      validatePreset(entry.getKey(), entry.getValue(), config);
    }
  }

  protected void validatePreset(String name, ReportPreset preset, BaseGoogleAdsConfig config) throws IOException {
    List<String> reportFields = preset.getFields();
    if (reportFields == null || reportFields.isEmpty()) {
      Assert.fail(String.format("'%s' reportFields is empty", name));
    }
    Set<String> reportFieldsSet = new HashSet<>(reportFields);
    if (reportFieldsSet.size() != reportFields.size()) {
      Assert.fail(String.format("'%s' reportFields contains duplicates", name));
    }
  }
}
