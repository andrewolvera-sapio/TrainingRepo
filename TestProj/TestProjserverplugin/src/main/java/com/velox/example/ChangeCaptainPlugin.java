/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.IoError;
import com.velox.api.datarecord.NotFound;
import com.velox.api.exception.recoverability.serverexception.UnrecoverableServerException;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.ActionDataFieldPlugin;
import com.velox.api.plugin.invocation.context.ActionDataFieldContext;
import com.velox.api.util.ServerException;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChangeCaptainPlugin extends ExemplarVeloxServerPlugin<ActionDataFieldContext> implements ActionDataFieldPlugin {
    @Override
    protected boolean shouldRun(ActionDataFieldContext ctx) throws Throwable {
        return ctx.getDataTypeName().equals("Sprint") &&
                ctx.getDataFieldName().equals("ChangeCaptain");
    }

    @Override
    protected PluginResult run(ActionDataFieldContext ctx) throws Throwable {
        List<DataRecord> developers = dataRecordManager.getAllRecordsOfType("Developer", user);
        HashMap<String, DataRecord> devMap = new HashMap<>();

        for (DataRecord dev : developers) {
            devMap.put((String)dev.getDataField("Name", user), dev);
        }

        List<String> keyList = new ArrayList<>(devMap.keySet());

        List<String> selectionList = new ArrayList<>();
        for (String key : keyList) {
            if (isActiveLeader(devMap.get(key))) {
                selectionList.add(key);
            }
        }

        List selection = clientCallback.showListDialog("Choose Developer", selectionList, false, user);

        //No selection
        if (selection == null) {return new PluginResult(true);}

        DataRecord currSprint = ctx.getDataRecord();
        wasPreviousCaptain(currSprint);
        if (notAlreadyChild(currSprint, (String) selection.toArray()[0])) {
            ctx.getDataRecord().addChild(devMap.get(selection.toArray()[0]), user);
        }

        ctx.getDataRecord().setDataField("Captain", selection.toArray()[0], user);
        dataRecordManager.storeAndCommit("Updated Sprint Captain", clientCallback.getClientCallbackRMI(), user);
        return new PluginResult(true);
    }

    public boolean isActiveLeader(DataRecord developer) throws RemoteException, NotFound {
        if (developer.getDataField("Active", user).equals(false) || !developer.getDataField("Role", user).equals("Leader")) { return false;}

        return true;
    }

    public void wasPreviousCaptain (DataRecord currSprint) throws RemoteException, NotFound, IoError, ServerException {
        List<DataRecord> developers = currSprint.getChildListOfType("Developer", user);
        String oldCaptain = (String) currSprint.getDataField("Captain", user);

        for (DataRecord developer : developers) {
            if (oldCaptain.equals(developer.getDataField("Name", user))) {
                if (notChildOfAssignments(currSprint, developer)) {
                    currSprint.removeChild(developer, clientCallback.getClientCallbackRMI(), user);
                }
                return;
            }
        }
    }

    private boolean notChildOfAssignments(DataRecord currSprint, DataRecord developer) throws IoError, RemoteException, UnrecoverableServerException, NotFound {
        List<DataRecord> assignments = currSprint.getChildListOfType("Assignment", user);
        List<String> assignmentDevs = new ArrayList<>();

        for (DataRecord assignment : assignments) {
            if (developer.getDataField("Name", user).equals(assignment.getDataField("Developer", user))) {
                return false;
            }
        }
        return true;

    }

    public boolean notAlreadyChild(DataRecord currSprint, String selection) throws IoError, RemoteException, UnrecoverableServerException, NotFound {
        List<DataRecord> developers = currSprint.getChildListOfType("Developer", user);

        for (DataRecord developer : developers) {
            if (selection.equals(developer.getDataField("Name", user))) {
                return false;
            }
        }
        return true;
    }
}
