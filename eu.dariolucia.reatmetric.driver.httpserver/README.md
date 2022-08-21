## HTTP Server Driver

The HTTP Server Driver offers a JSON/REST API that can be used by third-party software/web applications, to retrieve monitoring
information related to ReatMetric system.

Examples can be found by running the Reatmetric Test system and the contents present in this folder, to be hosted by a web server.
The IP and port of the test system are hardcoded in the HTML pages.

When the driver is in use, the following API will be available to the specified IP and port.

### Parameter descriptor list

~~~
    GET     http://<host>:<port>/<system name>/parameters/list
~~~

The request returns the list of parameters defined in the system, as an array of objects so defined:

~~~
    {
         "type" : "PARAMETER",
         "path" : <string>,
         "externalId" : <integer>,
         "description" : <string> or null,
         "rawDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "engDataType" : <string>, // See ValueTypeEnum enum names in eu.dariolucia.reatmetric.api.value
         "unit" : <string> or null,
         "synthetic" : <boolean>,
         "settable" : <boolean>
    }
~~~

*Javascript library function*: async listParameters() : return array of parameter descriptors.

### Event descriptor list

~~~
    GET     http://<host>:<port>/<system name>/events/list
~~~

The request returns the list of events defined in the system, as an array of objects so defined:

~~~
    {
         "type" : "EVENT",
         "path" : <string>,
         "externalId" : <integer>,
         "description" : <string> or null,
         "severity" : <string>, // See Severity enum names in eu.dariolucia.reatmetric.api.messages
         "eventType" : <string> or null
    }
~~~

*Javascript library function*: async listEvents() : return array of event descriptors.

### Parameter state subscription

This type of interaction allows clients to request the creation of a specific parameter subscription, based on the provided filter. 
On the server, parameters matching the subscription are kept in a map, where the key is the parameter path. Upon each GET, the map values 
are returned to the caller and the map is cleared. Hence the interaction, by design, returns only the latest parameter
state available at the time of each GET, if there were changes with respect to its previously returned state. This type
of interaction can be used to retrieve information to be used in ANDs and mimics. 

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/parameters/current/register
~~~

The request must provide in its body a ParameterDataFilter in JSON format, according to the following structure:

~~~
    {
         "parentPath" : <string> or null,
         "parameterPathList" : <array of string> or null,
         "routeList" : <array of string> or null,
         "validityList" : <array of string> or null, // See Validity enum names in eu.dariolucia.reatmetric.api.parameters
         "alarmStateList" : <array of string> or null, // See AlarmState enum names in eu.dariolucia.reatmetric.api.model
         "externalIdList" : <array of integer> or null
    }
~~~

The response returns in its body a key value (UUID) in the following structure:

~~~
    {
         "key" : <string> // UUID string
    }
~~~

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
parameters via a GET request (see below).

*Javascript library function*: async registerToStateParameters(filter) : return the key value as string.

*Javascript library function*: parameterFilter(parentPath, parameterPathList, routeList, validityList, alarmStateList, externalIdList) : return a parameter filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/parameters/current/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromStateParameters(key) : *void*

**Parameter fetch**
~~~
    GET     http://<host>:<port>/<system name>/parameters/current/get/<key>
~~~

The request returns the list of ParameterData object related to the subscription. Returned objects are removed from the 
server side map. The array of objects so defined:

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "externalId" : <integer>, 
        "path" : <string>, 
        "eng" : <string or time string or integer or float or boolean> or null,
        "raw" : <string or time string or integer or float or boolean> or null,
        "rcptime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "route" : <string> or null,
        "validity" : <string>, // See Validity enum names in eu.dariolucia.reatmetric.api.parameters
        "alarm" : <string> // See AlarmState enum names in eu.dariolucia.reatmetric.api.model
    }
~~~

*Javascript library function*: async getStateParameters(key) : return array of parameter data objects.

### Parameter stream subscription

This type of interaction allows clients to request the creation of a specific parameter subscription, based on the provided filter.
On the server, each parameter update matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the parameter samples
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
plots and charts. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/parameters/stream/register
~~~

The request must provide in its body a ParameterDataFilter in JSON format, according to the structure defined above.

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
parameters via a GET request (see below).

*Javascript library function*: async registerToStreamParameters(filter) : return the key value as string.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/parameters/stream/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromStreamParameters(key) : *void*

**Parameter fetch**
~~~
    GET     http://<host>:<port>/<system name>/parameters/stream/get/<key>
~~~

The request returns the list of ParameterData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is defined according to the structure defined above.

*Javascript library function*: async getStreamParameters(key) : return array of parameter data objects.

### Event stream subscription

This type of interaction allows clients to request the creation of a specific event subscription, based on the provided filter.
On the server, each event update matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the events
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
scrollable displays. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/events/register
~~~

