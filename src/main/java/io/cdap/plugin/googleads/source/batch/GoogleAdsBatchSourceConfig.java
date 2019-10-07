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
package io.cdap.plugin.googleads.source.batch;

import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.ReferencePluginConfig;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import io.cdap.plugin.googleads.common.ReportPresetHelper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Provides all required configuration for reading Google AdWords reports
 */
public class GoogleAdsBatchSourceConfig extends ReferencePluginConfig {

  public GoogleAdsBatchSourceConfig(String referenceName) {
    super(referenceName);
  }

  public static final String REFRESH_TOKEN = "refreshToken";
  public static final String CLIENT_ID = "clientId";
  public static final String CLIENT_SECRET = "clientSecret";
  public static final String DEVELOPER_TOKEN = "developerToken";
  public static final String CLIENT_CUSTOMER_ID = "clientCustomerId";
  public static final String START_DATE = "startDate";
  public static final String END_DATE = "endDate";
  public static final String INCLUDE_REPORT_SUMMARY = "includeReportSummary";
  public static final String USE_RAW_ENUM_VALUES = "useRawEnumValues";
  public static final String INCLUDE_ZERO_IMPRESSIONS = "includeZeroImpressions";
  public static final String REPORT_TYPE = "reportType";
  public static final String REPORT_FIELDS = "reportFields";

  @Name(REFRESH_TOKEN)
  @Description("Authorization to download the report")
  @Macro
  public String refreshToken;

  @Name(CLIENT_ID)
  @Description("OAuth 2.0 client ID from console")
  @Macro
  public String clientId;

  @Name(CLIENT_SECRET)
  @Description("OAuth 2.0 client Secret from console")
  @Macro
  public String clientSecret;

  @Name(DEVELOPER_TOKEN)
  @Description("Developer token consisting of unique string")
  @Macro
  public String developerToken;

  @Name(CLIENT_CUSTOMER_ID)
  @Description("Customer ID of the client account")
  @Macro
  public String clientCustomerId;

  @Name(START_DATE)
  @Description("Start date for the report data. YYYYMMDD format." +
    " Allow \"LAST_30_DAYS\", \"LAST_60_DAYS\" and \"LAST_90_DAYS\" options")
  @Macro
  protected String startDate;

  @Name(END_DATE)
  @Description("End date for the report data. YYYYMMDD format. Allow \"TODAY\" option")
  @Macro
  protected String endDate;

  @Name(INCLUDE_REPORT_SUMMARY)
  @Description("Specifies whether report include a summary row containing the report totals.")
  @Macro
  public Boolean includeReportSummary;

  @Name(USE_RAW_ENUM_VALUES)
  @Description("Specifies whether returned format to be the actual enum value.")
  @Macro
  public Boolean useRawEnumValues;

  @Name(INCLUDE_ZERO_IMPRESSIONS)
  @Description("Specifies whether report include rows where all specified metric fields are zero")
  @Macro
  public Boolean includeZeroImpressions;

  @Name(REPORT_TYPE)
  @Description("Google Ads report type to retrieve.")
  @Macro
  protected String reportType;

  @Name(REPORT_FIELDS)
  @Description("List of fields to pull from report. Fields from preset used in case of preset Report type selected")
  @Macro
  @Nullable
  protected String reportFields;

  public List<String> getReportFields() throws IOException {
    ReportPresetHelper presetHelper = new ReportPresetHelper();
    if (presetHelper.getReportPresets().containsKey(reportType)) {
      return presetHelper.getReportPreset(reportType).getFields();
    }
    List<String> fields = Arrays.asList(reportFields.split(","));
    return fields;
  }

  public void validate(FailureCollector failureCollector) throws IOException {
    GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
    validateAuthorisation(failureCollector, googleAdsHelper);
    validateDateRange(failureCollector);
    validateReportTypeAndFields(failureCollector, googleAdsHelper);
  }

  protected void validateAuthorisation(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    if (containsMacro(REFRESH_TOKEN)
      || containsMacro(CLIENT_ID)
      || containsMacro(CLIENT_SECRET)
      || containsMacro(DEVELOPER_TOKEN)
      || containsMacro(CLIENT_CUSTOMER_ID)) {
      return;
    }
    try {
      googleAdsHelper.getAdWordsSession(this);
    } catch (OAuthException | ValidationException e) {
      failureCollector.addFailure(e.getMessage(), "Enter valid credentials");
    }
  }

