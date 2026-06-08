///*
// * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
// * ====================================================================
// * This software is the property of Sapio Sciences.
// * ====================================================================
// */
//package com.velox.example.workflows;
//
//import com.velox.api.plugin.PluginResult;
//import com.velox.api.plugin.invocation.TaskEntryPlugin;
//import com.velox.api.plugin.invocation.context.TaskEntryContext;
//import com.velox.api.workflow.ActiveTask;
//import com.velox.sapio.commons.exemplar.plugin.veloxplugin.DefaultExperimentEntryInitPlugin;
//import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;
//
//public class ExperimentGroupSelectionPlugin extends ExemplarVeloxServerPlugin<TaskEntryContext> implements TaskEntryPlugin {
//    @Override
//    protected boolean shouldRun(TaskEntryContext ctx) throws Throwable {
//        return ctx.getActiveTask().getTaskName().equals("Select Experiment Group");
//    }
//
//    @Override
//    protected PluginResult run(TaskEntryContext ctx) throws Throwable {
//        ActiveTask task = ctx.getActiveTask();
//        if () {
//            displayError("Task already completed!");
//            return new PluginResult(true);
//
//        }
//        return new PluginResult(true);
//    }
//}
