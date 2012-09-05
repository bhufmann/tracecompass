/*******************************************************************************
 * Copyright (c) 2011 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mathieu Denis    (mathieu.denis55@gmail.com) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.lttng2.kernel.core.latency.analyzer;

import java.util.Vector;

import org.eclipse.tracecompass.tmf.core.util.Pair;


/**
 * Contains two lists of events name. The first list contains the events identified as starting request and the second list
 * contains the events identified as ending request
 * @since 3.2
 */
public class EventsPair extends Pair<Vector<String>, Vector<String>> {

    public EventsPair(Vector<String> startingEvents, Vector<String> endingEvents) {
        super(startingEvents, endingEvents);
    }
}
