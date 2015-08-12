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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

public class LatencyStatisticsView extends TmfView {

    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.latency.statsview"; //$NON-NLS-1$

    @Nullable private LatencyStatisticsViewer fStatsViewer = null;

    public LatencyStatisticsView() {
        super("StatsVIew");
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        fStatsViewer = new LatencyStatisticsViewer(NonNullUtils.checkNotNull(parent));
    }

    @Override
    public void setFocus() {
        LatencyStatisticsViewer statsViewer = fStatsViewer;
        if (statsViewer != null) {
            statsViewer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        LatencyStatisticsViewer statsViewer = fStatsViewer;
        if (statsViewer != null) {
            statsViewer.dispose();
        }
    }


}
