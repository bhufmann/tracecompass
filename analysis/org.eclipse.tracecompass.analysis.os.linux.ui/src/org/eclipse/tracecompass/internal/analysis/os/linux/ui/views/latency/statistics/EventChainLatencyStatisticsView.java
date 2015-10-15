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

package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency.statistics;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsView;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.statistics.AbstractSegmentStoreStatisticsViewer;

/**
 * Statistics view for latency chains
 *
 * @author Bernd Hufmann
 */
public class EventChainLatencyStatisticsView extends AbstractSegmentStoreStatisticsView {

    /** The view ID */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.ui.views.latency.chainstatsview"; //$NON-NLS-1$

    @Override
    protected AbstractSegmentStoreStatisticsViewer createSegmentStoreStatisticsViewer(Composite parent) {
        return new EventChainLatencyStatisticsViewer(parent);
    }
}
