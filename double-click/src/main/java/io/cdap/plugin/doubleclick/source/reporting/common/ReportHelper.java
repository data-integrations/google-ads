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
package io.cdap.plugin.doubleclick.source.reporting.common;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.dfareporting.Dfareporting;
import com.google.api.services.dfareporting.model.DateRange;
import com.google.api.services.dfareporting.model.File;
import com.google.api.services.dfareporting.model.Report;
import com.google.api.services.dfareporting.model.SortedDimension;
import io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates batch request based on source configuration.
 */
public class ReportHelper {

  private static final Logger log = LoggerFactory.getLogger(ReportHelper.class);

  private static Dfareporting reporting;

  static {
    try {
      reporting = DfaReportingFactory.getInstance();
    } catch (Exception e) {
      log.error("Can't initialize DFA Reporting and Trafficking API service instance!", e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * Creates report.
   */
  public static Long createReport(DoubleClickReportingBatchSourceConfig config) throws IOException {
    // Create a date range to report on.
    DateRange dateRange = new DateRange();
    dateRange.setRelativeDateRange(config.getDateRange());

    // Create a dimensions to report on.
    List<String> dimensionsList = config.getDimensionsList();
    List<SortedDimension> dimensions = dimensionsList.stream()
      .map(dimension -> new SortedDimension()
        .setName(dimension))
      .collect(Collectors.toList());

    // Create the criteria for the report.
    Report.Criteria criteria = new Report.Criteria();
    criteria.setDateRange(dateRange);
    criteria.setMetricNames(config.getMetricsList());
    criteria.setDimensions(dimensions);

    // Create the report.
    Report report = new Report();
    report.setCriteria(criteria);
    report.setType(config.getReportType());
    report.setName(config.getReportName());

    // Insert the report.
    return DfaReportingFactory.getInstance()
      .reports()
      .insert(config.getApplicationId(), report)
      .setOauthToken(config.getAccessToken())
      .execute()
      .getId();
  }

  public static File runReport(DoubleClickReportingBatchSourceConfig config, Long reportId)
    throws IOException, InterruptedException {
    // Run the report.
    File file = reporting.reports()
      .run(config.getApplicationId(), reportId)
      .setOauthToken(config.getAccessToken())
      .execute();
    log.info("File with ID {} has been created", file.getId());

    // Wait for the report file to finish processing.
    // An exponential backoff policy is used to limit retries and conserve request quota.
    BackOff backOff =
      new ExponentialBackOff.Builder()
        .setInitialIntervalMillis(10 * 1000) // 10 second initial retry
        .setMaxIntervalMillis(10 * 60 * 1000) // 10 minute maximum retry
        .setMaxElapsedTimeMillis(60 * 60 * 1000) // 1 hour total retry
        .build();

    do {
      file = reporting.files()
        .get(file.getReportId(), file.getId())
        .setOauthToken(config.getAccessToken())
        .execute();

      if ("REPORT_AVAILABLE".equals(file.getStatus())) {
        // File has finished processing.
        log.info("File status is {}, ready to download.", file.getStatus());
        return file;
      } else if (!"PROCESSING".equals(file.getStatus())) {
        // File failed to process.
        log.error("File status is {}, processing failed.", file.getStatus());
        throw new IllegalStateException(String.format("File status is %s, processing failed.", file.getStatus()));
      }

      // The file hasn't finished processing yet, wait before checking again.
      long retryInterval = backOff.nextBackOffMillis();
      if (retryInterval == BackOff.STOP) {
        throw new IllegalStateException("File processing deadline exceeded.");
      }

      log.info("File status is {}, sleeping for {}.", file.getStatus(), retryInterval);
      Thread.sleep(retryInterval);
    } while (true);
  }

  public static InputStream downloadReport(DoubleClickReportingBatchSourceConfig config, File reportMetadata)
    throws IOException {
    // Create a get request.
    Dfareporting.Files.Get getRequest = reporting.files()
      .get(reportMetadata.getReportId(), reportMetadata.getId())
      .setOauthToken(config.getAccessToken());

    // Optional: adjust the chunk size used when downloading the file.
    // getRequest.getMediaHttpDownloader().setChunkSize(MediaHttpDownloader.MAXIMUM_CHUNK_SIZE);

    // Execute the get request and download as InputStream.
    return getRequest.executeMediaAsInputStream();
  }
}
