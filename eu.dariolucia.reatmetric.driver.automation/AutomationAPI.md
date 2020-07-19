## Automation API

`info(String message):void;`

`warning(String message):void;`

`alarm(String message):void;`

`parameter(String paramPath):ParameterData;`

`event(String eventPath):EventData;`

`set(String paramPath, Object value):boolean;`

`raise(String eventPath):boolean;`

`raise(String eventPath, String qualifier):boolean;`

`raise(String eventPath, String qualifier, Object report):boolean;`

`prepareActivity(String activityPath):ActivityInvocationBuilder;`

`ActivityInvocationBuilder::withRoute(String route):ActivityInvocationBuilder;`

`ActivityInvocationBuilder::withProperty(String k, String v):ActivityInvocationBuilder;`

`ActivityInvocationBuilder::withArgument(String name, Object value, boolean engineering):ActivityInvocationBuilder`;

`ActivityInvocationBuilder::withArgument(AbstractActivityArgument arg):ActivityInvocationBuilder;`
            
`ActivityInvocationBuilder::execute():ActivityInvocationResult;`

`ActivityInvocationBuilder::executeAndWait():boolean;`
        
`ActivityInvocationBuilder::executeAndWait(int timeoutSeconds):boolean;`

`ActivityInvocationResult::waitForCompletion():boolean;`

`ActivityInvocationResult::isInvocationFailed():boolean;`

`ActivityInvocationResult::isCompleted():boolean;`

`ActivityInvocationResult::currentStatus():ActivityReportState;`
