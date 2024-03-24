/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.services.impl;

import eu.dariolucia.reatmetric.api.common.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Simple linear regression calculator, based on the code of
 * (<a href="https://notebook.community/cleuton/datascience/java/JavaRegression">Robert Sedgewick and Kevin Wayne</a>)
 * and updated to support BigDecimal as data type for increased resolution.
 */
public class LinearRegression {

    private LinearRegression() {
        throw new IllegalAccessError("Not expected to be instantiated");
    }

    /**
     * Performs a linear regression on the data points.
     *
     * @param  dataSet the input values
     * @return the slop (first) and intercept (second) pair
     * @throws IllegalArgumentException if the dataset is null or with less than 2 points
     */
    public static Pair<BigDecimal, BigDecimal> calculate(List<Pair<BigDecimal, BigDecimal>> dataSet) {
        if(dataSet == null || dataSet.size() < 2) {
            throw new IllegalArgumentException("Data set null or with less than 2 points");
        }
        int n = dataSet.size();

        // First scan
        BigDecimal sumx = BigDecimal.valueOf(0.0);
        BigDecimal sumy = BigDecimal.valueOf(0.0);
        BigDecimal sumx2 = BigDecimal.valueOf(0.0);
        for (Pair<BigDecimal, BigDecimal> iTh : dataSet) {
            sumx = sumx.add(iTh.getFirst());
            sumx2 = sumx2.add(iTh.getFirst().pow(2));
            sumy = sumy.add(iTh.getSecond());
        }
        BigDecimal xbar = sumx.divide(BigDecimal.valueOf(n), 9, RoundingMode.HALF_UP);
        BigDecimal ybar = sumy.divide(BigDecimal.valueOf(n), 9, RoundingMode.HALF_UP);

        // Second scan
        BigDecimal xxbar = BigDecimal.valueOf(0.0);
        BigDecimal yybar = BigDecimal.valueOf(0.0);
        BigDecimal xybar = BigDecimal.valueOf(0.0);
        for (Pair<BigDecimal, BigDecimal> iTh : dataSet) {
            xxbar = xxbar.add((iTh.getFirst().subtract(xbar)).multiply(iTh.getFirst().subtract(xbar)));
            yybar = yybar.add((iTh.getSecond().subtract(ybar)).multiply(iTh.getSecond().subtract(ybar)));
            xybar = xybar.add((iTh.getFirst().subtract(xbar)).multiply(iTh.getSecond().subtract(ybar)));
        }
        BigDecimal slope  = xybar.divide(xxbar, 9, RoundingMode.HALF_UP);
        BigDecimal intercept = ybar.subtract(slope.multiply(xbar));
        return Pair.of(slope, intercept);
    }

}
