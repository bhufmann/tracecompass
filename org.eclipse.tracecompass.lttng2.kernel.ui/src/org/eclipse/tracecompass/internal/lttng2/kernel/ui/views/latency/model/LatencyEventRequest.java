/*******************************************************************************
 * Copyright (c) 2011 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency.model;

import org.eclipse.tracecompass.internal.lttng2.kernel.core.latency.analyzer.EventMatcher;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

/**
 * <b><u>LatencyEventRequest</u></b>
 * <p>
 */
public class LatencyEventRequest extends TmfEventRequest {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    final private LatencyController fController;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------
    /**
     * Constructor
     * @param controller The Latency Controller
     * @param range The request time range
     * @param rank The start index
     * @param nbEvents The requested number of events
     * @param execType Background or Foreground
     */
    public LatencyEventRequest(LatencyController controller, TmfTimeRange range, int rank, int nbEvents, ITmfEventRequest.ExecutionType execType) {
        super(CtfTmfEvent.class, range, rank, nbEvents, execType);
        fController = controller;
        EventMatcher.getInstance().clearStack();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public void handleData(ITmfEvent event) {
        super.handleData(event);

        CtfTmfEvent startEvent = EventMatcher.getInstance().process(event);

        if (startEvent != null) {
            long latency = event.getTimestamp().getValue() - startEvent.getTimestamp().getValue();
            fController.handleData(getNbRead(), startEvent.getTimestamp().getValue(), latency, event.getTrace());
        }
    }

    @Override
    public void handleCompleted() {
        fController.handleCompleted();
        super.handleCompleted();
    }

    @Override
    public void handleCancel() {
        EventMatcher.getInstance().clearStack();
        fController.handleCancel();
        super.handleCancel();
    }
}
