// Copyright 2014 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.cdap.plugin.doubleclick.source.reporting.common;

import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.dfareporting.Dfareporting;
import io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSource;

/**
 * Utility methods to get DFA Reporting and Trafficking API service instance.
 */
public class DfaReportingFactory {

  private static final HttpTransport HTTP_TRANSPORT = Utils.getDefaultTransport();
  private static final JsonFactory JSON_FACTORY = Utils.getDefaultJsonFactory();

  /**
   * Performs all necessary setup steps for running requests against the API.
   *
   * @return An initialized {@link Dfareporting} service object.
   */
  public static Dfareporting getInstance() {
    // Create Dfareporting client.
    return new Dfareporting.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
      .setApplicationName(DoubleClickReportingBatchSource.NAME)
      .build();
  }
}
