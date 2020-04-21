![BackPack Logo](https://gitlab.exadel.com/EDMTP/backpack/raw/dev/ui.apps/src/main/content/jcr_root/apps/backpack/assets/backpack_full.png)

# AEM BackPack

A tool for easily creating, handling and distributing content packages across Adobe Experience Manager (AEM) ecosystem.

Based on the out-of-box AEM Package Manager, the **BackPack** provides the ability to bundle together different pieces of website content (texts, image sources and renditions, videos, documents, etc.) in a flexible manner.

When you select e.g. a page for inclusion into package, all the assets referenced by this page (and, optionally, its children) are included as well. The tool analyzes relations between resources to produce comprehensive sets of data so that entire web pages with all their addenda can be moved across sites, translated, reformatted or backed up for later usage.

**BackPack** is aimed to decrease the effort and costs of content management and distribution; improve the security and reliability
  of content-related business processes.

### Project structure

The project consists of two modules: 

* *core module* runs in an AEM instance containing to respond to user commands and provide data processing service.
* *ui/application module* embeds into AEM administering interface and runs in browser to give to the user control over package creation and storage, as well as detailed feedback. This module is built upon the modern Coral 3 graphic interface and TouchUI-ready.

The two modules communicate via JSON-based REST protocol. Basically, a user command is sent to an endpoint, parsed and processed by a specially designed AEM service, then result and/or status message returned to user.

### Installation

**BackPack** is distributed as a package you can install by hand or, optionally, add to your project building workflow via e.g. *Content Package Maven Plugin* (either *Adobe* version or *wcm.io* version).

Alternatively you can build and install **BackPack** from source code. Clone this repository and run

    mvn clean install -PautoInstallPackage
    
from the command line. You can change `aem.host` and `aem.port` values as needed in the main POM file's <properties> section or via a command-line key like `-Daem.host=xxx -Daem.port=xxx` (default is *localhost:4502*).

### Usage

In your _Adobe Experience Manager_ console, navigate to _Tools_ section and choose _Deployment_ from the left menu rail. Click _"Backpack"_ in the right section.

TouchUI-compliant *Package Manager* interface opens. In the _"Group"_ dropdown to the left, you can choose a particular packages group, or see them all, if "All packages" option selected.

Hover over a package to see available options. _"Edit"_ button will open the same package in the "classic" interface with some aditional capabilities.

To create a new package, click _"Create"_ button in the top right. A dialog window will open, where you can put package's name, version, group, thumbnail, path to the node containing resources for the package, and turn child resources inclusion on or off. Several paths may be selected. Click the button in the bottom to complete the operation.

### Testing and development

The source code of the project is test-covered. You can run unit tests apart from the usual Maven build workflow with

    mvn clean test

There is also the specific "test" profile that helps to collect code quality statistics, engage test coverage analysis, etc. Run it with e.g.

    mvn clean verify sonar:sonar -Ptest -Dsonar.host.url=[....]
    
The project is in active development stage. Community contribution is heartily welcome.
