/*******************************************************************************
* Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.handler;

import org.eclipse.tracecompass.internal.tmf.ui.views.ITmfZoomToSelectionProvider;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * Command handler to zoom to the current selection.
 *
 * @author Matthew Khouzam
 * @author Bernd Hufmann
 */
public class ZoomToSelectionHandler extends TmfViewBaseHandler {

    @Override
    public void execute(TmfView view) {
        ITmfZoomToSelectionProvider provider = view.getAdapter(ITmfZoomToSelectionProvider.class);
        if (provider != null) {
            provider.zoomToSelection();
        }
    }
}
