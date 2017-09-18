# omar-backend-tests

### Purpose

The purpose of the omar-backend-tests project is to provide a suite of automated tests for different OMAR backend services. These tests run nightly and when certain OMAR projects are built. The current status of the tests helps developers to ensure that OMAR's backend services are running correctly.

### Content

The omar-backend-tests are composed of 8 sets of tests, each for a different service.

These tests are made using Cucumber, a test language that resembles basic English. This allows anyone to look at the tests and understand their purpose and intent. Using the Cucumber tests as a guide, the tests are then converted into code. In the case of the backend tests, Groovy is used to programatically test the capabilities of the backend services.

- *WFS* - tests the OMAR Web Feature Service endpoints
- *WMS* - tests the OMAR Web Map Service endpoints
- *WCS* - tests the OMAR Web Coverage Service endpoints
- *WMTS* - tests the OMAR Web Map Tile Service endpoints
- *Mensuration* - tests the OMAR Mensuration Service endpoints
- *ImageSpace* - tests the OMAR ImageSpace Service endpoints
- *Geoscript* - tests the OMAR Geoscript Service endpoints
- *Basemap* - tests the OMAR Basemap Service endpoints

More details on the content of the tests can be found in the Cucumber *.feature* files located in *src/cucumber/resources/features*

### Automated Execution

The backend tests are automatically executed using a Jenkins build on https://jenkins.ossim.io - the job name is omar-backend-tests-dev for the dev branch and omar-backend-tests-release for the master branch. The tests run nightly.

### Manual Execution

Running the backend tests manually requires Gradle. With Gradle installed, run the *gradle backend* command from the project's base directory in order to execute the *backend* task defined in the project's Gradle Build file.

The default config file uses the dev deployment of OMAR located at https://omar-dev.ossim.io
