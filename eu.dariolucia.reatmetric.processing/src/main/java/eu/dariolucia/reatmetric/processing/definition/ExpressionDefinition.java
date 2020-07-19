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

import javax.script.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition {

    private static final Logger LOG = Logger.getLogger(ExpressionDefinition.class.getName());

    @XmlElement(name="expression", required = true)
    private String expression;

    @XmlElement(name="symbol")
    private List<SymbolDefinition> symbols = new LinkedList<>();

    public ExpressionDefinition() {
    }

    public ExpressionDefinition(String expression, List<SymbolDefinition> symbols) {
        this.expression = expression;
        this.symbols = symbols;
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

    // ----------------------------------------------------------------------------------------------------------------
    // Transient state, runtime methods
    // ----------------------------------------------------------------------------------------------------------------

    private transient CompiledScript compiledScript;
    private transient boolean canBeCompiled = true;
    private transient Bindings bindings;
    private transient ScriptEngine engine;

    public Object execute(IBindingResolver resolver, Map<String, Object> additionalBindings) throws ScriptException {
        // One engine per expression, to avoid concurrent access: might not be wise from a memory POV...
        if(engine == null) {
            engine = new ScriptEngineManager().getEngineByName("graal.js");
            bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("polyglot.js.allowAllAccess", true);
            bindings.put("polyglot.js.allowHostAccess", true);
        }
        // First time: try to compile the automation
        if(canBeCompiled && compiledScript == null) {
            if (engine instanceof Compilable) {
                Compilable cc = (Compilable) engine;
                try {
                    compiledScript = cc.compile(expression);
                } catch (ScriptException e) {
                    LOG.log(Level.SEVERE, "Expression " + expression + " cannot be compiled: " + e.getMessage(), e);
                    canBeCompiled = false;
                }
            } else {
                canBeCompiled = false;
            }
        }
        // Update the bindings
        for(SymbolDefinition sd : symbols) {
            bindings.put(sd.getName(), toBindingProperty(sd.getBinding(), resolver.resolve(sd.getReference())));
        }
        if(additionalBindings != null) {
            bindings.putAll(additionalBindings);
        }
        // Evaluate the automation
        if (compiledScript != null) {
            return compiledScript.eval(bindings);
        } else {
            return engine.eval(expression, bindings);
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
