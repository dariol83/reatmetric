/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.definition;

import eu.dariolucia.reatmetric.api.processing.scripting.IBindingResolver;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class XYCalibration extends CalibrationDefinition {

    @XmlElement(name = "point", required = true)
    private List<XYCalibrationPoint> points;

    @XmlAttribute
    private boolean extrapolate = false;

    public XYCalibration() {
    }

    public XYCalibration(List<XYCalibrationPoint> points, boolean extrapolate) {
        this.points = points;
        this.extrapolate = extrapolate;
    }

    public List<XYCalibrationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<XYCalibrationPoint> points) {
        this.points = points;
    }

    public boolean isExtrapolate() {
        return extrapolate;
    }

    public void setExtrapolate(boolean extrapolate) {
        this.extrapolate = extrapolate;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Transient objects
    // ----------------------------------------------------------------------------------------------------------------

    private transient List<XYSegment> segments = new ArrayList<>();

    @Override
    public Object calibrate(Object valueToCalibrate, IBindingResolver resolver) throws CalibrationException {
        if(segments.isEmpty()) {
            for(int i = 0; i < points.size() - 1; ++i) {
                segments.add(new XYSegment(
                        points.get(i).getX(),
                        points.get(i).getY(),
                        points.get(i + 1).getX(),
                        points.get(i + 1).getY()));
            }
            segments.sort(Comparator.comparingDouble(o -> o.x1));
        }
        double val = convertToDouble(valueToCalibrate);
        if(segments.get(0).isBefore(val)) {
            // if extrapolate, then extrapolate to the left
            if(extrapolate) {
                return segments.get(0).interpolate(val);
            } else {
                throw new CalibrationException("Value " + val + " outside ranges and no extrapolation flag set");
            }
        }
        for(XYSegment seg : segments) {
            if(seg.contains(val)) {
                return seg.interpolate(val);
            }
        }
        if(segments.get(segments.size() - 1).isAfter(val)) {
            // if extrapolate, then extrapolate to the right
            if(extrapolate) {
                return segments.get(segments.size() - 1).interpolate(val);
            } else {
                throw new CalibrationException("Value " + val + " outside ranges and no extrapolation flag set");
            }
        }
        return valueToCalibrate;
    }

    private static class XYSegment {

        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        public XYSegment(double x1, double y1, double x2, double y2) {
            if(x1 < x2) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
            } else {
                this.x1 = x2;
                this.y1 = y2;
                this.x2 = x1;
                this.y2 = y1;
            }
        }

        public boolean isBefore(double val) {
            return val < x1;
        }

        public boolean contains(double val) {
            return val >= x1 && val <= x2;
        }

        public double interpolate(double val) {
            return (y1*(x2 - val) + y2*(val - x1))/(x2 - x1);
        }

        public boolean isAfter(double val) {
            return val > x2;
        }
    }
}
