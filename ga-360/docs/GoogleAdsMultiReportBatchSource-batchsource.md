# Google Ads - Multi report batch source

Description
-----------
Google Ads is an online advertising platform developed by Google, where advertisers pay to display brief advertisements, service offerings, product listings, video content, and generate mobile application installs within the Google ad network to web users. Google Ads multi report source plugin would allow users to retrieve all of the Google Ads reports from their Google Ads account in batch mode.

Properties
----------
### Basic

**Refresh token:** Authorization to download the report. [Authentication Documentation](https://developers.google.com/adwords/api/docs/guides/authentication)

**Client ID Secrets:** OAuth 2.0 Client Id from [console](https://console.developers.google.com)

**Client Secret:** OAuth 2.0 Client Secret from [console](https://console.developers.google.com)

**Developer token:** Developer token which is a unique string. [doc](https://developers.google.com/adwords/api/docs/guides/reporting#prepare_the_request)

**Customer ID:** Customer ID of the client account.

**Start Date:** Start date for the report data. YYYYMMDD format. "LAST_30_DAYS", "LAST_60_DAYS" and "LAST_90_DAYS" values are allowed.

**End Date:** End date for the report data. YYYYMMDD format. "TODAY" value is allowed.

### Advanced

**Format:** Report format [doc](https://developers.google.com/adwords/api/docs/guides/reporting#supported_download_formats).

**Include Report Header:** Specifies whether to include a header row to a report. This row contains the report name and date range.

**Include Column Header:** Specifies whether to include a header row to a report. This row contains report field names.

**Include Report Summary:** Specifies whether report include a summary row containing the report totals.

**Use Raw Enum Values:** Set to true if you want the returned format to be the actual enum value, for example, "IMAGE_AD" instead of "Image ad". Set to false or omit this header if you want the returned format to be the display value.

**Include Zero Impressions:** Specifies whether the report includes rows where all specified metric fields equal to zero.
