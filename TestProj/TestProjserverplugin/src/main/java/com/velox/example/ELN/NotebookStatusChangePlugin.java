/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example.ELN;

import com.velox.api.eln.experimententry.ExperimentEntry;
import com.velox.api.eln.experimententry.ExperimentEntryStatus;
import com.velox.api.eln.notebookexperiment.NotebookExperiment;
import com.velox.api.plugin.EnbPluginResult;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.NotebookExperimentStatusChangePlugin;
import com.velox.api.plugin.invocation.context.NotebookExperimentStatusChangeContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;
import java.util.Date;

public class NotebookStatusChangePlugin extends ExemplarVeloxServerPlugin<NotebookExperimentStatusChangeContext> implements NotebookExperimentStatusChangePlugin {
    private static final Object key = "AUTO COMPLETE FIRST ENTRY";
    @Override
    protected boolean shouldRun(NotebookExperimentStatusChangeContext ctx) throws Throwable {
        NotebookExperiment exp = ctx.getNotebookExperiment();
        return exp.getStatus(user).equals(NotebookExperiment.NotebookExperimentStatus.New) && exp.getNotebookExperimentOptionMap(user).containsKey(key);
    }

    @Override
    protected PluginResult run(NotebookExperimentStatusChangeContext ctx) throws Throwable
    {
        NotebookExperiment exp = ctx.getNotebookExperiment();
        String entryName = exp.getNotebookExperimentOptionMap(user).get(key);
        ExperimentEntry entry = exp.getExperimentEntry(entryName, user);

        entry.setEntryStatus(ExperimentEntryStatus.Completed);
        entry.setSubmittedBy(user.getUsername());
        entry.setSubmittedDate(new Date());
        exp.setExperimentEntry(entry, user);

        return new EnbPluginResult(true, entry);
    }


}
