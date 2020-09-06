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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition {

    private static final Logger LOG = Logger.getLogger(ExpressionDefinition.class.getName());

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
    // Javascript support via GraalVM.js (slow at the moment, buggy if caching of Engine and Source is done)
    // https://github.com/graalvm/graaljs/issues/268
    // ----------------------------------------------------------------------------------------------------------------

    // TODO: if the jsEngine and the jsSource are cached, then you have memory leak
    // private transient Source jsSource;
    // private transient Engine jsEngine;

    private Object executeJs(IBindingResolver resolver, Map<String, Object> additionalBindings) throws ScriptException {
        try (Engine jsEngine = Engine.create()) {
            Source jsSource = Source.create("js", expression);
            // if(jsEngine == null) {
            //     jsEngine = Engine.create();
            //     jsSource = Source.create("js", expression);
            // }
            try (Context context = Context.newBuilder()
                    .engine(jsEngine)
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
                Value returnValue = context.eval(jsSource);
                return returnValue.as(Object.class);
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Python support
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
            return fromPythonObject(obj, expectedReturnValueType);
        } else {
            return null;
        }
    }

    private Object fromPythonObject(PyObject obj, ValueTypeEnum expectedReturnValueType) throws ScriptException {
        switch(expectedReturnValueType) {
            case BOOLEAN: return obj.asInt() != 0;
            case ENUMERATED: return obj.asInt();
            case REAL: return obj.asDouble();
            case UNSIGNED_INTEGER:
            case SIGNED_INTEGER: return obj.asLong();
            case CHARACTER_STRING: return obj.asString();
            default: throw new ScriptException("Return type " + expectedReturnValueType + " not supported for python expressions");
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

    private Object toBindingProperty(PropertyBinding binding, IEntityBinding resolve) throws ScriptException {
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
}
