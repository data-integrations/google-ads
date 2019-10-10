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
package io.cdap.plugin.googleads.source.single;

import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.googleads.common.BaseGoogleAdsConfig;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import io.cdap.plugin.googleads.common.ReportPresetHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Provides all required configuration for reading Google AdWords report
 */
public class BatchSourceGoogleAdsConfig extends BaseGoogleAdsConfig {

  public BatchSourceGoogleAdsConfig(String referenceName) {
    super(referenceName);
  }

  public static final String REPORT_TYPE = "reportType";
  public static final String REPORT_FIELDS = "reportFields";

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
    return Arrays.asList(reportFields.split(","));
  }

  @Override
  public void validate(FailureCollector failureCollector) {
    super.validate(failureCollector);
    GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
    validateReportTypeAndFields(failureCollector, googleAdsHelper);
  }

  protected void validateReportTypeAndFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
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
    } catch (IOException e) {
      failureCollector.addFailure(String.format("Can`t evaluate repo type from preset :%s", e.getMessage()),
                                  null).withConfigProperty(REPORT_TYPE);
      return;
    }
    validateFields(failureCollector, googleAdsHelper);
  }

  protected void validateFields(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    if (containsMacro(REPORT_FIELDS)) {
      return;
    }
    List<String> reportFields = null;
    try {
      reportFields = getReportFields();
    } catch (IOException e) {
      failureCollector.addFailure(String.format("Can`t evaluate repo fields from preset :%s", e.getMessage()),
                                  null).withConfigProperty(REPORT_TYPE);
      return;
    }
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
    String reportType = null;
    try {
      reportType = getReportType().value();
    } catch (IOException e) {
      failureCollector.addFailure(String.format("Can`t evaluate repo type from preset :%s", e.getMessage()),
                                  null).withConfigProperty(REPORT_TYPE);
      return;
    }
    try {
      reportDefinitionFields = googleAdsHelper.getReportDefinitionFields(this, reportType);
    } catch (OAuthException | ValidationException e) {
      failureCollector.addFailure(e.getMessage(), "Enter valid credentials");
      return;
    } catch (IOException e) {
      failureCollector.addFailure(String.format("Exception while downloading report definition :%s", e.getMessage()),
                                  null);
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

  public Schema getSchema() throws IOException {
    Set<Schema.Field> schemaFields = new HashSet<>();
    for (String name : getReportFields()) {
      schemaFields.add(Schema.Field.of(name, Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    }

    return Schema.recordOf(
      "GoogleAdsRecords",
      schemaFields);
  }

  public ReportDefinitionReportType getReportType() throws IOException {
    ReportPresetHelper presetHelper = new ReportPresetHelper();
    if (presetHelper.getReportPresets().containsKey(reportType)) {
      return presetHelper.getReportPreset(reportType).getType();
    }
    return ReportDefinitionReportType.fromValue(reportType);
  }
}
