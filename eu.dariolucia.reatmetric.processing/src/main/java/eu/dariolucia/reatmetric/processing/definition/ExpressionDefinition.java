/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.processing.definition.scripting.IBindingResolver;

import javax.script.*;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition {

    private static final Logger LOG = Logger.getLogger(ExpressionDefinition.class.getName());

    @XmlElement(name="expression", required = true)
    private String expression;

    @XmlElementWrapper(name="mapping")
    @XmlElement(name="symbol")
    private List<SymbolDefinition> symbols;

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

    public Object execute(ScriptEngine engine, IBindingResolver resolver, Map<String, Object> additionalBindings) throws ScriptException {
        // First time: try to compile the script
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
        // Create/update the bindings
        if(bindings == null) {
            bindings = engine.createBindings();
        }
        for(SymbolDefinition sd : symbols) {
            bindings.put(sd.getName(), resolver.resolve(sd.getReference().getId()));
        }
        if(additionalBindings != null) {
            bindings.putAll(additionalBindings);
        }
        // Evaluate the script
        try {
            if (compiledScript != null) {
                return compiledScript.eval(bindings);
            } else {
                return engine.eval(expression, bindings);
            }
        } catch(ScriptException e) {
            LOG.log(Level.SEVERE, "Expression " + expression + " cannot be evaluated: " + e.getMessage(), e);
            throw e;
        }
    }
}
