![BackPack Logo](https://github.com/exadel-inc/aem-backpack/raw/master/ui.apps/src/main/content/jcr_root/apps/etoolbox-backpack/assets/backpack.png)

# EToolbox BackPack

A tool for easily creating, handling and distributing content packages across Adobe Experience Manager (AEM) ecosystem.

Based on the out-of-box AEM Package Manager, the **BackPack** provides the ability to bundle together different pieces of website content (texts, image sources and renditions, videos, documents, etc.) in a flexible manner.

When you select e.g. a page for inclusion into package, all the assets referenced by this page (and, optionally, its children) are included as well. The tool analyzes relations between resources to produce comprehensive sets of data so that entire web pages with all their addenda can be moved across sites, translated, reformatted or backed up for later usage.

**BackPack** is aimed to decrease the effort and costs of content management and distribution; improve the security and reliability
  of content-related business processes.

### Project structure

The project is organized as a multi-module Maven build:

* `core` — the server-side OSGi bundle. It contains the Java code that implements BackPack services, request processing, package handling logic, and tests.
* `ui.apps` — the AEM application package. It contains the repository content installed under the application area, including the UI assets and browser-facing integration.
* `ui.apps.structure` — the repository structure package. It defines the AEM repository roots required by the application packages.
* `ui.config` — the AEM configuration package. It contains configuration content packaged separately from the application code.
* `all` — the aggregate container package. It embeds the `core`, `ui.apps`, and `ui.config` artifacts into a single package for deployment.


### Installation

**BackPack** is distributed as a package you can install by hand or, optionally, add to your project building workflow via e.g. *Content Package Maven Plugin* (either *Adobe* version or *wcm.io* version).

Alternatively you can build and install **BackPack** from source code. Clone this repository and run

    mvn clean install -PautoInstallPackage
    
from the command line. You can change `aem.host` and `aem.port` values as needed in the main POM file's <properties> section or via a command-line key like `-Daem.host=xxx -Daem.port=xxx` (default is *localhost:4502*).


### Requirements

The project is developed and tested in *AEM 6.4.5* (*uber-jar* 6.3.0) with Java 1.8. Please compile and use in environments other that this with caution.

### Usage

In your _Adobe Experience Manager_ console, navigate to _Tools_ section and choose _EToolbox_ from the left menu rail. Click _"Backpack"_ in the right section.

TouchUI-compliant *Package Manager* interface opens. In the _"Group"_ dropdown to the left, you can choose a particular packages group, or see them all, if "All packages" option selected.

Hover over a package to see available options. _"Edit"_ button will open the same package in the "classic" interface with some additional capabilities.

To create a new package, click _"Create"_ button in the top right. A dialog window will open, where you can put package's name, version, group, thumbnail, path to the node containing resources for the package, and turn child resources inclusion on or off. Several paths may be selected. Click the button in the bottom to complete the operation.

### Testing and development

The source code of the project is test-covered. You can run unit tests apart from the usual Maven build workflow with

    mvn clean test

There is also the specific "test" profile that helps to collect code quality statistics, engage test coverage analysis, etc. Run it with e.g.

    mvn clean verify sonar:sonar -Ptest -Dsonar.host.url=[....]
    
The project is in active development stage. Community contribution is heartily welcome.

### Licensing

The project is licensed under [Apache License, Version 2.0](LICENSE). All runtime project dependencies are guaranteed to be compliant with the license. Dependencies such as Adobe's *uber-jar* are considered *provided* in the end-user environment and are not explicitly engaged. The end user is to comply with the regulations of the corresponding licenses.
