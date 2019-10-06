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

import java.rmi.RemoteException;
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
  @Description("Refresh token")
  @Macro
  public String refreshToken;

  @Name(CLIENT_ID)
  @Description("Client ID")
  @Macro
  public String clientId;

  @Name(CLIENT_SECRET)
  @Description("Client Secret")
  @Macro
  public String clientSecret;

  @Name(DEVELOPER_TOKEN)
  @Description("Developer token")
  @Macro
  public String developerToken;

  @Name(CLIENT_CUSTOMER_ID)
  @Description("Customer ID")
  @Macro
  public String clientCustomerId;

  @Name(START_DATE)
  @Description("Start Date")
  @Macro
  protected String startDate;

  @Name(END_DATE)
  @Description("End Date")
  @Macro
  protected String endDate;

  @Name(INCLUDE_REPORT_SUMMARY)
  @Description("Include Report Summary")
  @Macro
  public Boolean includeReportSummary;

  @Name(USE_RAW_ENUM_VALUES)
  @Description("Use Raw Enum Values")
  @Macro
  public Boolean useRawEnumValues;

  @Name(INCLUDE_ZERO_IMPRESSIONS)
  @Description("Include Zero Impressions")
  @Macro
  public Boolean includeZeroImpressions;

  @Name(REPORT_TYPE)
  @Description("Report type")
  @Macro
  protected String reportType;

  @Name(REPORT_FIELDS)
  @Description("Fields")
  @Macro
  @Nullable
  protected String reportFields;

  public List<String> getReportFields() {
    ReportPresetHelper presetHelper = new ReportPresetHelper();
    if (presetHelper.getReportPresets().containsKey(reportType)) {
      return presetHelper.getReportPreset(reportType).getFields();
    }
    List<String> fields = Arrays.asList(reportFields.split(","));
    return fields;
  }

  public void validate(FailureCollector failureCollector) {
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

  protected void validateReportTypeAndFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    if (containsMacro(REPORT_TYPE)) {
      return;
    }
    ReportDefinitionReportType reportDefinitionReportType = null;
    try {
      reportDefinitionReportType = getReportType();
    } catch (IllegalArgumentException ex) {
      failureCollector.addFailure("Invalid reportDefinitionReportType",
                                  "Enter valid reportDefinitionReportType")
        .withConfigProperty(REPORT_TYPE);
    }
    if (reportDefinitionReportType != null) {
      validateFields(failureCollector, googleAdsHelper);
    }
  }

  protected void validateFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    if (containsMacro(REPORT_FIELDS)) {
      return;
    }
    List<String> reportFields = getReportFields();
    Set<String> reportFieldsSet = new HashSet<>(reportFields);
    if (reportFieldsSet.size() != reportFields.size()) {
      failureCollector.addFailure("reportFields contains duplicates",
                                  "Enter valid reportFields according to record type")
        .withConfigProperty(REPORT_FIELDS);
    }

    ReportDefinitionField[] reportDefinitionFields;
    try {
      reportDefinitionFields = googleAdsHelper.getReportDefinitionFields(this);
    } catch (OAuthException | ValidationException | RemoteException ignored) {
      return;
    }
    Map<String , ReportDefinitionField> reportFieldsMap = new HashMap<>();
    for (ReportDefinitionField reportDefinitionField : reportDefinitionFields) {
      if (reportFieldsSet.contains(reportDefinitionField.getFieldName())) {
        reportFieldsMap.put(reportDefinitionField.getFieldName(), reportDefinitionField);
        if (reportDefinitionField.getExclusiveFields() != null) {
          for (String exclusive : reportDefinitionField.getExclusiveFields()) {
            if (reportFieldsMap.containsKey(exclusive)) {
              failureCollector.addFailure(String.format("Field %s conflict with %s",
                                                        reportDefinitionField.getFieldName(),
                                                        exclusive),
                                          "Enter valid reportFields according to record type")
                .withConfigProperty(REPORT_FIELDS);
            }
          }
        }
      }
    }
    reportFieldsSet.removeAll(reportFieldsMap.keySet());
    if (!reportFieldsSet.isEmpty()) {
      for (String field : reportFieldsSet) {
        failureCollector.addFailure(String.format("Invalid Field %s", field),
                                    "Enter valid reportFields according to record type")
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
      failureCollector.addFailure("Invalid startDate format.", "Enter valid YYYYMMDD date format.")
        .withConfigProperty(START_DATE);
    }
    try {
      endDate = simpleDateFormat.parse(getEndDate());
    } catch (ParseException e) {
      failureCollector.addFailure("Invalid endDate format.", "Enter valid YYYYMMDD date format.")
        .withConfigProperty(END_DATE);
    }
    if (startDate != null &&
      endDate != null &&
      startDate.after(endDate)) {
      failureCollector.addFailure("startDate must be earlier than endDate.", "Enter valid date.");
    }
  }

  public Schema getSchema() {
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

  public ReportDefinitionReportType getReportType() {
    ReportPresetHelper presetHelper = new ReportPresetHelper();
    if (presetHelper.getReportPresets().containsKey(reportType)) {
      return presetHelper.getReportPreset(reportType).getType();
    }
    return ReportDefinitionReportType.fromValue(reportType);
  }
}
