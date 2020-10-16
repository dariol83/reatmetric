/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.automation.internal;

import eu.dariolucia.reatmetric.api.processing.IActivityHandler;
import eu.dariolucia.reatmetric.core.api.IServiceCoreContext;
import eu.dariolucia.reatmetric.driver.automation.common.Constants;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import javax.script.ScriptException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavascriptExecutor implements IScriptExecutor {

    private static final Logger LOG = Logger.getLogger(JavascriptExecutor.class.getName());

    private final String contents;
    private final IActivityHandler.ActivityInvocation invocation;
    private final String fileName;
    private final IServiceCoreContext context;
    private final String jsApiData;
    private final DataSubscriptionManager dataSubscriptionManager;

    private volatile Engine jsEngine = null;
    private volatile boolean aborted = false;
    private ScriptExecutionManager manager;

    public JavascriptExecutor(DataSubscriptionManager dataSubscriptionManager, IServiceCoreContext context, String initData, String contents, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.dataSubscriptionManager = dataSubscriptionManager;
        this.contents = contents;
        this.invocation = activityInvocation;
        this.fileName = fileName;
        this.context = context;
        this.jsApiData = initData;
    }

    @Override
    public Object execute() throws ScriptException {
        try {
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            jsEngine = Engine.create();
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            try (Context context = Context.newBuilder()
                    .engine(jsEngine)
                    .allowAllAccess(true)
                    .build()) {
                if(aborted) {
                    throw new IllegalStateException("Script " + fileName + " aborted");
                }
                Value bindings = context.getBindings("js");
                for (Map.Entry<String, Object> entry : invocation.getArguments().entrySet()) {
                    if (!entry.getKey().equals(Constants.ARGUMENT_FILE_NAME)) {
                        bindings.putMember(entry.getKey(), entry.getValue());
                    }
                }
                if(aborted) {
                    throw new IllegalStateException("Script " + fileName + " aborted");
                }
                // Add API functions
                manager = new ScriptExecutionManager(this.dataSubscriptionManager, this.context, invocation, fileName);
                bindings.putMember(Constants.BINDING_SCRIPT_MANAGER, manager);
                if(aborted) {
                    throw new IllegalStateException("Script " + fileName + " aborted");
                }
                context.eval("js", jsApiData);
                if(aborted) {
                    throw new IllegalStateException("Script " + fileName + " aborted");
                }
                Value returnValue = context.eval("js", this.contents);
                if(returnValue != null) {
                    return returnValue.as(Object.class);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        } catch (Error e) {
            LOG.log(Level.SEVERE, "Unexpected error when executing script " + fileName + ": " + e.getMessage(), e);
            throw new ScriptException(e.getMessage());
        } finally {
            jsEngine.close();
            jsEngine = null;
            manager = null;
        }
    }

    @Override
    public void abort() {
        aborted = true;
        ScriptExecutionManager theManager = manager;
        if(theManager != null) {
            theManager._abort();
        }
        Engine toAbort = jsEngine;
        if(toAbort != null) {
            try {
                jsEngine.close(true);
            } catch (Throwable e) {
                // Ignore!
            }
        }
    }
}
