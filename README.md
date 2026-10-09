# OpenLMIS RDC Fulfillment Extension Module
This is the OpenLMIS 3.x Fulfillment Extension Module for the RDC (Democratic Republic of Congo) implementation.
It replaces the default order number generation with the `COMMANDE-ddMMyyyy-PROGRAM_CODE-SEQUENCE` format,
e.g. `COMMANDE-28092026-VAR-0001`.

## Prerequisites
* Docker 1.11+
* `openlmis-fulfillment` 9.3.2+ (runs the extension migrations from `db/extension`)

## Quick Start
1. Fork/clone this repository from GitHub.

 ```shell
 git clone https://github.com/OpenLMIS-RDC/rdc-fulfillment-extension.git
 ```
2. Fork/clone `openlmis-fulfillment` repository from GitHub.

 ```shell
 git clone https://github.com/OpenLMIS/openlmis-fulfillment.git
 ```
3. To build the jar, run the tests and the code analysis run `docker compose run --rm builder`.
4. To build the `openlmisrdc/fulfillment-extension` image run `docker compose build image`. The image contains the
   extension jar and the `extensions.properties` configuration file from this repository:

 ```
 OrderNumberGenerator=CommandeOrderNumberGenerator
 ```
5. In the RDC deployment (`rdc-deployment`), run the image as a one-off service that copies both files into a volume,
   and mount the same volume as `/extensions` in the fulfillment service. The fulfillment service has to wait until
   the copy finishes (`condition: service_completed_successfully`).
6. Start the fulfillment service and create an order to check if your changes have been applied.

## <a name="extensions"></a> Example of extensions usage

#### General information
OpenLMIS allows extending or overriding certain behavior of service using extension points and extension modules.
Every independent service can expose extension points that an extension module may utilize to extend its behavior.
Extension point is a Java interface placed in the service. Every extension point has its default implementation that
can be overridden. Extension modules can contain custom implementation of one or more extension points from main service.

Decision about which implementation should be used is made based on the configuration file `extensions.properties`.
This configuration file specifies which modules (implementations) should be used for the Service's extension points.
For RDC, the configuration file specifies that a `CommandeOrderNumberGenerator` module should be used for the
extension point `OrderNumberGenerator`.

```
#RDC extensions configuration
OrderNumberGenerator=CommandeOrderNumberGenerator
```

The extension point `OrderNumberGenerator` is an ID defined by the interface `OrderNumberGenerator.java`,
while the extension module `CommandeOrderNumberGenerator` is an implementation of that interface whose name is a
Spring Bean defined in `CommandeOrderNumberGenerator.java`.

The mapping is required whenever the extension jar is on the fulfillment classpath. Without it, the service finds two
`OrderNumberGenerator` implementations and order creation fails.

Configuration file lives in this repository and is shipped together with the extension jar in the Docker image.

Following classes are example of extension points usage:

- **OrderNumberGenerator.java** - extension point from openlmis-fulfillment repository, that has Id defined in
    ExtensionPointId class.
- **Base36EncodedOrderNumberGenerator.java** - default implementation of that interface, it has `@Component`
    annotation that contains its Id.
- **CommandeOrderNumberGenerator.java** - class implementing OrderNumberGenerator interface from openlmis-fulfillment
    repository. It has `@Component` annotation that contains its Id. It builds the order number from:
    - `COMMANDE` prefix,
    - the current date in French format (`ddMMyyyy`) in the service time zone (`TIME_ZONE_ID`),
    - the program code,
    - a sequence kept per supplying facility and program, padded to 4 digits and reset every month.
- **OrderNumberSequenceRepository.java** - returns the next sequence value, using a PostgreSQL upsert on the
    `fulfillment.order_number_sequence` table, so concurrently created orders never get the same number.
- **ExtensionManager.java** - class from openlmis-fulfillment repository that has getExtension method. It returns
    implementation of an extension class that is defined in configuration file for extension point with given Id.

The extension migration in `src/main/resources/db/extension` creates the sequence table and turns off the
order number prefix, program code and type suffix in `order_number_configurations`, so the generated number is used
as the order code without any additions. New migrations can be created with
`gradle generateExtensionMigration -PmigrationName=name_of_migration`.

#### Naming convention
The extension points' and extension modules' IDs should be **unique** and in **UpperCamelCase**.
A situation where two extension modules have the same ID leads to undeterministic behavior - it is not possible to predict which bean will be used.

* Extension points should be descriptive of the behavior that may be changed.  For example "RequisitionOrderQuantityCalculation" instead of "OrderQuantity".
* Extension Modules should describe the behaviour that is implemented, and the extension point that is being used.  For example "RequisitionOrderQuantityCalculationAMC" and "RequisitionOrderQuantityCalculationISA".
