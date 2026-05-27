/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.OnSavePlugin;
import com.velox.api.plugin.invocation.context.OnSaveContext;
import com.velox.api.util.ServerException;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;
import javassist.tools.rmi.RemoteException;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class SprintOnSavePlugin extends ExemplarVeloxServerPlugin<OnSaveContext> implements OnSavePlugin {
    Multimap<String, DataRecord> DataRecordMap = ArrayListMultimap.create();
    String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    boolean emailErr = false;
    boolean nameErr = false;

    @Override
    public boolean shouldRun(OnSaveContext ctx) throws ServerException, RemoteException {
        List<DataRecord> DataRecordList = ctx.getDataRecordList();
        DataRecordList.forEach(dataRec -> DataRecordMap.put(dataRec.getDataTypeName(),
                dataRec));
        return DataRecordMap.containsKey("Contact") || DataRecordMap.containsKey("Developer") || DataRecordMap.containsKey("Assignment");
    }
    @Override
    protected PluginResult run(OnSaveContext ctx) throws Throwable {
        DataRecord contactRecord = null;
        DataRecord developerRecord = null;
        DataRecord assignmentRecord = null;
        if (DataRecordMap.containsKey("Contact")) {
            contactRecord = DataRecordMap.get("Contact").iterator().next();
        }
        if (DataRecordMap.containsKey("Developer")) {
            developerRecord = DataRecordMap.get("Developer").iterator().next();
        }
        if (DataRecordMap.containsKey("Assignment")) {
            assignmentRecord = DataRecordMap.get("Assignment").iterator().next();
        }
        // Checking contact email validity
        if (contactRecord != null) {
            if (!(contactRecord.getLastSavedStringVal("Email").equals(contactRecord.getStringVal("Email", user)))) {
                if (!isValidEmail(contactRecord.getStringVal("Email", user))) {
                    emailErr = true;
                }
            }
        }

        //Checking contact and/or developer name validity
        if (contactRecord != null) {
            if (!(contactRecord.getLastSavedStringVal("Name").equals(contactRecord.getStringVal("Name", user)))) {
                if (contactRecord.getStringVal("Name", user).contains(",")) {
                    nameErr = true;
                }
            }
        }
        if (developerRecord != null) {
            if (!(developerRecord.getLastSavedStringVal("Name").equals(developerRecord.getStringVal("Name", user)))) {
                if (developerRecord.getStringVal("Name", user).contains(",")) {
                    nameErr = true;
                }
            }
        }

        // Assignment save logic
        if (assignmentRecord != null) {
            if (!(assignmentRecord.getLastSavedStringVal("Developer").equals(assignmentRecord.getStringVal("Developer", user)))) {
                boolean isEmptyInput = assignmentRecord.getStringVal("Developer", user).isEmpty();
                List<DataRecord> parentList = assignmentRecord.getParentList(user);
                DataRecord sprintRecord = parentList.get(0);

                DataRecord currDevRecord = getDev(assignmentRecord.getStringVal("Developer", user));

                List<DataRecord> allAssignmentRecords = sprintRecord.getChildListOfType("Assignment", user);
                HashMap<String, Integer> assignmentDevNameMap = new HashMap<>();

                List<DataRecord> allSprintDevs = sprintRecord.getChildListOfType("Developer", user);

                if (isEmptyInput) {
                    assignmentDevNameMap.put(assignmentRecord.getLastSavedStringVal("Developer"), 1);} else {
                    assignmentDevNameMap.put((String) assignmentRecord.getDataField("Developer", user), 1);}

                for (DataRecord record : allAssignmentRecords) {
                    String name = (String) record.getLastSavedStringVal("Developer");
                    if (assignmentDevNameMap.get(name) == null) {
                        assignmentDevNameMap.put(name, 1);
                    } else {
                        Integer curr = assignmentDevNameMap.get(name);
                        assignmentDevNameMap.put(name, ++curr);
                    }
                }



                // Remove dev no new
                if (currDevRecord == null && !isEmptyInput) {return new PluginResult(false);}

                if (isEmptyInput) {


                    if (assignmentDevNameMap.get(assignmentRecord.getLastSavedStringVal("Developer")) < 3) {
                        sprintRecord.removeChild(getDev(assignmentRecord.getLastSavedStringVal("Developer")), clientCallback.getClientCallbackRMI(), user);
                        return new PluginResult(true);
                    }

                }
                // Add dev
                else {
                    Integer total = assignmentDevNameMap.get(currDevRecord.getDataField("Name", user));
                    if (total < 2) {
                        if (addDevToSprint(allSprintDevs, currDevRecord)) {
                            sprintRecord.addChild(currDevRecord, user);
                            if (removeOldDev(assignmentRecord.getLastSavedStringVal("Developer"), allSprintDevs)) {
                                sprintRecord.removeChild(getDev(assignmentRecord.getLastSavedStringVal("Developer")), clientCallback.getClientCallbackRMI(), user);
                            }
                        }
                        return new PluginResult(true);
                    }
                }
            }
        }

        // Build and display errors
        if (emailErr || nameErr) {
            StringBuilder sb = new StringBuilder();
            if (emailErr) {
                sb.append(contactRecord.getStringVal("Email", user)).append(" is not a valid email!\n");
            }
            if (nameErr) {
                sb.append("Name entry cannot contain commas!");
            }
            displayError(sb.toString());
            return new PluginResult(false);
        }

        return new PluginResult(true);
    }

    private boolean removeOldDev(String developer, List<DataRecord> allSprintDevs) throws ServerException, java.rmi.RemoteException, NotFound {
        DataRecord currDev = getDev(developer);
        if (currDev == null) return false;
        for (DataRecord sprintDev : allSprintDevs) {
            if (sprintDev == currDev) {
                return true;
            }
        }
        return false;
    }

    private DataRecord getDev(String developer) throws ServerException, java.rmi.RemoteException, NotFound {
        List<DataRecord> developers = dataRecordManager.getAllRecordsOfType("Developer", user);
        for (DataRecord dev : developers) {
            if (dev.getDataField("Name", user).equals(developer)) {
                return dev;
            }
        }
        return null;
    }

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return Pattern.compile(EMAIL_REGEX)
                .matcher(email)
                .matches();
    }

    public boolean addDevToSprint(List<DataRecord> sprintDevs, DataRecord currDev) {
        for (DataRecord sprintDev : sprintDevs) {
            if (currDev == sprintDev) {
                return false;
            }
        }
        return true;
    }
}
