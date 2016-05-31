/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.ui.project.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfExperimentFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;

/**
 * Operation to add traces to an experiment.
 *
 * @author Bernd Hufmann
 */
public class SelectTracesOperation implements IRunnableWithProgress {

    private final @Nullable TmfExperimentElement fExperimentElement;
    private final @Nullable TmfTraceFolder fParentTraceFolder;
    private final @Nullable List<TmfTraceElement> fTraceElements;
    private final @Nullable List<IResource> fResources;
    private final @Nullable Map<String, TmfTraceElement> fPreviousTraces;
    private @NonNull IStatus fStatus = Status.OK_STATUS;

    /**
     * Constructor
     *
     * @param experimentFolderElement
     *              workspace experiment folder containing the experiment
     * @param experiment
     *              experiment folder where to add traces
     * @param parentTraceFolder
     *              the parent trace folder containing the trace resources
     * @param resources
     *              the trace resources to add to the experiment
     */
    public SelectTracesOperation(@NonNull TmfExperimentFolder experimentFolderElement, @NonNull IFolder experiment, @NonNull TmfTraceFolder parentTraceFolder, @NonNull List<IResource> resources) {
        this(experimentFolderElement.getExperiment(experiment), parentTraceFolder, null, resources, null);
    }

    /**
     * Constructor. It will add traces to given experiment and remove traces
     * that don't exist anymore.
     *
     * @param experimentElement
     *              the experiment element to add the traces
     * @param traces
     *              the trace elements
     * @param previousTraces
     *              map of traces currently available in the experiment
     */
    public SelectTracesOperation(@NonNull TmfExperimentElement experimentElement, @NonNull TmfTraceElement[] traces, @NonNull Map<String, TmfTraceElement> previousTraces) {
        this(experimentElement, null, traces, null, previousTraces);
    }

    // Full constructor for internal use only
    private SelectTracesOperation(TmfExperimentElement experimentElement, TmfTraceFolder parentTraceFolder, TmfTraceElement[] traces, List<IResource> resources, Map<String, TmfTraceElement> previousTraces) {
        fExperimentElement = experimentElement;
        fParentTraceFolder = parentTraceFolder;
        if (traces == null) {
            fTraceElements = null;
        } else {
            fTraceElements = new ArrayList<>();
            fTraceElements.addAll(Arrays.asList(traces));
        }
        fResources = resources;
        fPreviousTraces = previousTraces;
    }

    @Override
    public void run(IProgressMonitor progressMonitor) {
        TmfExperimentElement experimentElement = fExperimentElement;
        if (experimentElement == null) {
            return;
        }

        // Check if operation was cancelled.
        boolean changed = false;

        Map<String, TmfTraceElement> previousTraces = new HashMap<>();
        if (fPreviousTraces != null) {
            previousTraces = fPreviousTraces;
        }

        List<TmfTraceElement> elements = fTraceElements;
        if (elements == null) {
            if ((fParentTraceFolder != null) && (fResources != null)) {
                elements = fParentTraceFolder.getTraceElements(fResources);
            } else {
                return;
            }
        }

        // Add the selected traces to the experiment
        try {
            // FIXME startTime and endTime need to be supplied by dialog
            long startTime = 1461710138250414000L;
            long endTime = 1461763217579870000L;
            List<TmfTraceElement> intersectingElements = getIntersectingTraces(elements, progressMonitor, startTime, endTime);
            Set<String> keys = previousTraces.keySet();
            SubMonitor subMonitor = SubMonitor.convert(progressMonitor, intersectingElements.size() + keys.size());
            for (TmfTraceElement trace : intersectingElements) {
                ModalContext.checkCanceled(progressMonitor);
                String name = trace.getElementPath();
                if (keys.contains(name)) {
                    subMonitor.setTaskName(Messages.SelectTracesWizardPage_TraceRemovalTask + " " + trace.getElementPath()); //$NON-NLS-1$
                    keys.remove(name);
                } else {
                    subMonitor.setTaskName(Messages.SelectTracesWizardPage_TraceSelectionTask + " " + trace.getElementPath()); //$NON-NLS-1$
                    experimentElement.addTrace(trace, false);
                    changed = true;
                }
                subMonitor.worked(1);
            }

            // Remove traces that were unchecked (thus left in fPreviousTraces)
            for (Map.Entry<String, TmfTraceElement> entry : previousTraces.entrySet()) {
                ModalContext.checkCanceled(progressMonitor);
                TmfTraceElement trace = entry.getValue();
                subMonitor.setTaskName(Messages.SelectTracesWizardPage_TraceRemovalTask + " " + trace.getElementPath()); //$NON-NLS-1$

                try {
                    experimentElement.removeTrace(trace);
                } catch (CoreException e) {
                    Activator.getDefault().logError(Messages.SelectTracesWizardPage_SelectionError + " " + experimentElement.getName(), e); //$NON-NLS-1$
                }
                changed = true;
                subMonitor.worked(1);
            }
            if (changed) {
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        experimentElement.closeEditors();
                    }
                });
                experimentElement.deleteSupplementaryResources();
            }
            setStatus(Status.OK_STATUS);
        } catch (InterruptedException e) {
            setStatus(Status.CANCEL_STATUS);
        } catch (Exception e) {
            Activator.getDefault().logError(Messages.SelectTracesWizardPage_SelectionError, e);
            setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.SelectTracesWizardPage_SelectionError, e));
        }
    }

    private static List<TmfTraceElement> getIntersectingTraces(List<TmfTraceElement> elements, IProgressMonitor progressMonitor, long startTime, long endTime) throws InterruptedException {
        List<TmfTraceElement> intersectingElements = new ArrayList<>();
        SubMonitor subMonitor = SubMonitor.convert(progressMonitor, elements.size());
        for (TmfTraceElement tmfTraceElement : elements) {
            ModalContext.checkCanceled(progressMonitor);
            ITmfTrace trace = tmfTraceElement.instantiateTrace();
            ITmfEvent event = tmfTraceElement.instantiateEvent();
            try {
                trace.initTrace(tmfTraceElement.getResource(), tmfTraceElement.getResource().getLocation().toOSString(), event.getClass());
                subMonitor.setTaskName("Indexing trace " + trace.getName()); //$NON-NLS-1$
                TmfTraceManager.refreshSupplementaryFiles(trace);
                // read first event for early checks
                final ITmfContext context = trace.seekEvent(0);
                trace.getNext(context);
                context.dispose();
                if (trace.getStartTime().toNanos() <= endTime) {
                    // index trace to get endTime
                    trace.indexTrace(true);
                    if (trace.getEndTime().toNanos() >= startTime) {
                        intersectingElements.add(tmfTraceElement);
                    }
                }
                subMonitor.worked(1);
            } catch (TmfTraceException e) {
                //                TraceUtils.displayErrorMsg(Messages.SynchronizeTracesHandler_Title, Messages.SynchronizeTracesHandler_InitError + "\n\n" + e);
                continue;
            } finally {
                trace.dispose();
            }
        }
        return intersectingElements;
    }

    /**
     * Set the status for this operation
     *
     * @param status
     *            the status
     */
    protected void setStatus(@NonNull IStatus status) {
        fStatus = status;
    }

    /**
     * Returns the status of the operation execution.
     *
     * @return status
     */
    public @NonNull IStatus getStatus() {
        return fStatus;
    }

}
