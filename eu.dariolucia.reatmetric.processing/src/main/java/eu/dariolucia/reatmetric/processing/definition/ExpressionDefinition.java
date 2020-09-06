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

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;
import eu.dariolucia.reatmetric.api.processing.scripting.IEntityBinding;
import eu.dariolucia.reatmetric.api.processing.scripting.IEventBinding;
import eu.dariolucia.reatmetric.api.processing.scripting.IParameterBinding;
import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition {

    public final static String PYTHON_RESULT_NAME = "_result";

    @XmlElement(name="expression", required = true)
    private String expression;

    @XmlElement(name="symbol")
    private List<SymbolDefinition> symbols = new LinkedList<>();

    @XmlAttribute(name="dialect")
    private ExpressionDialect dialect = ExpressionDialect.JS;

    public ExpressionDefinition() {
    }

    public ExpressionDefinition(String expression, List<SymbolDefinition> symbols) {
        this.expression = expression;
        this.symbols = symbols;
    }

    public ExpressionDefinition(String expression, List<SymbolDefinition> symbols, ExpressionDialect dialect) {
        this.expression = expression;
        this.symbols = symbols;
        this.dialect = dialect;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public List<SymbolDefinition> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolDefinition> symbols) {
        this.symbols = symbols;
    }

    public ExpressionDialect getDialect() {
        return dialect;
    }

    public void setDialect(ExpressionDialect dialect) {
        this.dialect = dialect;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient state, runtime methods
    // ----------------------------------------------------------------------------------------------------------------

    public Object execute(IBindingResolver resolver, Map<String, Object> additionalBindings, ValueTypeEnum expectedReturnValueType) throws ScriptException {
        switch(dialect) {
            case JS: return executeJs(resolver, additionalBindings);
            case GROOVY: return executeGroovy(resolver, additionalBindings);
            case PYTHON: return executePython(resolver, additionalBindings, expectedReturnValueType);
            default:
                throw new ScriptException("Dialect not supported: " + dialect);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Javascript support via GraalVM.js (slow at the moment, buggy if caching of Engine and Source is done).
    // SoftReference need to overcome bug: https://github.com/graalvm/graaljs/issues/268
    // ----------------------------------------------------------------------------------------------------------------

    private transient volatile SoftReference<GraalVmJsCache> jsCache = new SoftReference<>(null);

    private Object executeJs(IBindingResolver resolver, Map<String, Object> additionalBindings) throws ScriptException {
        GraalVmJsCache cached = jsCache.get();
        if(cached == null) {
            // It was gone
            Engine jsEngine = Engine.create();
            Source jsSource = Source.create("js", expression);
            cached = new GraalVmJsCache(jsEngine, jsSource);
            jsCache = new SoftReference<>(cached);
        }
        return cached.process(resolver, additionalBindings, symbols);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Python support: python support for expression is provided only for simple statements and it is slow.
    // It is deprecated.
    // ----------------------------------------------------------------------------------------------------------------

    private transient PythonInterpreter pythonEngine;
    private transient PyCode pythonCode;

    private Object executePython(IBindingResolver resolver, Map<String, Object> additionalBindings, ValueTypeEnum expectedReturnValueType) throws ScriptException {
        // One engine per expression, to avoid concurrent access: might not be wise from a memory POV...
        if(pythonEngine == null) {
            pythonEngine = new PythonInterpreter();
            pythonCode = pythonEngine.compile(expression);
        }
        // Update the bindings
        for(SymbolDefinition sd : symbols) {
            pythonEngine.set(sd.getName(), toBindingProperty(sd.getBinding(), resolver.resolve(sd.getReference())));
        }
        if(additionalBindings != null) {
            for(Map.Entry<String, Object> add : additionalBindings.entrySet()) {
                pythonEngine.set(add.getKey(), add.getValue());
            }
        }
        // Evaluate the script
        PyObject obj = pythonEngine.eval(pythonCode);
        if(obj != null) {
            Object returnObj = obj.__tojava__(Object.class);
            if(returnObj == null) {
                // Thanks to https://stackoverflow.com/questions/1887320/get-data-back-from-jython-scripts-using-jsr-223
                obj = pythonEngine.get(PYTHON_RESULT_NAME);
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
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Groovy support
    // ----------------------------------------------------------------------------------------------------------------

    private transient GroovyShell groovyShell;
    private transient Binding groovyBinding;
    private transient Script groovyScript;

    private Object executeGroovy(IBindingResolver resolver, Map<String, Object> additionalBindings) throws ScriptException {
        try {
            if (groovyShell == null) {
                groovyBinding = new Binding();
                groovyShell = new GroovyShell();
                groovyScript = groovyShell.parse(expression);
                groovyScript.setBinding(groovyBinding);
            }
        } catch (CompilationFailedException e) {
            throw new ScriptException(e);
        }
        // Update the bindings
        for(SymbolDefinition sd : symbols) {
            groovyBinding.setProperty(sd.getName(), toBindingProperty(sd.getBinding(), resolver.resolve(sd.getReference())));
        }
        if(additionalBindings != null) {
            for(Map.Entry<String, Object> entry : additionalBindings.entrySet()) {
                groovyBinding.setProperty(entry.getKey(), entry.getValue());
            }
        }
        try {
            return groovyScript.run();
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private static Object toBindingProperty(PropertyBinding binding, IEntityBinding resolve) throws ScriptException {
        switch (binding) {
            case OBJECT: return resolve;
            case PATH: return resolve.path();
            case GEN_TIME: return resolve.generationTime();
            case RCT_TIME: return resolve.receptionTime();
            case ALARM_STATE: {
                if(resolve instanceof IParameterBinding) {
                    return ((IParameterBinding) resolve).alarmState();
                }
                break;
            }
            case ROUTE: {
                if(resolve instanceof IParameterBinding) {
                    return ((IParameterBinding) resolve).route();
                }
                if(resolve instanceof IEventBinding) {
                    return ((IEventBinding) resolve).route();
                }
                break;
            }
            case SOURCE: {
                if(resolve instanceof IEventBinding) {
                    return ((IEventBinding) resolve).source();
                }
                break;
            }
            case VALIDITY: {
                if(resolve instanceof IParameterBinding) {
                    return ((IParameterBinding) resolve).validity();
                }
                break;
            }
            case ENG_VALUE: {
                if(resolve instanceof IParameterBinding) {
                    return ((IParameterBinding) resolve).value();
                }
                break;
            }
            case SOURCE_VALUE: {
                if(resolve instanceof IParameterBinding) {
                    return ((IParameterBinding) resolve).rawValue();
                }
                break;
            }
            case QUALIFIER: {
                if(resolve instanceof IEventBinding) {
                    return ((IEventBinding) resolve).qualifier();
                }
                break;
            }
            default:
                throw new ScriptException("Cannot resolve property binding " + binding + " against object of type " + resolve.getClass());
        }
        throw new ScriptException("Cannot resolve property binding " + binding + " against object of type " + resolve.getClass());
    }

    private static class GraalVmJsCache {
        private final Engine engine;
        private final Source source;

        public GraalVmJsCache(Engine engine, Source source) {
            this.engine = engine;
            this.source = source;
        }

        public Engine getEngine() {
            return engine;
        }

        public Source getSource() {
            return source;
        }

        public synchronized Object process(IBindingResolver resolver, Map<String, Object> additionalBindings, List<SymbolDefinition> symbols) throws ScriptException {
            try (Context context = Context.newBuilder()
                    .engine(engine)
                    .allowAllAccess(true)
                    .build()) {
                Value bindings = context.getBindings("js");
                // Update the bindings
                for(SymbolDefinition sd : symbols) {
                    bindings.putMember(sd.getName(), toBindingProperty(sd.getBinding(), resolver.resolve(sd.getReference())));
                }
                if(additionalBindings != null) {
                    for(Map.Entry<String, Object> entry : additionalBindings.entrySet()) {
                        bindings.putMember(entry.getKey(), entry.getValue());
                    }
                }
                Value returnValue = context.eval(source);
                return returnValue.as(Object.class);
            }
        }

        // I know, it is deprecated, if you suggest a better -working- approach with Cleaner, I will be happy to implement it here and give you credits
        @Override
        protected synchronized void finalize() throws Throwable {
            engine.close(true);
            super.finalize();
        }
    }
}
