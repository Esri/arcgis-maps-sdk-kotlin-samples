# ArcGIS Maps SDK Kotlin Samples 

[![Link: ArcGIS Developers home](https://img.shields.io/badge/ArcGIS%20Developers%20Home-633b9b?style=flat-square)](https://developers.arcgis.com)
[![Link: Documentation](https://img.shields.io/badge/Documentation-633b9b?style=flat-square)](https://developers.arcgis.com/kotlin/)
[![Link: Tutorials](https://img.shields.io/badge/Tutorials-633b9b?style=flat-square)](https://developers.arcgis.com/documentation/mapping-apis-and-services/tutorials/)
[![Badge: Samples](https://img.shields.io/badge/Samples-633b9b?style=flat-square)](https://developers.arcgis.com/kotlin/sample-code/)
[![Link: Toolkit](https://img.shields.io/badge/Toolkit-633b9b?style=flat-square)](https://developers.arcgis.com/kotlin/toolkit/)
[![Link: Esri Community](https://img.shields.io/badge/🙋-Get%20help%20in%20Esri%20Community-633b9b?style=flat-square)](https://community.esri.com/t5/kotlin-maps-sdk-questions/bd-p/kotlin-maps-sdk-questions)

<a href='https://play.google.com/store/apps/details?id=com.esri.arcgismaps.kotlin.sampleviewer'> <img src='https://github.com/user-attachments/assets/924c2721-9a8a-4387-8fa2-c1b6f99f6bac' alt='Get it on Google Play' width="150" /></a>

## Overview

ArcGIS Maps SDK for Kotlin v300.0.0 samples.  The `main` branch of this repository contains sample app modules for the latest available version of the [ArcGIS Maps Kotlin SDK](https://developers.arcgis.com/kotlin/). Samples released under older versions can be found through the [git tags](https://github.com/Esri/arcgis-maps-sdk-kotlin-samples/tags).  Please read our [wiki](https://github.com/Esri/arcgis-maps-sdk-kotlin-samples/wiki) for help with working with this repository.

## Prerequisites

* The samples are building with `compileSdkVersion 35`
* [Android Studio](http://developer.android.com/sdk/index.html)
* [An ArcGIS Developers API key](https://developers.arcgis.com/kotlin/get-started/#3-get-an-api-key)

## Developer Instructions

Please read our [developer instructions wiki page](https://github.com/Esri/arcgis-maps-sdk-kotlin-samples/wiki/Developer-Instructions) to set up your developer environment with Android Studio.  Instructions include forking and cloning the repository for those new to Git.

Once the project is cloned to disk you can import into Android Studio:

* From the Welcome to Android Studio screen, click the **Open** button. (If you're already inside a project, click **File > Open** in the menu bar instead.)
* Navigate to the **arcgis-maps-sdk-kotlin-samples/** folder and click **OK**.

## Accessing Esri location services

Accessing Esri location services, including basemaps, routing, and geocoding, requires authentication using either an API Key or an ArcGIS identity:

### API key

A long-lived access token that gives your application access to ArcGIS location services. Go to the [Create an API key](https://links.esri.com/create-an-api-key) tutorial to obtain a new API key access token. Ensure that the following privileges are enabled:

  * **Location services** > **Basemaps**
  * **Location services** > **Geocoding**
  * **Location services** > **Routing**

The Kotlin samples in this repository have been structured to use an access token, set once, which will run in all samples.
Set your access token in the `local.properties` in the same folder as the `secrets.defaults.properties`.
The ACCESS_TOKEN property should contain quotes around the key itself:
```gradle
ACCESS_TOKEN=YOUR_ACCESS_TOKEN
```

### ArcGIS identity

An ArcGIS named user account that is a member of an organization in ArcGIS Online or ArcGIS Enterprise.

## Run a sample

Once you have set up your developer environment you can run any sample from within Android Studio by selecting the app module from the **Edit Configurations** drop down and clicking the **Run** button from the toolbar.

### Build/Run sample from Gradle

You can execute all the build tasks using the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) command line tool. It's available as a batch file for Windows (gradlew.bat) and a shell script for Linux/Mac (gradlew.sh) and it is accessible from the root of the project.  

* Build a debug APK

```
$ ./gradlew assembleDebug
```

* Install the app on the device

```adb
adb -d install path/to/sample.apk
```

Built APK's are saved to **arcgis-maps-sdk-kotlin-samples/app/build/outputs/**. More information about running apps on devices can be found [here](https://developer.android.com/studio/run/device.html).

## Issues

Have a question about functionality in the ArcGIS Maps SDK Kotlin Samples? Want to ask other users for development advice, discuss a workflow, ask Esri staff and other users about bugs in the API? Use [GeoNet](https://community.esri.com/t5/kotlin-maps-sdk-questions/bd-p/kotlin-maps-sdk-questions) for any general questions like this, so others can learn from and contribute to the discussion.

Do you have something to [contribute](.github/CONTRIBUTING.md)? Send a pull request! New Samples, bug fixes and documentation fixes are welcome.

Have a problem running one of the samples in this repo? Does the sample not work on a specific device? Have questions about how the code in this repo is working? Want to request a specific sample? In that case, [submit a new issue](https://github.com/Esri/arcgis-maps-sdk-kotlin-samples/issues).

## Contributing

Anyone and everyone is welcome to [contribute](.github/CONTRIBUTING.md). We do accept pull requests.

1. Get Involved
2. Report Issues
3. Contribute Code
4. Improve Documentation

Please see our [guidelines for contributing doc](https://github.com/Esri/contributing/blob/master/README.md)

## Third-Party Libraries

This project uses the following third-party libraries:

- **[Showdown](https://github.com/showdownjs/showdown/blob/master/LICENSE)**
  - License: MIT License
  - Copyright: 2018, 2021

- **[Highlight.js](https://github.com/highlightjs/highlight.js/blob/main/LICENSE)**
  - License: BSD 3-Clause License
  - Copyright: 2006, Ivan Sagalaev

## Licensing

Copyright 2022 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [LICENSE](LICENSE?raw=1) file
