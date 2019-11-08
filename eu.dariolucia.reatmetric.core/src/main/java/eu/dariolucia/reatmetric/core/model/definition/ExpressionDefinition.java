/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.core.model.definition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExpressionDefinition {

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
}
