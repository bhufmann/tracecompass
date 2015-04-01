/*******************************************************************************
 * Copyright (c) 2010, 2011 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Philippe Sawicki (INF4990.A2010@gmail.com)   - Initial API and implementation
 *   Mathieu Denis    (mathieu.denis55@gmail.com) - Refactored code
 *   Bernd Hufmann - Adapted to new model-view-controller design
 *******************************************************************************/
package org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.datadefinition.ColorMap;
import org.eclipse.nebula.visualization.widgets.datadefinition.ColorMap.PredefinedColorMap;
import org.eclipse.nebula.visualization.widgets.figures.IntensityGraphFigure;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency.model.GraphScaledData;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency.model.IGraphDataModel;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency.model.IGraphModelListener;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.views.latency.model.LatencyGraphModel;

/**
 * <b><u>GraphViewer</u></b>
 * <p>
 * Graph viewer.
 *
 * @author Philippe Sawicki
 */
public class GraphViewer extends Composite implements IGraphModelListener {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Latency graph model
     */
    private LatencyGraphModel fModel;

    private LightweightSystem lightweightSystem;
    private IntensityGraphFigure intensityGraphFigure;
    private Label heatmapTitel;
    private Composite header;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

	/**
	 * Constructor.
	 * @param parent The parent composite node.
	 * @param style The SWT style to use to render the view.
	 */
	public GraphViewer(Composite parent, int style) {
		super(parent, style);

		initialize();

		fModel = new LatencyGraphModel();
        fModel.addGraphModelListener(this);
	}

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.linuxtools.lttng.ui.views.latency.AbstractViewer#dispose()
	 */
	@Override
	public void dispose() {
	    fModel.removeGraphModelListener(this);
	    super.dispose();
	}

	public void clear() {
	}

	public void clearBackground() {
	}

    public IGraphDataModel getModel() {
        return fModel;
    }

    @Override
    public void graphModelUpdated() {
        if (!isDisposed() && getDisplay() != null) {
            getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (!isDisposed()) {
                        updateGrpah();
//                        redraw();
                    }
                }
            });
        }
    }

    @Override
    public void currentEventUpdated(long currentEventTime) {
        graphModelUpdated();
    }

    private void initialize() {
        GridData layoutData;
        Display display = Display.getCurrent();
        setLayout(new FillLayout());
        Composite composite = new Composite(this, SWT.FILL);
        composite.setLayout(new GridLayout(1, true));
        /*
         * Header
         */
        layoutData = new GridData(GridData.FILL_HORIZONTAL);
        header = new Composite(composite, SWT.NONE);
        header.setLayout(new GridLayout(1, true));
        header.setLayoutData(layoutData);
        //
        layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.grabExcessHorizontalSpace = true;
        heatmapTitel = new Label(header, SWT.CENTER);
        heatmapTitel.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
        heatmapTitel.setLayoutData(layoutData);
        Font font = new Font(display, "Arial", 12, SWT.BOLD);
        heatmapTitel.setFont(font);
        heatmapTitel.setText("");
        font.dispose();
        /*
         * Heatmap
         */
        layoutData = new GridData(GridData.FILL_BOTH);
        Canvas canvas = new Canvas(composite, SWT.FILL | SWT.BORDER);
        canvas.setLayoutData(layoutData);
        canvas.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        //
        lightweightSystem = new LightweightSystem(canvas);
        lightweightSystem.getRootFigure().setBackgroundColor(display.getSystemColor(SWT.COLOR_WHITE));
        //
        intensityGraphFigure = new IntensityGraphFigure();
        intensityGraphFigure.setForegroundColor(display.getSystemColor(SWT.COLOR_BLACK));
        intensityGraphFigure.getXAxis().setTitle("Time");
        intensityGraphFigure.getYAxis().setTitle("Latency");
    }

    public void updateGrpah() {
        double height = lightweightSystem.getRootFigure().getBounds().height;
                double width = lightweightSystem.getRootFigure().getBounds().width;
//      double width = intensityGraphFigure.getClientArea().getSize().width;
      // height of the plot area
//      double height = intensityGraphFigure.getClientArea().getSize().height;

      if (height <= 0.0 || width <= 0.0) {
          return;
      }
      GraphScaledData fScaledData = fModel.scaleTo((int)width, (int)height, 2);

      long fXMin = fScaledData.getHorFirstBucketTime() > 0 ? fScaledData.getHorFirstBucketTime() : 0;
      long fXMax = 0;
      if (fScaledData.getHorLastBucket() > 0) {
          fXMax = fScaledData.getHorBucketEndTime(fScaledData.getHorNbBuckets() - 1);
      }

      long fYMin = fScaledData.getVerFirstBucketTime() > 0 ? fScaledData.getVerFirstBucketTime() : 0;
      long fYMax = 0;
      if (fScaledData.getVerLastBucket() > 0) {
          fYMax = fScaledData.getVerBucketEndTime(fScaledData.getVerNbBuckets() - 1);
      }

      int xLen = fScaledData.getHorNbBuckets();
      int yLen = fScaledData.getVerNbBuckets();

//      int barWidth = fScaledData.getBarWidth();

      int maxIntensity = 0;

      int[] heatMapData = new int[xLen * yLen * 2];

      for (int i = 0; i < xLen; i++) {
          for (int j = 0; j < yLen; j++) {
              int eventCount = fScaledData.getEventCount(i, j);
              if (eventCount > maxIntensity) {
                  maxIntensity = eventCount;
              }

              heatMapData[j * xLen + i] = eventCount;
          }
      }

        /*
         * Set the range and min/max values.
         */
//        intensityGraphFigure.getXAxis().setRange(new Range(0, xLen));
//        intensityGraphFigure.getYAxis().setRange(new Range(0, yLen));

        intensityGraphFigure.getXAxis().setRange(new Range(fXMin, fXMax));
        intensityGraphFigure.getYAxis().setRange(new Range(fYMin, fYMax));

        //
        intensityGraphFigure.setMin(0);
//        intensityGraphFigure.setMax(maxIntensity / (dataHeight / 5.0d)); // Important when zooming in.
        intensityGraphFigure.setMax(maxIntensity);

        intensityGraphFigure.setDataHeight(yLen);
        intensityGraphFigure.setDataWidth(xLen);
        //
        intensityGraphFigure.setColorMap(new ColorMap(PredefinedColorMap.JET, true, true));
        /*
         * Set the heatmap data
         */
        lightweightSystem.setContents(intensityGraphFigure);
        intensityGraphFigure.setDataArray(heatMapData);
    }

}