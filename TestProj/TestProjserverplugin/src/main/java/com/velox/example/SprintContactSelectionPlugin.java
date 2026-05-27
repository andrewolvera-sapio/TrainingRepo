/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.SelectionListPlugin;
import com.velox.api.plugin.invocation.context.SelectionListContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.util.ArrayList;
import java.util.List;

public class SprintContactSelectionPlugin extends ExemplarVeloxServerPlugin<SelectionListContext> implements SelectionListPlugin {
    @Override
    protected boolean shouldRun(SelectionListContext ctx) throws Throwable {
        return ctx.getDataTypeName().equals("Sprint") && ctx.getDataFieldName().equals("Contact");
    }

    @Override
    protected PluginResult run(SelectionListContext ctx) throws Throwable {
        if (!(ctx.getDataTypeName().equals("Sprint") &&
                ctx.getDataFieldName().equals("Contact"))) {
            return new PluginResult(false);
        }
        List<DataRecord> currSprint = new ArrayList<>();
        currSprint.add(ctx.getDataRecord());

        List<List<DataRecord>> parentAccount = dataRecordManager.getParentsOfType(currSprint, "Account", user);
        List<List<DataRecord>> contactRecordsInTheSystem =
                dataRecordManager.getChildrenOfType(parentAccount.get(0), "Contact", user);
        List<Object> contacts = new ArrayList<>();
        for (DataRecord contactRecord : contactRecordsInTheSystem.get(0)) {
            contacts.add(contactRecord.getDataField("Name", user));
        }
        return new PluginResult(true, contacts);
    }
}
