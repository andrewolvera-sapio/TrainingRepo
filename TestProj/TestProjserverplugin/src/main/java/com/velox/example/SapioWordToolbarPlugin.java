/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.FormToolbarPlugin;
import com.velox.api.plugin.invocation.context.FormToolbarContext;
import com.velox.api.plugin.invocation.context.OnFormToolbarContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.util.List;

public class SapioWordToolbarPlugin extends ExemplarVeloxServerPlugin<FormToolbarContext> implements FormToolbarPlugin {

    @Override
    public String getLine1Text() {
        return "Sapio";
    }
    @Override
    public String getLine2Text() {
        return "Sap";
    }

    @Override
    protected PluginResult run(FormToolbarContext ctx) throws Throwable {
        List<String> sapioWordList = dataMgmtServer.getPickListManager(user).getPickListConfig("Sapio Words").getEntryList();
        List selection = clientCallback.showListDialog("Sapio Word Select", sapioWordList, false, user);
        if (selection == null) {
            return new PluginResult(true);
        }
        ctx.getDataRecord().setDataField("SapioWord", selection.toArray()[0], user);
        dataRecordManager.storeAndCommit("Updated Sapio Word", clientCallback.getClientCallbackRMI(), user);
        return new PluginResult(true);
    }

    @Override
    public byte[] getIcon() {
        try {
            return getIcon("Your Path from src/main/resources for the image icon.");
        } catch (Exception e){

        }
        return new byte[0];
    }

    @Override
    public boolean onFormToolbar(OnFormToolbarContext ctx) throws Throwable {
        return ctx.getDataTypeName().equals("Sapio") &&
                ctx.getDataRecord().getDataField("EnableWordButton", user).equals(true);

    }
    @Override
    public String getDescription() {
        return "wasd";
    }
    }
