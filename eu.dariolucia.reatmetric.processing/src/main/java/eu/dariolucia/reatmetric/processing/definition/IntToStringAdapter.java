/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class IntToStringAdapter extends XmlAdapter<String, Integer> {
    @Override
    public String marshal(Integer s) throws Exception {
        return "#" + s;
    }

    @Override
    public Integer unmarshal(String v) throws Exception {
        return Integer.parseInt(v.substring(1));
    }
}
