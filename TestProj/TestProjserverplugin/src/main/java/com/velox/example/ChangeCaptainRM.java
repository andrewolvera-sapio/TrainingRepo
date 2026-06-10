package com.velox.example;

import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.ActionDataFieldPlugin;
import com.velox.api.plugin.invocation.context.ActionDataFieldContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

public class ChangeCaptainRM extends ExemplarVeloxServerPlugin<ActionDataFieldContext> implements ActionDataFieldPlugin {
    @Override
    protected boolean shouldRun(ActionDataFieldContext ctx) throws Throwable {
        return ctx.getDataTypeName().equals("Sprint") &&
                ctx.getDataFieldName().equals("ChangeCaptain");
    }

    @Override
    protected PluginResult run(ActionDataFieldContext ctx) throws Throwable {
        return new PluginResult(true);
    }
}
