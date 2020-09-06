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
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import javax.script.ScriptException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroovyExecutor implements IScriptExecutor {

    private static final Logger LOG = Logger.getLogger(GroovyExecutor.class.getName());

    private final String contents;
    private final IActivityHandler.ActivityInvocation invocation;
    private final String fileName;
    private final IServiceCoreContext context;
    private final String groovyApiData;

    private volatile GroovyShell groovyShell;
    private volatile Binding groovyBinding;
    private volatile Script groovyScript;
    private volatile boolean aborted;
    private volatile ScriptExecutionManager manager;

    public GroovyExecutor(IServiceCoreContext context, String initData, String contents, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.contents = contents;
        this.invocation = activityInvocation;
        this.fileName = fileName;
        this.context = context;
        this.groovyApiData = initData;
    }

    @Override
    public Object execute() throws ScriptException {
        try {
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            groovyShell = new GroovyShell();
            groovyBinding = new Binding();
            groovyScript = groovyShell.parse(groovyApiData + "\n\n" + contents);
            groovyScript.setBinding(groovyBinding);
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            for (Map.Entry<String, Object> entry : invocation.getArguments().entrySet()) {
                if (!entry.getKey().equals(Constants.ARGUMENT_FILE_NAME)) {
                    groovyBinding.setProperty(entry.getKey(), entry.getValue());
                }
            }
            manager = new ScriptExecutionManager(this.context, invocation, fileName);
            groovyBinding.setProperty(Constants.BINDING_SCRIPT_MANAGER, manager);
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            return groovyScript.run();
        } catch (Exception e) {
            throw new ScriptException(e);
        } catch (Error e) {
            LOG.log(Level.SEVERE, "Unexpected error when executing script " + fileName + ": " + e.getMessage(), e);
            throw new ScriptException(e.getMessage());
        } finally {
            manager = null;
            groovyShell = null;
            groovyScript = null;
            groovyBinding = null;
        }
    }

    @Override
    public void abort() {
        aborted = true;
        ScriptExecutionManager theManager = manager;
        if(theManager != null) {
            theManager._abort();
            theManager = null;
        }
        groovyShell = null;
        groovyScript = null;
        groovyBinding = null;
    }
}
