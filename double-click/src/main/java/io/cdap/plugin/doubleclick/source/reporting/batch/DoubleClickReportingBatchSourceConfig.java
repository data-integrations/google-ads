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
package io.cdap.plugin.doubleclick.source.reporting.batch;

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.ReferencePluginConfig;
import io.cdap.plugin.doubleclick.source.reporting.common.SchemaBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Base configuration for Double Click Campaign Manager Reporting sources.
 */
public class DoubleClickReportingBatchSourceConfig extends ReferencePluginConfig {

  public static final String APPLICATION_ID = "applicationId";
  public static final String ACCESS_TOKEN = "accessToken";
  public static final String USE_EXISTING_REPORT = "useExistingReport";
  public static final String REPORT_ID = "reportId";
  public static final String REPORT_NAME = "reportName";
  public static final String REPORT_TYPE = "reportType";
  public static final String DATE_RANGE = "dateRange";
  public static final String METRICS = "metricsList";
  public static final String DIMENSIONS = "dimensionsList";
  public static final String ADVANCED_PROPERTIES = "advancedProperties";

  @Name(APPLICATION_ID)
  @Description("The application ID")
  @Macro
  protected String applicationId;

  @Name(ACCESS_TOKEN)
  @Description("Access token to access Double Click Campaign Manager reporting API")
  @Macro
  protected String accessToken;

  @Name(USE_EXISTING_REPORT)
  @Description("Indicates if the plugin should use an existing report or it should generate a new report")
  @Macro
  protected String useExistingReport;

  @Name(REPORT_ID)
  @Description("The report ID to fetch the data for")
  @Nullable
  @Macro
  protected String reportId;

  @Name(REPORT_NAME)
  @Description("The report name to create")
  @Nullable
  @Macro
  protected String reportName;

  @Name(REPORT_TYPE)
  @Description("The report type to create")
  @Nullable
  @Macro
  protected String reportType;

  @Name(DATE_RANGE)
  @Description("The date range to create report")
  @Nullable
  @Macro
  protected String dateRange;

  @Name(METRICS)
  @Description("A list of metrics based on the report type")
  @Nullable
  @Macro
  protected String metricsList;

  @Name(DIMENSIONS)
  @Description("A list of dimensions based on the report type")
  @Nullable
  @Macro
  protected String dimensionsList;

  @Name(ADVANCED_PROPERTIES)
  @Description("A set of advanced properties to include in the report criteria, based on the selected report type")
  @Nullable
  @Macro
  protected String advancedProperties;

  private transient Schema schema = null;

  public DoubleClickReportingBatchSourceConfig(String referenceName) {
    super(referenceName);
  }

  public Schema getSchema() {
    if (schema == null) {
      schema = SchemaBuilder.buildSchema(getMetricsList(), getDimensionsList());
    }
    return schema;
  }

  public Long getApplicationId() {
    return Long.valueOf(applicationId);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public Boolean isUseExistingReport() {
    return Boolean.parseBoolean(useExistingReport);
  }

  @Nullable
  public Long getReportId() {
    if (Objects.isNull(reportId)) {
      return null;
    }
    return Long.valueOf(reportId);
  }

  @Nullable
  public String getReportName() {
    return reportName;
  }

  @Nullable
  public String getReportType() {
    return reportType;
  }

  @Nullable
  public String getDateRange() {
    return dateRange;
  }

  @Nullable
  public String getAdvancedProperties() {
    return advancedProperties;
  }

  public List<String> getMetricsList() {
    if (!Strings.isNullOrEmpty(metricsList)) {
      return Arrays.asList(metricsList.split(","));
    } else {
      return Collections.emptyList();
    }
  }

  public List<String> getDimensionsList() {
    if (!Strings.isNullOrEmpty(dimensionsList)) {
      return Arrays.asList(dimensionsList.split(","));
    } else {
      return Collections.emptyList();
    }
  }

  public void validate(FailureCollector failureCollector) {
    if (!containsMacro(applicationId) && Strings.isNullOrEmpty(applicationId)) {
      failureCollector
          .addFailure(String.format("%s must be specified.", APPLICATION_ID), null)
          .withConfigProperty(APPLICATION_ID);
    }
    if (!containsMacro(accessToken) && Strings.isNullOrEmpty(accessToken)) {
      failureCollector
          .addFailure(String.format("%s must be specified.", ACCESS_TOKEN), null)
          .withConfigProperty(ACCESS_TOKEN);
    }
    if (!containsMacro(useExistingReport) && Strings.isNullOrEmpty(useExistingReport)) {
      failureCollector
        .addFailure(String.format("%s must be specified.", USE_EXISTING_REPORT), null)
        .withConfigProperty(USE_EXISTING_REPORT);
    }
    if (isUseExistingReport()) {
      if (!containsMacro(reportId) && Strings.isNullOrEmpty(reportId)) {
        failureCollector
          .addFailure(String.format("%s must be specified.", REPORT_ID), null)
          .withConfigProperty(REPORT_ID);
      }
    } else {
      if (!containsMacro(reportName) && Strings.isNullOrEmpty(reportName)) {
        failureCollector
          .addFailure(String.format("%s must be specified.", REPORT_NAME), null)
          .withConfigProperty(REPORT_NAME);
      }
      if (!containsMacro(reportType) && Strings.isNullOrEmpty(reportType)) {
        failureCollector
          .addFailure(String.format("%s must be specified.", REPORT_TYPE), null)
          .withConfigProperty(REPORT_TYPE);
      }
      if (getMetricsList().isEmpty()) {
        failureCollector
          .addFailure(String.format("%s or must be specified.", METRICS), null)
          .withConfigProperty(METRICS);
      }
    }
  }
}
