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

import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.ReferencePluginConfig;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides all required configuration for reading Google AdWords reports
 */
public class GoogleAdsBatchSourceConfig extends ReferencePluginConfig {

  public GoogleAdsBatchSourceConfig(String referenceName) {
    super(referenceName);
  }

  @Description("Refresh token")
  @Macro
  public String refreshToken;

  @Description("Client ID")
  @Macro
  public String clientId;

  @Description("Client Secret")
  @Macro
  public String clientSecret;

  @Description("Developer token")
  @Macro
  public String developerToken;

  @Description("Customer ID")
  @Macro
  public String clientCustomerId;

  @Description("Start Date")
  @Macro
  protected String startDate;

  @Description("End Date")
  @Macro
  protected String endDate;

  @Description("Include Report Summary")
  @Macro
  public Boolean includeReportSummary;

  @Description("Use Raw Enum Values")
  @Macro
  public Boolean useRawEnumValues;

  @Description("Include Zero Impressions")
  @Macro
  public Boolean includeZeroImpressions;

  @Description("Report type")
  @Macro
  protected String reportType;

  @Description("Fields")
  @Macro
  protected String reportFields;

  public List<String> getReportFields() {
    List<String> fields = Arrays.asList(reportFields.split(","));
    if (fields.size()==1 && fields.contains("ALL")){
      try {
        return new GoogleAdsHelper().getNoConflictFields(this);
      } catch (OAuthException | ValidationException | RemoteException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return fields;
  }

  public void validate(FailureCollector failureCollector) {
    GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
    validateAuthorisation(failureCollector, googleAdsHelper);
    validateDateRange(failureCollector);
    validateReportTypeAndFields(failureCollector, googleAdsHelper);
  }

  protected void validateAuthorisation(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    try {
      googleAdsHelper.getAdWordsSession(this);
    } catch (OAuthException | ValidationException e) {
      failureCollector.addFailure(e.getMessage(), "Enter valid credentials");
    }
  }

  protected void validateReportTypeAndFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    ReportDefinitionReportType reportDefinitionReportType = null;
    try {
      reportDefinitionReportType = getReportType();
    } catch (IllegalArgumentException ex) {
      failureCollector.addFailure("Invalid reportDefinitionReportType", "Enter valid reportDefinitionReportType").withConfigProperty(reportType);
    }
    if (reportDefinitionReportType != null){
      validateFields(failureCollector, googleAdsHelper);
    }
  }

  protected void validateFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    try {
      List<String> fields = googleAdsHelper.getAllFields(this);
      if (!fields.containsAll(getReportFields())){
        failureCollector.addFailure("Invalid reportFields", "Enter valid reportFields according to record type").withConfigProperty(reportFields);
      }
    } catch (OAuthException | ValidationException | RemoteException ignored) {
    }
  }

  protected void validateDateRange(FailureCollector failureCollector) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyDDmm");
    Date startDate = null;
    Date endDate = null;
    try {
      startDate = simpleDateFormat.parse(getStartDate());
    } catch (ParseException e) {
      failureCollector.addFailure("Invalid startDate format.", "Enter valid YYYYMMDD date format.").withConfigProperty(this.startDate);
    }
    try {
      endDate = simpleDateFormat.parse(getEndDate());
    } catch (ParseException e) {
      failureCollector.addFailure("Invalid endDate format.", "Enter valid YYYYMMDD date format.").withConfigProperty(this.endDate);
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

  public String getDate(String date) {

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
    return ReportDefinitionReportType.fromValue(reportType);
  }
}
