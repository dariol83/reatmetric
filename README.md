# ReatMetric
ReatMetric is a Java-based software infrastructure for the implementation of Monitoring & Control (M&C) systems, with a
strong focus on the space domain. ReatMetric components provide a simple but efficient implementation of the typical 
functions used in an M&C system.

## System Overview

### Modules
ReatMetric is a modular framework, decomposed in modules following the Java module system mechanism. Each module implements specific functionalities and can be _typically_ be replaced in isolation, if needed. The module definition follows a layered approach:
- eu.dariolucia.reatmetric.api: this module contains the interfaces that define the boundaries of the framework. Software interfacing a ReatMetric system from outside (e.g. a graphical user interface) depends on this module only at compile time. This approach allows changing the underlying implementation of the M&C functionalities without affecting external modules. In addition to the interfaces, a set of POJO objects (typically immutable) is present, as well as some utility classes.
- eu.dariolucia.reatmetric.persist: this module contains an implementation of the archiving interfaces as defined by the _api_ module based on Apache Derby and a file-based approach. While this implementation is suited for one-off testing and small M&C systems, its usage in large scale systems is not suggested, due to the file-based approach limitations.
- eu.dariolucia.reatmetric.processing: this module contains the implementation of the M&C processing capabilities at the level of parameters, events and activities. The terminology as well as the conceptual decomposition is partially derived from the ECSS standard ECSS-E-ST-70-31C: while the data definition tries to cover to the maximum possible extent the standard, the coverage is not complete in order to limit the complexity of its implementation.
- eu.dariolucia.reatmetric.core: this module provides an implementation of the main service specified by the _api_ module, and it has a direct dependency on the _processing_ implementation. It provides brokers for raw data distribution and message distribution and defines a lower level API for the definition of _drivers_.
- eu.dariolucia.reatmetric.driver.spacecraft: this module provides a driver implementation for the monitoring and control of a CCSDS/ECSS-PUS compliant spacecraft. Currently it includes support for the reception of TM/AOS frames via SLE RAF/RCF services, TM packet extraction, parameter decoding, PUS 9 time correlation, PUS 5 event mapping. Support for spacecraft telecommands is part of the roadmap.eu.dariolucia.reatmetric.driver.
- eu.dariolucia.reatmetric.driver.test: this module is a simple test driver that shows how to implement a simple custom driver.
- eu.dariolucia.reatmetric.ui: this module implements a fully featured UI (JavaFX-based) that can be used to start and operate a ReatMetric-based system.

### Dependencies
The ReatMetric modules are based on a very limited set of dependencies:
- eu.dariolucia.ccsds: providing support for SLE/TM/TC/PUS handling of the _spacecraft_ driver;
- openJFX and ControlsFX: for the graphical user interface of the _ui_ module;
- Apache Derby: providing the storage backend of the _persist_ module;
- JAXB: for the configuration of all modules.

## Core Functionalities

### Abstraction layer for processing

#### Parameters

#### Events

#### Activities

### Archiving

### Raw data dissemination

### Operational messages dissemination

### UI Interface 

## Spacecraft M&C Driver

### TM support

### TC support

### Performance

## Getting Started

## Implement your driver

## Roadmap
- Spacecraft TC support
- Scheduler
- Automation system
- Alternative _persist_ implementations (server-based - in addition to file-based - Apache Derby, PostgreSQL)

## Acknowledgements and Credits

