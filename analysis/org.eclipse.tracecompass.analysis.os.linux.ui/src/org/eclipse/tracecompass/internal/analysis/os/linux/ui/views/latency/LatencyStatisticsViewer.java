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
package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.statistics.LatencyEntry;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.statistics.LatencyStatisticsAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

public class LatencyStatisticsViewer extends AbstractTmfTreeViewer {

    @Nullable private LatencyStatisticsAnalysisModule fModule;

    public static final String SYSCALL_LEVEL = "System calls";
    public static final String CPU_LEVEL = "CPU";

    private static final String[] COLUMN_NAMES = new String[] {
            "Level",
            "Min",
            "Max",
            "Average"
    };

    public LatencyStatisticsViewer(Composite parent) {
        super(parent, false);
        setLabelProvider(new CpuLabelProvider());
    }

    /** Provides label for the CPU usage tree viewer cells */
    protected static class CpuLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(@Nullable Object element, int columnIndex) {

            if (element instanceof HiddenTreeViewerEntry) {
                if (columnIndex == 0) {
                    return ((HiddenTreeViewerEntry) element).getName();
                }
            } else {
                LatencyTreeViewerEntry obj = (LatencyTreeViewerEntry) element;
                if (obj != null) {
                    if (columnIndex == 0) {
                        return String.valueOf(obj.getName());
                    } else if (columnIndex == 1) {
                        return String.valueOf(obj.getEntry().getMin());
                    } else if (columnIndex == 2) {
                        return String.valueOf(obj.getEntry().getMax());
                    } else if (columnIndex == 3) {
                        return String.valueOf(obj.getEntry().getAverage());
                    }
                }
            }
            return "";
        }
    }


    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return new ITmfTreeColumnDataProvider() {

            @Override
            public List<TmfTreeColumnData> getColumnData() {
                /* All columns are sortable */
                List<TmfTreeColumnData> columns = new ArrayList<>();
                TmfTreeColumnData column = new TmfTreeColumnData(COLUMN_NAMES[0]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((e1 == null) || (e2 == null)) {
                            return 0;
                        }

                        LatencyTreeViewerEntry n1 = (LatencyTreeViewerEntry) e1;
                        LatencyTreeViewerEntry n2 = (LatencyTreeViewerEntry) e2;

                        return n1.getName().compareTo(n2.getName());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[1]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((e1 == null) || (e2 == null)) {
                            return 0;
                        }

                        LatencyTreeViewerEntry n1 = (LatencyTreeViewerEntry) e1;
                        LatencyTreeViewerEntry n2 = (LatencyTreeViewerEntry) e2;

                        return Long.compare(n1.getEntry().getMin(), n2.getEntry().getMin());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[2]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((e1 == null) || (e2 == null)) {
                            return 0;
                        }

                        LatencyTreeViewerEntry n1 = (LatencyTreeViewerEntry) e1;
                        LatencyTreeViewerEntry n2 = (LatencyTreeViewerEntry) e2;

                        return Long.compare(n1.getEntry().getMax(), n2.getEntry().getMax());

                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[3]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(@Nullable Viewer viewer, @Nullable Object e1, @Nullable Object e2) {
                        if ((e1 == null) || (e2 == null)) {
                            return 0;
                        }

                        LatencyTreeViewerEntry n1 = (LatencyTreeViewerEntry) e1;
                        LatencyTreeViewerEntry n2 = (LatencyTreeViewerEntry) e2;

                        return Double.compare(n1.getEntry().getAverage(), n2.getEntry().getAverage());

                    }
                });
                columns.add(column);

                return columns;
            }

        };
    }


    @Override
    public void initializeDataSource() {
        /* Should not be called while trace is still null */
        ITmfTrace trace = checkNotNull(getTrace());

        LatencyStatisticsAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, LatencyStatisticsAnalysisModule.class, LatencyStatisticsAnalysisModule.ID);
        if (module == null) {
            return;
        }
        module.schedule();
        fModule = module;
    }

    @Override
    @Nullable protected ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }
        if (getTrace() == null || fModule == null) {
            return null;
        }

        LatencyStatisticsAnalysisModule module = fModule;
        if (module == null) {
            return null;
        }

        module.waitForCompletion();

        LatencyEntry entry = module.getTotalStats();

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();

        TmfTreeViewerEntry child = new LatencyTreeViewerEntry("Total", entry);
        entryList.add(child);

        HiddenTreeViewerEntry syscalls = new HiddenTreeViewerEntry(SYSCALL_LEVEL);
        child.addChild(syscalls);

        Map<String, LatencyEntry> perSyscalStats = module.getPerSyscallStats();

        Iterator<Entry<String, LatencyEntry>> stats = perSyscalStats.entrySet().iterator();
        while (stats.hasNext()) {
            Entry<String, LatencyEntry> statsEntry = stats.next();
            syscalls.addChild(new LatencyTreeViewerEntry(statsEntry.getKey(), statsEntry.getValue()));
        }

        HiddenTreeViewerEntry cpus = new HiddenTreeViewerEntry(CPU_LEVEL);
        child.addChild(cpus);

        Map<Integer, LatencyEntry> perCpuStats = module.getPerCpuStats();
        Iterator<Entry<Integer, LatencyEntry>> iter2 = perCpuStats.entrySet().iterator();

        Map<Integer, Map<String, LatencyEntry>> perCpuPerSyscall = module.getPerCpuPerSyscal();

        while (iter2.hasNext()) {
            Entry<Integer, LatencyEntry> statsEntry = iter2.next();
            LatencyTreeViewerEntry cpuEntry = new LatencyTreeViewerEntry(String.valueOf(statsEntry.getKey()), statsEntry.getValue());
            cpus.addChild(cpuEntry);
            Map<String, LatencyEntry> perCpuSyscall = perCpuPerSyscall.get(statsEntry.getKey());
            if (perCpuSyscall != null) {
                stats = perCpuSyscall.entrySet().iterator();
                while (stats.hasNext()) {
                    Entry<String, LatencyEntry> statsEntry2 = stats.next();
                    cpuEntry.addChild(new LatencyTreeViewerEntry(statsEntry2.getKey(), statsEntry2.getValue()));
                }
            }
        }

        return root;
    }


    class LatencyTreeViewerEntry extends TmfTreeViewerEntry  {
        LatencyEntry fEntry;

        LatencyTreeViewerEntry(String name, LatencyEntry entry) {
            super(name);
            fEntry = entry;
        }

        LatencyEntry getEntry() {
            return fEntry;
        }

    }

    class HiddenTreeViewerEntry extends LatencyTreeViewerEntry {
        public HiddenTreeViewerEntry(String name) {
            super(name, new LatencyEntry());
        }
    }

}
