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

import javax.script.ScriptException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to define an expression, which is characterized by:
 * <ul>
 *     <li>a string, defining the expression itself</li>
 *     <li>a list of {@link SymbolDefinition}, binding a given name to a system entity property in the processing model</li>
 *     <li>a dialect, which is limited to Groovy in this implementation</li>
 * </ul>
 *
 * In general, an expression can return any value. Some specific applications, e.g. the use of expressions for
 * the computation of a {@link ValidityCondition}, might restrict the returned value type to specific ones (e.g.
 * boolean).
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition implements Serializable {

    @XmlElement(name="expression", required = true)
    private String expression;

    @XmlElement(name="symbol")
    private List<SymbolDefinition> symbols = new LinkedList<>();

    @XmlAttribute(name="dialect")
    private ExpressionDialect dialect = ExpressionDialect.GROOVY;

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

    /**
     * The expression to be evaluated.
     * <p></p>
     * Element: expression
     *
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * The list of {@link SymbolDefinition} that appear in the specified expression, for which a binding to a
     * processing model system entity (and related property) is needed. Such binding is then resolved at runtime.
     * <p></p>
     * Elements: symbol
     *
     * @return the list of {@link SymbolDefinition}
     */
    public List<SymbolDefinition> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolDefinition> symbols) {
        this.symbols = symbols;
    }

    /**
     * The dialect/language of the expression. Only {@link ExpressionDialect#GROOVY} is supported in this
     * implementation.
     * <p></p>
     * Attribute: dialect
     *
     * @return the expression dialect ({@link ExpressionDialect#GROOVY})
     */
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
        if (dialect == ExpressionDialect.GROOVY) {
            return executeGroovy(resolver, additionalBindings);
        }
        throw new ScriptException("Dialect not supported: " + dialect);
    }

    /**
     * This method is called upon initialisation by a separate thread that starts preloading the engines and the expression code.
     *
     * @throws ScriptException in case of issues dealing with the script engine
     */
    public void preload() throws ScriptException {
        if (dialect == ExpressionDialect.GROOVY) {
            initGroovyCache();
        } else {
            throw new ScriptException("Dialect not supported: " + dialect);
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
            initGroovyCache();
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

    // Synchronized due to async cache building
    private synchronized void initGroovyCache() {
        if (groovyShell == null) {
            groovyBinding = new Binding();
            groovyShell = new GroovyShell();
            groovyScript = groovyShell.parse(expression);
            groovyScript.setBinding(groovyBinding);
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
}
