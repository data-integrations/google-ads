# Double Click Campain Manager batch source

## CAUTION: this version of the application is incomplete, as it was not tested on real data due to the lack of a license 

Description
-----------
This plugin is used to query DoubleClick Search API.

The plugin can use the existing report or create a new one based on the criteria passed.

Using this plugin you can get metrics and dimensions with built-in set. 

Please, see _https://developers.google.com/doubleclick-advertisers_ to get more information.

Properties
----------
### Basic

**Reference Name:** Name used to uniquely identify this source for lineage, annotating metadata, etc.

**Application ID:** The application ID from which the data is retrieved.

**Use an existing report or create a new one?:** Indicates if the plugin should use an existing report or it should generate a new report.

**Report ID:** The report ID to fetch the data for.

**Report name:** The report name to create.

**Report type:** The report type to create.

**Date range:** The date range to create report.

**Metrics:** A list of metrics based on the report type.

**Dimensions:** A list of dimensions based on the report type.

### Advanced

**Advanced properties:** A set of advanced properties to include in the report criteria, based on the selected report type.

### Credentials

**Access token:** Access token to access Double Click Campaign Manager reporting API.
