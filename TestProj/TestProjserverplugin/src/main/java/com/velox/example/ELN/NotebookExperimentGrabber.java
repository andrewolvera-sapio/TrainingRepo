/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example.ELN;

import com.velox.api.eln.experimententry.ExperimentEntry;
import com.velox.api.eln.experimententry.ExperimentFormEntry;
import com.velox.api.eln.notebookexperiment.NotebookExperiment;
import com.velox.api.plugin.EnbPluginResult;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.NotebookExperimentGrabberPlugin;
import com.velox.api.plugin.invocation.context.NotebookExperimentGrabberContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

public class NotebookExperimentGrabber extends ExemplarVeloxServerPlugin<NotebookExperimentGrabberContext> implements NotebookExperimentGrabberPlugin {

    @Override
    public String getGrabberDisplayName() {
        return "Instrument Tracker";
    }

    @Override
    protected PluginResult run(NotebookExperimentGrabberContext ctx) throws Throwable {
        NotebookExperiment exp = ctx.getNotebookExperiment();
        if (exp == null)
        {
            return new PluginResult(false);
        }

        ExperimentEntry existing = exp.getExperimentEntry("Instrument Tracking Field Set", user);
        if (existing != null) {
            displayError("Instrument Tracking Field Set Already Created");
            return new PluginResult(false);
        }

        ExperimentFormEntry formEntry = exp.addExperimentFormEntry("Instrument Tracking Field Set", 3, "InstrumentTracking", user);
        exp.setExperimentEntry(formEntry, user);
        experimentManager.storeNotebookExperiment(exp, clientCallback.getClientCallbackRMI(), user);
        return new EnbPluginResult(true);
    }

    @Override
    public byte[] getIcon() {
        return getIcon("test-grabber.svg");
    }

}
