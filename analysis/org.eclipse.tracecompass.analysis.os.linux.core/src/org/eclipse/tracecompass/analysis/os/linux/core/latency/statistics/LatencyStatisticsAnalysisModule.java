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

import java.io.File;
import java.io.IOException;
//import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.LatencyAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.SystemCall;
import org.eclipse.tracecompass.internal.tmf.core.statesystem.mipmap.TmfStateSystemOperations;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfNanoTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class LatencyStatisticsAnalysisModule extends TmfAbstractAnalysisModule {

    public static String ID = "org.eclipse.tracecompass.analysis.os.linux.core.latency.statistics";

    private @Nullable LatencyAnalysis statsMod;
    @NonNull private static final String SSID = "mimap-latency";
    private static ITmfStateSystem ssq;

    private LatencyEntry fTotalStats;

    Map<String, LatencyEntry> perSyscallStats = new HashMap<>();

    Map<Integer, LatencyEntry> perCpu = new HashMap<>();

    Map<Integer, Map<String, LatencyEntry>> perCpuSysCall = new HashMap<>();

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        boolean ret = super.setTrace(trace);

        statsMod = TmfTraceUtils.getAnalysisModuleOfClass(trace, LatencyAnalysis.class, LatencyAnalysis.ID);
        LatencyAnalysis latency = statsMod;
        if (latency == null) {
            return false;
        }

        latency.schedule();

        return ret;
    }
    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        System.out.println(">>> Start Latency Statistical Analysis");

        System.out.println(  ">>> Start Latency Analysis");
        LatencyAnalysis latency = statsMod;
        ITmfTrace trace = getTrace();
        if ((latency == null) || (trace == null)) {
            return false;
        }
        latency.waitForCompletion();
        System.out.println("  >>> Latency Analysis completed");

        ISegmentStore<ISegment> store = latency.getResults();

        if (store != null) {

            boolean result = calculateTotalManual(store, monitor);

            if (!result) {
                return false;
            }

            result = calculateTotalPerSyscall(store, monitor);
            if (!result) {
                return false;
            }

            result = calculateTotalPerCpuPerSyscall(store, monitor);
            if (!result) {
                return false;
            }
            // periodic every delta from the start time.
            result =  calculatePeriodicManual(trace, store, 1000000000L, monitor);
            if (!result) {
                return false;
            }

            result = calculateMipMap(trace, store, monitor);
            if (!result) {
                return false;
            }
        }
        System.out.println(">>> Latency Statistical Analysis completed");
        return false;
    }

    private boolean calculateTotalManual(@NonNull ISegmentStore<ISegment> store, IProgressMonitor monitor) {
        fTotalStats = new LatencyEntry();
        Iterator<ISegment> iter = store.iterator();
        while (iter.hasNext()) {
            if (monitor.isCanceled()) {
                return false;
            }
            ISegment segment = iter.next();
            fTotalStats.update(segment.getLength());
        }
        System.out.println("    total: nbSegments=" + fTotalStats.getNbSegments() + ", min=" + fTotalStats.getMin() + ", max=" + fTotalStats.getMax() + ", avg=" + fTotalStats.getAverage());
        return true;
    }

    private boolean calculateTotalPerSyscall(@NonNull ISegmentStore<ISegment> store, IProgressMonitor monitor) {
        perSyscallStats = new HashMap<>();

        Iterator<ISegment> iter = store.iterator();
        while (iter.hasNext()) {
            if (monitor.isCanceled()) {
                return false;
            }
            ISegment segment = iter.next();
            if (segment instanceof SystemCall) {
                SystemCall syscall = (SystemCall) segment;
                LatencyEntry values = perSyscallStats.get(syscall.getName());
                if (values == null) {
                    values = new LatencyEntry();
                }
                values.update(segment.getLength());
                perSyscallStats.put(syscall.getName(), values);
            }
        }

        Iterator<Entry<String, LatencyEntry>> stats = perSyscallStats.entrySet().iterator();
        while (stats.hasNext()) {
            Entry<String, LatencyEntry> statsEntry = stats.next();
            LatencyEntry value = statsEntry.getValue();
            System.out.println("    syscall=" + statsEntry.getKey() + ", nbSegments=" + value.getNbSegments() + ", min=" + value.getMin() + ", max=" + value.getMax() + ", avg=" + value.getAverage());
        }

        return true;
    }

    private boolean calculateTotalPerCpuPerSyscall(@NonNull ISegmentStore<ISegment> store, IProgressMonitor monitor) {

        perCpu = new HashMap<>();
        perCpuSysCall = new HashMap<>();

        Iterator<ISegment> iter = store.iterator();
        while (iter.hasNext()) {
            if (monitor.isCanceled()) {
                return false;
            }
            ISegment segment = iter.next();
            if (segment instanceof SystemCall) {
                SystemCall syscall = (SystemCall) segment;
                Map<String, LatencyEntry> perSysCalls = perCpuSysCall.get(syscall.getCpu());
                if (perSysCalls == null) {
                    perSysCalls = new HashMap<>();
                }
                LatencyEntry values = perSysCalls.get(syscall.getName());
                if (values == null) {
                    values = new LatencyEntry();
                }
                values.update(segment.getLength());

                perSysCalls.put(syscall.getName(), values);
                perCpuSysCall.put(Integer.valueOf(syscall.getCpu()), perSysCalls);
                LatencyEntry perCpuStatsValue = perCpu.get(syscall.getCpu());
                if (perCpuStatsValue == null) {
                    perCpuStatsValue = new LatencyEntry();
                }
                perCpuStatsValue.update(segment.getLength());
                perCpu.put(syscall.getCpu(), perCpuStatsValue);
            }
        }

        Iterator<Entry<Integer, Map<String, LatencyEntry>>> stats = perCpuSysCall.entrySet().iterator();

        while (stats.hasNext()) {
            Entry<Integer, Map<String, LatencyEntry>>  statsEntry = stats.next();
            Map<String, LatencyEntry> value = statsEntry.getValue();
            Iterator<Entry<String, LatencyEntry>> otherIter = value.entrySet().iterator();
            while (otherIter.hasNext()) {
                Entry<String, LatencyEntry> entry = otherIter.next();
                LatencyEntry mySalue = entry.getValue();
                System.out.println("    syscall=" + statsEntry.getKey() + ", nbSegments=" + mySalue.getNbSegments() + ", min=" + mySalue.getMin() + ", max=" + mySalue.getMax() + ", avg=" + mySalue.getAverage());
            }
        }

        Iterator<Entry<Integer, LatencyEntry>> iter2 = perCpu.entrySet().iterator();
        while (iter2.hasNext()) {
            Entry<Integer, LatencyEntry> cpuStats = iter2.next();
            LatencyEntry cpuValue = cpuStats.getValue();
            System.out.println("    cpu=" + cpuStats.getKey() + ", nbSegments=" + cpuValue.getNbSegments() + ", min=" + cpuValue.getMin() + ", max=" + cpuValue.getMax() + ", avg=" + cpuValue.getAverage());
        }

        return true;
    }

    private static boolean calculatePeriodicManual(@NonNull ITmfTrace trace, @NonNull ISegmentStore<ISegment> store, long delta, IProgressMonitor monitor) {
        ITmfTimestamp start = trace.getStartTime();
        ITmfTimestamp end = trace.createTimestamp(start.getValue() + delta);

        while (trace.getEndTime().compareTo(end) >= 0) {
            if (monitor.isCanceled()) {
                return false;
            }
            LatencyEntry values = new LatencyEntry();
            for (ISegment segment : store.getIntersectingElements(start.getValue())) {
                values.update(segment.getLength());
            }
            if (values.getNbSegments() != 0) {
                System.out.println("    " + start + " - " + end);
                System.out.println("    nbSegments=" + values.getNbSegments() + ", min=" + values.getMin() + ", max=" + values.getMax() + ", avg=" + values.getAverage());
            }
            start = end;
            end = trace.createTimestamp(end.getValue() + delta);
        }

        return true;
    }

    private static boolean calculateMipMap(@NonNull ITmfTrace trace, @NonNull ISegmentStore<ISegment> store, IProgressMonitor monitor) {
        LatencyMipMapStateProvider mmp = new LatencyMipMapStateProvider(trace);

        try {
            String directory = TmfTraceManager.getSupplementaryFileDir(trace);
            File htFile = new File(directory + "latency.ht");
            //            createInMemoryBackend(SSID, 0)
            IStateHistoryBackend be = StateHistoryBackendFactory.createHistoryTreeBackendNewFile(SSID,  htFile, mmp.getVersion(), trace.getStartTime().getValue(), 1000);
            ITmfStateSystemBuilder ssb = StateSystemFactory.newStateSystem(be);
            mmp.assignTargetStateSystem(ssb);
            ssq = ssb;

            Iterator<ISegment> iter = store.iterator();
            LatencyEvent event = null;
            while (iter.hasNext()) {
                ISegment segment = iter.next();
                if (monitor.isCanceled()) {
                    return false;
                }
                event = new LatencyEvent(trace, segment);
                mmp.eventHandle(event);
            }
            mmp.dispose();
            ssq.waitUntilBuilt();

            int quark = ssq.getQuarkAbsolute(LatencyMipMapStateProvider.TEST_ATTRIBUTE_NAME);
            long start = ssq.getStartTime();
            long end = ssq.getCurrentEndTime();
            double avg = TmfStateSystemOperations.queryRangeArithmAverage(ssq, start, end, quark);
            ITmfStateValue minValue = TmfStateSystemOperations.queryRangeMin(ssq, start, end, quark);
            ITmfStateValue maxValue = TmfStateSystemOperations.queryRangeMax(ssq, start, end, quark);
            System.out.println("    MipMap: min=" + minValue.unboxLong() + ", max=" + maxValue.unboxLong() + ", avg=" + avg);
            return true;
        } catch (TimeRangeException | StateValueTypeException | AttributeNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        return false;
    }

    @Override
    protected void canceling() {
    }

    static class LatencyEvent extends TmfEvent {
        private final String fTypeId = "Latency";
        private final @NonNull String fLabel1 = "Segment";
        private final String[] fLabels = new String[] { fLabel1 };
        private final TmfEventType fType = new TmfEventType(fTypeId, TmfEventField.makeRoot(fLabels));
        private final ISegment fSegment;

        LatencyEvent (ITmfTrace trace, ISegment segment) {
            super(trace,
                 ITmfContext.UNKNOWN_RANK,
                 new TmfNanoTimestamp(segment.getStart()),
                 null,
                 null);
            fSegment = segment;
        }

        @Override
        public ITmfEventType getType() {
            return fType;
        }

        @Override
        public ITmfEventField getContent() {
            ITmfEventField field = new TmfEventField(fLabel1, fSegment, null);
            ITmfEventField[] fields = new ITmfEventField[] { field };
            @NonNull String rawContent = field.toString();
            return new TmfEventField(rawContent, null, fields);
        }
    }

    /*
     * TODO remove. It was added to be able to see the state system
     * in the state system explorer
     */
    public static ITmfStateSystem getStateSystem() {
        return ssq;
    }

    public LatencyEntry getTotalStats() {
        return fTotalStats;
    }

    public Map<String, LatencyEntry> getPerSyscallStats() {
        return perSyscallStats;
    }

    public Map<Integer, Map<String, LatencyEntry>> getPerCpuPerSyscal() {
        return perCpuSysCall;
    }

    public Map<Integer, LatencyEntry> getPerCpuStats() {
        return perCpu;
    }
 }
