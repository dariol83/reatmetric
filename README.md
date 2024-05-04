![ReatMetric](eu.dariolucia.reatmetric.ui/src/main/resources/eu/dariolucia/reatmetric/ui/fxml/images/logos/logo-full-color-640px.png)

ReatMetric is a well-documented Java-based software infrastructure for the implementation of Monitoring & Control (M&C) systems, 
with a strong focus on the space domain. ReatMetric components provide a simple but efficient implementation of the typical 
functions used in an M&C system.

![GitHub Release](https://img.shields.io/github/v/release/dariol83/reatmetric)

![Displays](docs/images/reatmetric-windows-01.PNG "ReatMetric")

## Documentation
The system concepts, design, configuration and usage are described in the [documentation](docs/ReatMetric%20System%20Manual.adoc).

Suggestions on how to improve the documentation and on missing description of features can be provided opening an issue on
GitHub.

## Dependencies
ReatMetric is based on a very limited set of dependencies:
- [eu.dariolucia.ccsds](https://www.github.com/dariol83/ccsds): providing support for SLE/TM/TC/PUS handling of the _spacecraft_ driver;
- [openJFX](https://openjfx.io): for the graphical user interface of the _ui_ module;
- [ControlsFX](https://github.com/controlsfx/controlsfx): advanced UI controls for the _ui_ module;
- [eu.dariolucia.jfx.timeline](https://www.github.com/dariol83/timeline): providing support for schedule rendering in the _ui_ module;
- [Apache Derby](http://db.apache.org/derby): providing the storage backend of the _persist_ module;
- [JAXB](https://javaee.github.io/jaxb-v2): for the configuration of all modules;
- [Groovy](https://groovy-lang.org): for the Groovy language support in the _processing_ and _automation_ modules (best choice);
- [GraalVM](https://www.graalvm.org): for the Javascript language support in the _automation_ modules;
- [Jython](https://www.jython.org/): for the Python language support in the _automation_ modules;
- [Json Path](https://github.com/json-path/JsonPath): for the parsing of JSON objects and files.

Including also the indirect dependencies, a typical ReatMetric backend deployment (i.e. without UI) is composed by 33 Jars,
10 from ReatMetric and 23 from external dependencies, for a total of 19.4 MB.

## Performance
The performance of the processing model and of the spacecraft driver have been measured on the following 2 platforms:

1) Zotac Magnus EN72070V: Intel Core i7-9750H, Hexa-core, 2.6 GHz without turbo boost; 32 GB DDR4 2666 MHz; Windows 10 Professional 64 bits; openJDK 11.
2) Raspberry PI 4: Broadcom BCM2711, Quad Core Cortex-A72 (ARM v8) 64-bit SoC @ 1.5GHz, 8 GB DDR4 3200 MHz; Manjaro Linux 64 bits; openJDK 11.

Each platform runs only the backend application, without UI.

The data definition of the processing model included:
- 80'000 processing parameters
- 5'000 synthetic parameters
- 10'000 reported events
- 2'000 condition-based events
- 10'000 activities

The TM/TC data definition of the spacecraft included:
- 3'600 PUS (3,25) TM packets
- 10'000 PUS 5 TM packets
- time packet, verification reports
- 10'000 TC commands

TM/TC setup:
- single RAF SLE in online complete mode;
- single CLTU SLE;
- TM frame: 1115 bytes with CLCW, no FECF;
- TM packets: with packet CRC.

### Platform 1 results
- Processing start-up time (as per logs - first run, no cache): 26 seconds
- Processing start-up time (as per logs - with cache): 10 seconds
- Max TM rate: 23.1 Mbit/sec (SLE TML level - RAF complete mode - processing backpressure propagated to the data generator)
- Nb. of TM frames per second: ca 2.300/sec
- Nb. of TM packets per second: ca 8.000/sec
- Nb. of TM parameter samples decoded per second: ca 435.000/sec
- Nb. of processed items generated per second: ca 530.000/sec
- Memory usage (heap size): between 2 and 4 GB, Windows reports 5 GB
- CPU load: between 35% and 45% (equivalent of almost 6 cores fully utilised)

![Connector Performance](docs/images/reatmetric-test-all-in-a-box-01.PNG "Connector Performance")
![System Performance](docs/images/reatmetric-test-all-in-a-box-02.PNG "System Performance")

### Platform 2 results
- Processing start-up time (as per logs - first run, no cache): 113 seconds
- Processing start-up time (as per logs - with cache): 56 seconds
- Max TM rate: 4.6 Mbit/sec (peak: 7 Mbit/sec, no backlog: 2.5 Mbit/sec)
- Nb. of TM frames per second (peak): ca 450/sec, no backlog: 250/sec
- Nb. of TM packets per second (peak): ca 1270/sec, no backlog: 900/sec
- Nb. of TM parameter samples decoded per second (peak): 70.000/sec
- Nb. of processed items generated per second (peak): 100.000/sec
- Memory usage server (heap size): between 2 and 4 GB, top reports 4.6 GB (capped with -Xmx4G)
- Memory usage UI (Windows Task Monitor): 1.6 GB
- CPU load: between 320% and 350% (all 4 cores above 80%)

![Connector Performance](docs/images/reatmetric-test-raspberry-01.PNG "Connector Performance")
![System Performance](docs/images/reatmetric-test-raspberry-02.PNG "System Performance")

## Getting Started

### All-in-one
If you want to quickly try ReatMetric out, I suggest the following approach:
- Build the complete tree with maven: mvn clean install
- Create a folder called 'reatmetric' inside your home folder and decompress there the configuration zip inside eu.dariolucia.reatmetric.ui.test/src/main/resources
- Update the configuration data as appropriate. There is no need to change the processing definition data
- Go inside eu.dariolucia.reatmetric.ui.test/target and run the following line (assuming Java is in your path)

(Windows)

    java --module-path="deps" -Dreatmetric.core.config=<path to ReatMetric>\configuration.xml --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI

(Linux)  

    java --module-path="deps" -Dreatmetric.core.config=<path to ReatMetric>/configuration.xml --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI

### With remoting
If you want to try ReatMetric using a client-server deployment, I suggest the following approach:
- Build the complete tree with maven: mvn clean install
- Create a folder called 'reatmetric' inside your home folder and decompress there the configuration zip inside eu.dariolucia.reatmetric.ui.test/src/main/resources
- Update the configuration data as appropriate. There is no need to change the processing definition data
- Go inside eu.dariolucia.reatmetric.remoting.test/target and run the following line (assuming Java is in your path)

(Windows)

    java --module-path="deps" -Dreatmetric.core.config=<path to ReatMetric>\configuration.xml -m eu.dariolucia.reatmetric.remoting/eu.dariolucia.reatmetric.remoting.ReatmetricRemotingServer 19000

(Linux)
  
    java --module-path="deps" -Dreatmetric.core.config=<path to ReatMetric>/configuration.xml -m eu.dariolucia.reatmetric.remoting/eu.dariolucia.reatmetric.remoting.ReatmetricRemotingServer 19000

- Create a folder called 'reatmetric_remoting' inside your home folder
- Inside the folder created in the previous step, create a remoting configuration, so that the UI can connect
- Go inside eu.dariolucia.reatmetric.ui.remoting/target and run the following line (assuming Java is in your path)

(Windows)

    java --module-path="deps" -Djava.rmi.server.hostname=<server IP to use for local connections> -Dreatmetric.remoting.connector.config=<path to ReatMetric remoting>\configuration.xml --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI

(Linux) 
 
    java --module-path="deps" -Djava.rmi.server.hostname=<server IP to use for local connections> -Dreatmetric.remoting.connector.config=<path to ReatMetric remoting>/configuration.xml --add-exports javafx.base/com.sun.javafx.event=org.controlsfx.controls -m eu.dariolucia.reatmetric.ui/eu.dariolucia.reatmetric.ui.ReatmetricUI

Example of remoting configuration:

    <ns1:connectors xmlns:ns1="http://dariolucia.eu/reatmetric/remoting/connector/configuration">
	    <connector local-name="Test System" remote-name="Test System" host="192.168.2.106" port="19000" />
    </ns1:connectors>

## Roadmap

### To version 1.1.0
- SNMP driver
- SNMP generator for SNMP driver

### Future ideas
- Drop Javascript automation driver, due to GraalVM memory leak?
- Use JEP for Python support in automation, drop Jython
- persist implementation based on PostgreSQL and Timeseries
- Report generation module (driver? activity mapped?)
- File circulation/distribution driver, based on FTP/SFTP/SCP/HTTP/REST
- Export data function
- Helidon gRPC/REST remoting/Helidon gRPC/REST connector (requires Java 21)
- Helidon gRPC/REST remoting driver (requires Java 21)
- Hot redundancy at Reatmetric Core level (two instances fully synchronised, but only one has the connectors open and the scheduler active. When one goes down, the other takes over)

## Acknowledgements and Credits
A special mention goes to Theresa Köster from the University of Gießen, who evaluated ReatMetric (among other tools) 
against the Flying Laptop operational simulator. With her contributions, ideas and suggestions, she helped greatly to 
verify the TM/TC implementation compatibility of ReatMetric against a real, operational system.

Special thanks go to the F-Series team at Airbus DS, Germany, which is using ReatMetric and the underlying CCSDS software
library as a testing system for the development support and verification of the F-Series core avionics FLP2 and FLC. The
team contributed to resolve some issues in the implementation of the protocols and provided valuable feedback for the 
improvement of the usability of the ReatMetric system.

![F-Series](docs/images/f-series.png)

