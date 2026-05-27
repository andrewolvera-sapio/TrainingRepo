/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.plugin.PluginDirective;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.directive.DataRecordTableDirective;
import com.velox.api.plugin.invocation.ActionMenuPlugin;
import com.velox.api.plugin.invocation.context.ActionMenuContext;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.util.*;

public class AddRolesToolbarServerSidePlugin extends ExemplarVeloxServerPlugin<ActionMenuContext> implements ActionMenuPlugin{
    @Override
    public List<String> getSectionNamePath() {
        return Arrays.asList("Add","Roles");
    }

    @Override
    public String getLine1Text() {
        return "Add";
    }

    @Override
    public String getLine2Text() {
        return "Roles";
    }

    @Override
    protected PluginResult run(ActionMenuContext actionMenuContext) throws Throwable {
        try {
            int numRoles = Integer.parseInt(clientCallback.showInputDialog("Create how many roles?: "));
            List<DataRecord> roleRecords = dataRecordManager.addDataRecords("DeveloperRole", numRoles, user);
            PluginDirective directive = new DataRecordTableDirective(roleRecords);
            return new PluginResult(true, directive);
        } catch (Exception e){
            return new PluginResult(false);
        }
    }

    @Override
    public byte[] getIcon() {
        try {
            return getIcon("Your Path from src/main/resources for the image icon.");
        } catch (Exception e){

        }
        return new byte[0];
    }


}