  protected void validateReportTypeAndFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper)
    throws IOException {
    if (containsMacro(REPORT_TYPE)) {
      return;
    }
    ReportDefinitionReportType reportDefinitionReportType = null;
    try {
      reportDefinitionReportType = getReportType();
    } catch (IllegalArgumentException ex) {
      failureCollector.addFailure(String.format("reportType '%s' is not a valid report type", reportType),
                                  null).withConfigProperty(REPORT_TYPE);
      return;
    }
    validateFields(failureCollector, googleAdsHelper);
  }

  protected void validateFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper)
    throws IOException {
    if (containsMacro(REPORT_FIELDS)) {
      return;
    }
    List<String> reportFields = getReportFields();
    if (reportFields == null || reportFields.isEmpty()) {
      failureCollector.addFailure("reportFields is empty",
                                  "Enter valid reportFields according to report type or select preset report type")
        .withConfigProperty(REPORT_FIELDS);
    }
    Set<String> reportFieldsSet = new HashSet<>(reportFields);
    if (reportFieldsSet.size() != reportFields.size()) {
      failureCollector.addFailure("reportFields contains duplicates", null)
        .withConfigProperty(REPORT_FIELDS);
    }

    ReportDefinitionField[] reportDefinitionFields;
    try {
      reportDefinitionFields = googleAdsHelper.getReportDefinitionFields(this);
    } catch (OAuthException | ValidationException e) {
      failureCollector.addFailure(e.getMessage(), "Enter valid credentials");
      return;
    }
    Map<String , ReportDefinitionField> reportFieldsMap = new HashMap<>();
    for (ReportDefinitionField reportDefinitionField : reportDefinitionFields) {
      if (reportFieldsSet.contains(reportDefinitionField.getFieldName())) {
        reportFieldsMap.put(reportDefinitionField.getFieldName(), reportDefinitionField);
        if (reportDefinitionField.getExclusiveFields() != null) {
          for (String exclusive : reportDefinitionField.getExclusiveFields()) {
            if (reportFieldsMap.containsKey(exclusive)) {
              failureCollector.addFailure(String.format("Field '%s' conflicts with field '%s'",
                                                        reportDefinitionField.getFieldName(),
                                                        exclusive), null)
                .withConfigProperty(REPORT_FIELDS);
            }
          }
        }
      }
    }
    reportFieldsSet.removeAll(reportFieldsMap.keySet());
    if (!reportFieldsSet.isEmpty()) {
      for (String field : reportFieldsSet) {
        failureCollector.addFailure(String.format("Invalid Field '%s'", field), null)
          .withConfigProperty(REPORT_FIELDS);
      }
    }
  }

  protected void validateDateRange(FailureCollector failureCollector) {
    if (containsMacro(START_DATE)
      || containsMacro(END_DATE)) {
      return;
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyDDmm");
    Date startDate = null;
    Date endDate = null;
    try {
      startDate = simpleDateFormat.parse(getStartDate());
    } catch (ParseException e) {
      failureCollector.addFailure("Invalid startDate format.", "Use YYYYMMDD date format.")
        .withConfigProperty(START_DATE);
    }
    try {
      endDate = simpleDateFormat.parse(getEndDate());
    } catch (ParseException e) {
      failureCollector.addFailure("Invalid endDate format.", "Use YYYYMMDD date format.")
        .withConfigProperty(END_DATE);
    }
    if (startDate != null &&
      endDate != null &&
      startDate.after(endDate)) {
      failureCollector.addFailure("startDate must be earlier than endDate.", "Enter valid date.");
    }
  }

  public Schema getSchema() throws IOException {
    Set<Schema.Field> schemaFields = new HashSet<>();
    for (String name : getReportFields()) {
      schemaFields.add(Schema.Field.of(name, Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    }

    return Schema.recordOf(
      "GoogleAdsRecords",
      schemaFields);
  }

  public String getStartDate() {
    return getDate(startDate);
  }

  public String getEndDate() {
    return getDate(endDate);
  }

  private String getDate(String date) {

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    Date today = new Date();
    if (date.equalsIgnoreCase("TODAY")) {
      return dateFormat.format(today);
    }
    Calendar cal = new GregorianCalendar();
    cal.setTime(today);
    if (date.equalsIgnoreCase("LAST_30_DAYS")) {
      cal.add(Calendar.DAY_OF_MONTH, -30);
      Date today30 = cal.getTime();
      return dateFormat.format(today30);
    }
    if (date.equalsIgnoreCase("LAST_60_DAYS")) {
      cal.add(Calendar.DAY_OF_MONTH, -60);
      Date today60 = cal.getTime();
      return dateFormat.format(today60);
    }
    if (date.equalsIgnoreCase("LAST_90_DAYS")) {
      cal.add(Calendar.DAY_OF_MONTH, -90);
      Date today90 = cal.getTime();
      return dateFormat.format(today90);
    }
    return date;
  }

  public ReportDefinitionReportType getReportType() throws IOException {
    ReportPresetHelper presetHelper = new ReportPresetHelper();
    if (presetHelper.getReportPresets().containsKey(reportType)) {
      return presetHelper.getReportPreset(reportType).getType();
    }
    return ReportDefinitionReportType.fromValue(reportType);
  }
}
