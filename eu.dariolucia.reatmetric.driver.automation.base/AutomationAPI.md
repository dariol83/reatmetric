## Automation API

### Script structure
A script must be a sequence of Javascript/Groovy/Python statements, which will be executed when the script is evaluated by the script engine. The
statements shall not be placed into a function wrapper, even though Javascript/Groovy/Python functions can be specified at the beginning of the 
script file.

API function names as well as the variable names 'FILENAME' and '_scriptManager' are reserved and shall not be used as 
declared variable names. 

### Declaration of arguments
Script arguments do not need declaration: the automation engine binds all the activity arguments to equivalent language 
symbols. The only argument which is not mapped is the argument named "FILENAME", which is the reserved argument name to
map to the specific script file inside the script directory. 

### API functions
The automation engine provides to scripts the following API functions.

#### Log messages

`info(String message):void`

`warning(String message):void`

`alarm(String message):void`

Raise a message with the specified severity as operational message.

#### State fetching

`parameter(String paramPath):ParameterData`

Return the current state (as ParameterData) of the specified parameter. Can return null if no such parameter can be located in the processing model.

`event(String eventPath):EventData`

Return the current state (as EventData) of the specified event. Can return null if no such event can be located in the processing model.

`wait_for_event(String eventPath, int timeoutSeconds):EventData`

Wait timeoutSeconds for the next occurrence of the specified event. Return the event (as EventData) of the specified event, if received during the specified time. 
Can return null if no such event is received during the specified time.

`wait_for_parameter(String parameterPath, int timeoutSeconds):ParameterData`

Wait timeoutSeconds for the next occurrence of the specified parameter. Return the parameter (as ParameterData) of the specified parameter, if received during the specified time. 
Can return null if no such parameter is received during the specified time.

#### Parameter injection

`inject_parameter(String paramPath, Object value):boolean`

Inject the source value of the specified parameter. Return true if the function call to the processing model went OK, false in case of injection exception.

#### Event injection

`raise_event(String eventPath):boolean`

Raise the specified event. Return true if the function call to the processing model went OK, false in case of injection exception.

`raise_event(String eventPath, String qualifier):boolean`

Raise the specified event with the attached qualifier. Return true if the function call to the processing model went OK, false in case of injection exception.

`raise_event(String eventPath, String qualifier, Object report):boolean`

Raise the specified event with the attached qualifier and report. Return true if the function call to the processing model went OK, false in case of injection exception.

#### Activity execution and monitoring

`prepare_activity(String activityPath):ActivityInvocationBuilder`

Prepare the invocation of an activity. Return an object that can be used to customise the activity execution and invoke it subsequently. Can return null if no such activity can be located in the processing model..

`ActivityInvocationBuilder::with_route(String route):ActivityInvocationBuilder`

Set the activity invocation route. Return the builder object.

`ActivityInvocationBuilder::with_property(String k, String v):ActivityInvocationBuilder`

Set a property. Return the builder object.

`ActivityInvocationBuilder::with_argument(String name, Object value, boolean engineering):ActivityInvocationBuilder`

Set a plain argument. Return the builder object.

`ActivityInvocationBuilder::with_argument(AbstractActivityArgument arg):ActivityInvocationBuilder`

Set an argument (plain or array). Return the builder object.
            
`ActivityInvocationBuilder::execute():ActivityInvocationResult`

Execute the activity. Return an ActivityInvocationResult object that can be used to asynchronously monitor the status of the activity.

`ActivityInvocationBuilder::execute_and_wait():boolean;`

Execute the activity and wait for its completion. Return true if the activity execution completed with success, otherwise false.        

`ActivityInvocationBuilder::execute_and_wait(int timeoutSeconds):boolean`

Execute the activity and wait for its completion in the specified time in seconds. Return true if the activity execution completed with success, otherwise (also in case of timeout) false.

`ActivityInvocationBuilder::prepare_schedule(String source, String externalId, Integer expectedDurationSeconds):SchedulingActivityInvocationBuilder`

Starting preparing a scheduling request for the activity. If expectedDurationSeconds is null, then the expected
duration as per activity definition is used.

`SchedulingActivityInvocationBuilder::with_resource(String resource):SchedulingActivityInvocationBuilder`

Add a resource to the resource set of the scheduling request.

`SchedulingActivityInvocationBuilder::with_resources(Collection<String> resources):SchedulingActivityInvocationBuilder`

Add resources to the resource set of the scheduling request.

`SchedulingActivityInvocationBuilder::with_resources(String... resources):SchedulingActivityInvocationBuilder`

Add resources to the resource set of the scheduling request.

`SchedulingActivityInvocationBuilder::with_latest_invocation_time(Instant time):SchedulingActivityInvocationBuilder`

Set the latest invocation time to the scheduling request.

`SchedulingActivityInvocationBuilder::with_conflict_strategy(ConflictStrategy conflictStrategy):SchedulingActivityInvocationBuilder`

Set the conflict strategy for the activity execution.

`SchedulingActivityInvocationBuilder::with_creation_conflict_strategy(CreationConflictStrategy strategy):SchedulingActivityInvocationBuilder`

Set the conflict strategy for the scheduling request.

`SchedulingActivityInvocationBuilder::schedule_absolute(Instant scheduledTime):boolean`

Schedule the activity at the specified time.

`SchedulingActivityInvocationBuilder::schedule_relative(int delaySeconds, String... predecessors):boolean`

Schedule the activity to start after the completion of all listed predecessors (external IDs), and with the addition of the specified delay in seconds.

`SchedulingActivityInvocationBuilder::schedule_event(String eventPath, int millisecondsProtectionTime):boolean`

Schedule the activity to start when the specified event is detected, and with the specified protection time.

`ActivityInvocationResult::wait_for_completion():boolean`

Wait for the activity to complete. Return true if the activity execution completed with success, otherwise false.

`ActivityInvocationResult::wait_for_completion(int timeoutSeconds):boolean`

Wait for the activity to complete in the specified time in seconds. Return true if the activity execution completed with success, otherwise (also in case of timeout) false.

`ActivityInvocationResult::is_invocation_failed():boolean`

Return true if the activity invocation failed, otherwise false.

`ActivityInvocationResult::is_completed():boolean`

Return true if the activity execution completed, otherwise false.

`ActivityInvocationResult::current_status():ActivityReportState`

Return the current status (as ActivityReportState) of the activity execution.

`delete_scheduled_activity(String externalId):boolean`

Delete the specified entry in the scheduler. Return true if the entry was deleted, otherwise false.

### How to return a value

For Javascript and Groovy it is enough to evaluate the variable as last instruction at the end of the script. For instance, if the script uses the variable
`theResult` and the value must be reported, it is enough to declare, at the end of the script:

`theResult;`

For Python, the special variable name `_result` shall be used to report the return value, since Python does not return a value using the last evaluated
expression.