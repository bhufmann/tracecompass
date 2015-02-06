/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.scripting.trace;

import java.text.ParseException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfOpenTraceHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;

/**
 * @author eedbhu
 */
public class TraceManager {

    /**
     * Opens a trace
     * @param traceToOpen
     *                trace to open (absolute path)
     * @return 0
     */
    @WrapToScript
    public int open(final String traceToOpen) {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                IProject project = TmfProjectRegistry.createProject(TmfCommonConstants.DEFAULT_TRACE_PROJECT_NAME, null, new NullProgressMonitor());
                try {
                    TmfTraceFolder destinationFolder = TmfProjectRegistry.getProject(project, true).getTracesFolder();
                    TmfOpenTraceHelper.openTraceFromPath(destinationFolder, traceToOpen, null);
                } catch (CoreException e) {
                }
            }
        });
        return 0;
    }

    /**
     * Syncs to time of current trace
     * @param time
     *              String of time
     * @return 0 if successful else false
     */
    @WrapToScript
    public int selectTime(String time) {
        long value = 0;
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTimeRange range = trace.getTimeRange();
            try {
                value = TmfTimestampFormat.getDefaulTimeFormat().parseValue(time, range.getStartTime().getValue());
            } catch (ParseException e) {
                return 1;
            }
            ITmfTimestamp ts = new TmfTimestamp(value, ITmfTimestamp.NANOSECOND_SCALE);
            TmfSignalManager.dispatchSignal(new TmfTimeSynchSignal(this, ts, ts));
        }
        return 0;
    }

    /**
     * Syncs to time of current trace
     * @param start
     *              String of selection begin time
     * @param end
     *              String of selection end time
     * @return 0 if successful else false
     */
    @WrapToScript
    public int selectTimeRange(String start, String end) {
        long startValue = 0;
        long endValue = 0;
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTimeRange range = trace.getTimeRange();
            try {
                startValue = TmfTimestampFormat.getDefaulTimeFormat().parseValue(start, range.getStartTime().getValue());
                endValue = TmfTimestampFormat.getDefaulTimeFormat().parseValue(end, range.getStartTime().getValue());
            } catch (ParseException e) {
                return 1;
            }
            TmfSignalManager.dispatchSignal(new TmfTimeSynchSignal(
                    this,
                    new TmfTimestamp(startValue, ITmfTimestamp.NANOSECOND_SCALE),
                    new TmfTimestamp(endValue, ITmfTimestamp.NANOSECOND_SCALE)));
        }
        return 0;
    }


    /**
     * Syncs to time range of current trace
     * @param start
     *              String of start time
     * @param end
     *              String of end time
     * @return 0 if successful else false
     */
    @WrapToScript
    public int syncToRange(String start, String end) {
        long startTime = 0;
        long endTime = 0;
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTimeRange traceRange = trace.getTimeRange();
            try {
                startTime = TmfTimestampFormat.getDefaulTimeFormat().parseValue(start, traceRange.getStartTime().getValue());
                endTime = TmfTimestampFormat.getDefaulTimeFormat().parseValue(end, traceRange.getStartTime().getValue());
            } catch (ParseException e) {
                return 1;
            }
            TmfTimeRange range = new TmfTimeRange(
                    new TmfTimestamp(startTime, ITmfTimestamp.NANOSECOND_SCALE),
                    new TmfTimestamp(endTime, ITmfTimestamp.NANOSECOND_SCALE));

            TmfSignalManager.dispatchSignal(new TmfRangeSynchSignal(this, range));
        }
        return 0;
    }

    /**
     * @return the max event delta of 2 consecutive events
     */
    @WrapToScript
    public String eventDeltaMax () {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
//        final List<String> result = new ArrayList<>(1000);
        final ITmfTimestamp[] fDelta = new ITmfTimestamp[1];
        fDelta[0] = TmfTimestamp.BIG_BANG;
        if (trace != null) {
            // Build a background request for all the trace data. The index is
            // updated as we go by readNextEvent().
            ITmfEventRequest request = new TmfEventRequest(ITmfEvent.class,
                    TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND) {
                ITmfTimestamp fPrevTime = null;

                @Override
                public void handleData(final ITmfEvent event) {
                    super.handleData(event);

                    if (fPrevTime != null) {
                        ITmfTimestamp delta = event.getTimestamp().getDelta(fPrevTime);
                        if (delta.compareTo(fDelta[0]) > 0) {
                            fDelta[0] = delta;
                        }
                    }
                    fPrevTime = event.getTimestamp();
                }
            };

            // Submit the request and wait for completion if required
            trace.sendRequest(request);
                try {
                    request.waitForCompletion();
                } catch (final InterruptedException e) {
                }
        }
        return fDelta[0].toString();
    }

}
