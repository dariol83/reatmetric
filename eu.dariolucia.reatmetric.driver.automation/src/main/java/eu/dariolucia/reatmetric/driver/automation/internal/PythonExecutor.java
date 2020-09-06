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
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import javax.script.ScriptException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PythonExecutor implements IScriptExecutor {

    private static final Logger LOG = Logger.getLogger(PythonExecutor.class.getName());

    private final String contents;
    private final IActivityHandler.ActivityInvocation invocation;
    private final String fileName;
    private final IServiceCoreContext context;
    private final String pythonApiData;

    private volatile PythonInterpreter pythonEngine;
    private volatile boolean aborted;
    private volatile ScriptExecutionManager manager;

    public PythonExecutor(IServiceCoreContext context, String initData, String contents, IActivityHandler.ActivityInvocation activityInvocation, String fileName) {
        this.contents = contents;
        this.invocation = activityInvocation;
        this.fileName = fileName;
        this.context = context;
        this.pythonApiData = initData;
    }

    @Override
    public Object execute() throws ScriptException {
        PyCode pythonCode;
        try {
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            pythonEngine = new PythonInterpreter();
            pythonCode = pythonEngine.compile(pythonApiData + "\n\n" + contents);
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            for (Map.Entry<String, Object> entry : invocation.getArguments().entrySet()) {
                if (!entry.getKey().equals(Constants.ARGUMENT_FILE_NAME)) {
                    pythonEngine.set(entry.getKey(), entry.getValue());
                }
            }
            manager = new ScriptExecutionManager(this.context, invocation, fileName);
            pythonEngine.set(Constants.BINDING_SCRIPT_MANAGER, manager);
            if(aborted) {
                throw new IllegalStateException("Script " + fileName + " aborted");
            }
            PyObject obj = pythonEngine.eval(pythonCode);
            if(obj != null) {
                Object returnObj = obj.__tojava__(Object.class);
                if(returnObj == null) {
                    // Thanks to https://stackoverflow.com/questions/1887320/get-data-back-from-jython-scripts-using-jsr-223
                    obj = pythonEngine.get(Constants.PYTHON_RESULT_NAME);
                    if(obj != null) {
                        return obj.__tojava__(Object.class);
                    } else {
                        return null;
                    }
                } else {
                    return returnObj;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new ScriptException(e);
        } catch (Error e) {
            LOG.log(Level.SEVERE, "Unexpected error when executing script " + fileName + ": " + e.getMessage(), e);
            throw new ScriptException(e.getMessage());
        } finally {
            manager = null;
            PythonInterpreter toClose = pythonEngine;
            if(toClose != null) {
                toClose.close();
            }
            pythonEngine = null;
            pythonCode = null;
        }
    }

    @Override
    public void abort() {
        aborted = true;
        ScriptExecutionManager theManager = manager;
        if(theManager != null) {
            theManager._abort();
        }
        PythonInterpreter toClose = pythonEngine;
        if(toClose != null) {
            toClose.close();
        }
    }
}
