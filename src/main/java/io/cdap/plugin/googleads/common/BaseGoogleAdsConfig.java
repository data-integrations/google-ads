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
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.ReferencePluginConfig;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Provides all required configuration for reading Google AdWords reports
 */
public class BaseGoogleAdsConfig extends ReferencePluginConfig {

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
  @Description("Developer token which is a unique string")
  @Macro
  public String developerToken;
  @Name(CLIENT_CUSTOMER_ID)
  @Description("Customer ID of the client account")
  @Macro
  public String clientCustomerId;
  @Name(INCLUDE_REPORT_SUMMARY)
  @Description("Specifies whether report include a summary row containing the report totals.")
  @Macro
  public Boolean includeReportSummary;
  @Name(USE_RAW_ENUM_VALUES)
  @Description("Set to true if you want the returned format to be the actual enum value," +
    " for example, \"IMAGE_AD\" instead of \"Image ad\"." +
    " Set to false or omit this header if you want the returned format to be the display value.")
  @Macro
  public Boolean useRawEnumValues;
  @Name(INCLUDE_ZERO_IMPRESSIONS)
  @Description("Specifies whether the report includes rows where all specified metric fields equal to zero")
  @Macro
  public Boolean includeZeroImpressions;
  @Name(START_DATE)
  @Description("Start date for the report data. YYYYMMDD format." +
    " \"LAST_30_DAYS\", \"LAST_60_DAYS\" and \"LAST_90_DAYS\" values are allowed.")
  @Macro
  public String startDate;
  @Name(END_DATE)
  @Description("End date for the report data. YYYYMMDD format. \"TODAY\" value is allowed.")
  @Macro
  public String endDate;

  public BaseGoogleAdsConfig(String referenceName) {
    super(referenceName);
  }

  public void validate(FailureCollector failureCollector) {
    GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
    validateAuthorization(failureCollector, googleAdsHelper);
    validateDateRange(failureCollector);
  }

  protected void validateAuthorization(FailureCollector failureCollector, GoogleAdsHelper googleAdsHelper) {
    if (containsMacro(REFRESH_TOKEN)
      || containsMacro(CLIENT_ID)
      || containsMacro(CLIENT_SECRET)
      || containsMacro(DEVELOPER_TOKEN)
      || containsMacro(CLIENT_CUSTOMER_ID)) {
      return;
    }
    try {
      googleAdsHelper.getAdWordsSession(this);
    } catch (OAuthException e) {
      failureCollector.addFailure(e.getMessage(), "Enter valid credentials");
    } catch (ValidationException e) {
      failureCollector.addFailure(
        String.format("invalid value '%s' : %s", e.getTrigger(), e.getMessage()),
        "Enter valid credentials");
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
    } else if (date.equalsIgnoreCase("LAST_60_DAYS")) {
      cal.add(Calendar.DAY_OF_MONTH, -60);
      Date today60 = cal.getTime();
      return dateFormat.format(today60);
    } else if (date.equalsIgnoreCase("LAST_90_DAYS")) {
      cal.add(Calendar.DAY_OF_MONTH, -90);
      Date today90 = cal.getTime();
      return dateFormat.format(today90);
    }
    return date;
  }
}
