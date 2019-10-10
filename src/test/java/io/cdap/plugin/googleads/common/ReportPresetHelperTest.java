package io.cdap.plugin.googleads.common;

import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    List<String> fields = new ArrayList<>();
    fields.add("AccountCurrencyCode");
    fields.add("AccountDescriptiveName");
    fields.add("AccountTimeZone");

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
      System.out.println(String.format("'%s' reportFields is empty", name));
//      Assert.fail(String.format("'%s' reportFields is empty", name));
    }
    Set<String> reportFieldsSet = new HashSet<>(reportFields);
    if (reportFieldsSet.size() != reportFields.size()) {
      System.out.println(String.format("'%s' reportFields contains duplicates", name));
//      Assert.fail(String.format("'%s' reportFields contains duplicates", name));
    }

    ReportDefinitionField[] reportDefinitionFields;
    try {
      GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
      reportDefinitionFields = googleAdsHelper.getReportDefinitionFields(config, preset.getType().value());
    } catch (OAuthException | ValidationException e) {
      Assert.fail("invalid credentials");
      return;
    }
    Map<String , ReportDefinitionField> reportFieldsMap = new HashMap<>();
    for (ReportDefinitionField reportDefinitionField : reportDefinitionFields) {
      if (reportFieldsSet.contains(reportDefinitionField.getFieldName())) {
        reportFieldsMap.put(reportDefinitionField.getFieldName(), reportDefinitionField);
        if (reportDefinitionField.getExclusiveFields() != null) {
          for (String exclusive : reportDefinitionField.getExclusiveFields()) {
            if (reportFieldsMap.containsKey(exclusive)) {
              System.out.println(String.format("'%s' Field '%s' conflicts with field '%s'",
                                               name,
                                               reportDefinitionField.getFieldName(),
                                               exclusive));
//              Assert.fail(String.format("'%s' Field '%s' conflicts with field '%s'",
//                                        name,
//                                        reportDefinitionField.getFieldName(),
//                                        exclusive));
            }
          }
        }
      }
    }
    reportFieldsSet.removeAll(reportFieldsMap.keySet());
    if (!reportFieldsSet.isEmpty()) {
      for (String field : reportFieldsSet) {
        //Assert.fail(String.format("'%s' Invalid Field '%s'", name, field));
        System.out.println(String.format("'%s' Invalid Field '%s'", name, field));
      }
    }
  }
}
