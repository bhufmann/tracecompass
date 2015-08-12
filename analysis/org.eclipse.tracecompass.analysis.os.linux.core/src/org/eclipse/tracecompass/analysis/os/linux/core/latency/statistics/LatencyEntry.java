/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.os.linux.core.latency.statistics;

public class LatencyEntry {
    private long min;
    private long max;
    private long sum;
    private long nbSegments;

    public LatencyEntry() {
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
        this.sum = 0;
        this.nbSegments = 0;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public long getNbSegments() {
        return nbSegments;
    }

    public double getAverage() {
        return ((double) sum) / nbSegments;
    }

    public void update (long value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
        sum += value;
        nbSegments++;
    }
}
