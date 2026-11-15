## Spacecraft Driver

Check the [documentation](Documentation.adoc).

The reception-only `JtdpConnector` supports the JSatDecode TCP Binary Distribution Protocol using Java networking.
Configuration, status and event mappings are described in [JTDP reception](Documentation.adoc#_jtdp_reception), with a
complete single-decoder processing model in [jtdp_model.xml](src/main/resources/jtdp_model.xml).

A conversion service from the SCOS-2000 MIB files to the ReatMetric format suitable for the usage with the ReatMetric
Spacecraft driver is available at:

http://dariolucia.eu/reatmetric/reatmetric-conversion.html

