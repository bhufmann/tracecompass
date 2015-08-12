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
package org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap;

import java.util.List;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue.Type;

/**
 * The average mipmap feature.
 *
 * Each mipmap state value is the arithmetical average of all the lower-level
 * state values it covers. The state value is a Double.
 */
public class ArithmAvgMipmapFeature extends TmfMipmapFeature {

    /**
     * Constructor
     *
     * @param baseQuark
     *            The quark for the attribute we want to mipmap
     * @param mipmapQuark
     *            The quark of the mipmap feature attribute
     * @param mipmapResolution
     *            The resolution that will be use in the mipmap
     * @param ss
     *            The state system in which to insert the state changes
     */
    public ArithmAvgMipmapFeature(final int baseQuark, final int mipmapQuark, final int mipmapResolution, final ITmfStateSystemBuilder ss) {
        super(baseQuark, mipmapQuark, mipmapResolution, ss);
    }

    @Override
    protected ITmfStateValue computeMipmapValue(List<ITmfStateInterval> lowerIntervals, long startTime, long endTime) {
        double sum = 0.0;
        try {
            for (ITmfStateInterval interval : lowerIntervals) {
                ITmfStateValue value = interval.getStateValue();
                if (value.getType() == Type.DOUBLE) {
                    sum += value.unboxDouble();
                } else {
                    sum += value.unboxLong();
                }
            }
        } catch (StateValueTypeException e) {
            e.printStackTrace();
        }
        double average = sum / lowerIntervals.size();
        ITmfStateValue avgValue = TmfStateValue.newValueDouble(average);
        return avgValue;
    }
}