The request must provide in its body a EventDataFilter in JSON format, according to the following structure.

~~~
    {
         "parentPath" : <string> or null,
         "eventPathList" : <array of string> or null,
         "routeList" : <array of string> or null,
         "typeList" : <array of string> or null, 
         "sourceList" : <array of string> or null,
         "severityList" : <array of string> or null, // See Severity enum names in eu.dariolucia.reatmetric.api.messages
         "externalIdList" : <array of integer> or null
    }
~~~

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
events via a GET request (see below).

*Javascript library function*: async registerToEvents(filter) : return the key value as string.

*Javascript library function*: eventFilter(parentPath, eventPathList, sourceList, routeList, typeList, severityList, externalIdList) : return an event filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/events/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromEvents(key) : *void*

**Event fetch**
~~~
    GET     http://<host>:<port>/<system name>/events/get/<key>
~~~

The request returns the list of EventData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is so defined.

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "externalId" : <integer>, 
        "path" : <string>, 
        "qualifier" : <string> or null,
        "rcptime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "type" : <string> or null,
        "route" : <string> or null,
        "source" : <string> or null,
        "severity" : <string> // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

*Javascript library function*: async getEvents(key) : return array of event data objects.

### Operational message stream subscription

This type of interaction allows clients to request the creation of a specific operational message subscription, based on the provided filter.
On the server, each operational message matching the subscription is kept in a queue. Upon each GET, the values in the queue 
are returned to the caller and the queue is cleared. Hence the interaction, by design, returns all the messages
generated by the system from a previous GET. This type of interaction can be used to retrieve information to be used in 
scrollable displays. 

The driver has a maximum queue size per subscription.

**Subscription**
~~~
    POST    http://<host>:<port>/<system name>/messages/register
~~~

The request must provide in its body a OperationalMessageDataFilter in JSON format, according to the following structure.

~~~
    {
         "messageTextContains" : <string> or null,
         "idList" : <array of string> or null,
         "sourceList" : <array of string> or null,
         "severityList" : <array of string> or null // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

The response returns in its body a key value according to the structure defined above.

The effect of this invocation is the creation, on the server side, of a subscription that will deliver the related 
messages via a GET request (see below).

*Javascript library function*: async registerToMessages(filter) : return the key value as string.

*Javascript library function*: messageFilter(messageTextContains, idList, sourceList, severityList) : return an operational message filter object.

**Removal of subscription**
~~~
    DELETE  http://<host>:<port>/<system name>/messages/deregister/<key>
~~~

The effect of this invocation is the removal, on the server side, of the subscription linked to the specified key.

*Javascript library function*: async deregisterFromMessages(key) : *void*

**Operational messages fetch**
~~~
    GET     http://<host>:<port>/<system name>/messages/get/<key>
~~~

The request returns the list of OperationalMessageData objects related to the subscription. Returned objects are removed from the 
server side queue. The array of objects is so defined.

~~~
    {
        "internalId" : <integer>,
        "gentime" : <time string>, // Format as YYYY-MM-DD'T'hh:mm:ss.SSSZ
        "id" : <string> or null,
        "message" : <string>, 
        "source" : <string> or null,
        "severity" : <string> // See Severity enum names in eu.dariolucia.reatmetric.api.messages
    }
~~~

*Javascript library function*: async getMessages(key) : return array of operational message data objects.

### Model entity descriptor retrieval

~~~
    GET     http://<host>:<port>/<system name>/model/<path of system element, with / separator instead of . separator>
~~~

If successful, the request returns an object with two properties:
- element: it contains the descriptor of the entity referenced by the <path of system element>. If the path is not provided, then null is returned
- children: it contains an array with the descriptors of the child elements. If the element has no children, an empty array is returned.

~~~
    {
        "element" : <descriptor>,
        "children" : [
            <descriptor>,
            <descriptor>,
            <descriptor>,
            ...
            <descriptor>
        ]
    }
~~~

The descriptors have the following structure.

**Container**
~~~
    {
        "type" : "CONTAINER", 
        "path" : <string> // full system entity path with . separator  
    }
~~~

**Parameter**

Defined as above ("Parameter descriptor list").

**Event**

Defined as above ("Event descriptor list"). 

**Activity**

To be defined. 

*Javascript library function*: async getDescriptor(path) : return the element-children information assigned to the provided path.

### Model entity enable/disable

~~~
    POST     http://<host>:<port>/<system name>/model/<path of system element, with / separator instead of . separator>/[enable|disable]
~~~

This request enables or disables the processing linked to the specified system element.

*Javascript library function*: async enable(path) : *void*

*Javascript library function*: async disable(path) : *void*