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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.AbstractTmfMipmapStateProvider;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class LatencyMipMapStateProvider extends AbstractTmfMipmapStateProvider {

    private static final int VERSION = 0;

    @NonNull private static final String ID = "Latency Statistics"; //$NON-NLS-1$

    public static final String TEST_ATTRIBUTE_NAME = "total";
    private static final int RESOLUTION = 16;

    private final String ERROR_ATTRIBUTE_NOT_FOUND = "Error : Impossible to find the attribute"; //$NON-NLS-1$
    private final String ERROR_INVALID_STATE_VALUE = "Error : Invalid state value"; //$NON-NLS-1$
    private final String ERROR_INVALID_TIMESTAMP = "Error : Invalid timestamp"; //$NON-NLS-1$

    LatencyMipMapStateProvider (@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        // TODO Auto-generated method stub
        return new LatencyMipMapStateProvider(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        final long ts = event.getTimestamp().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        try {
            int quark = ss.getQuarkAbsoluteAndAdd(TEST_ATTRIBUTE_NAME);
            ISegment value = (ISegment) event.getContent().getField("Segment").getValue();
            modifyMipmapAttribute(ts, TmfStateValue.newValueLong(value.getLength()), quark, MIN | MAX | ARITHM_AVG, RESOLUTION);
        } catch (TimeRangeException e) {
            Activator.logError(ERROR_INVALID_TIMESTAMP, e);
        } catch (AttributeNotFoundException e) {
            Activator.logError(ERROR_ATTRIBUTE_NOT_FOUND, e);
        } catch (StateValueTypeException e) {
            Activator.logError(ERROR_INVALID_STATE_VALUE, e);
        }
    }
}
