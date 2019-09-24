# Google Ads batch source

Description
-----------
Google Ads source plugin would allow users to retrieve all of their Google Ads reports in batch mode.

Properties
----------
### Basic

**Authorization token:** Authorization to download the report.

**Developer token:** Developer token consisting of unique string.

**Customer ID:** Customer ID of the client account.

**Start Date:** Start date for the report data.

**End Date:** End date for the report data.

**Format:** Report format.

### Advanced

**Include Report Header:** If false, report output will not include a header row containing the report name and date range.

**Include Column Header:** If false, report output will not include a header row containing field names.

**Include Report Summary:** If false, report output will not include a summary row containing the report totals.

**Use Raw Enum Values:** Set to true if you want the returned format to be the actual enum value.

**Include Zero Impressions:** If true, report output will include rows where all specified metric fields are zero.

**Report type:** Report type.

**Fields:** Coma separated list of fields to pull.