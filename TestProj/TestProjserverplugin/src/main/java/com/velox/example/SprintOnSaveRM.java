/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.OnSavePlugin;
import com.velox.api.plugin.invocation.context.OnSaveContext;
import com.velox.api.util.ServerException;
import com.velox.example.models.AssignmentModel;
import com.velox.example.models.DeveloperModel;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;
import com.velox.sapio.commons.exemplar.recordmodel.record.RecordModel;
import com.velox.sapio.commons.exemplar.recordmodel.relationship.Children;
import javassist.tools.rmi.RemoteException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SprintOnSaveRM extends ExemplarVeloxServerPlugin<OnSaveContext> implements OnSavePlugin {
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
        if (DataRecordMap.containsKey("Contact")) {contactRecord = DataRecordMap.get("Contact").iterator().next();}
        if (DataRecordMap.containsKey("Developer")) {developerRecord = DataRecordMap.get("Developer").iterator().next();}
        if (DataRecordMap.containsKey("Assignment")) {assignmentRecord = DataRecordMap.get("Assignment").iterator().next();}

        if (contactRecord != null) {contactValidation(contactRecord);}
        if (developerRecord != null) {developerValidation(developerRecord);}
        if (assignmentRecord != null) {
            PluginResult assignmentResult = assignmentValidation(assignmentRecord);
            if (assignmentResult != null) {
                return assignmentResult;
            }
        }

        return finalize(emailErr, nameErr, contactRecord);
    }



    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return Pattern.compile(EMAIL_REGEX)
                .matcher(email)
                .matches();
    }

    private void contactValidation(DataRecord contactRecord) throws java.rmi.RemoteException, NotFound {
        if (!(contactRecord.getLastSavedStringVal("Email").equals(contactRecord.getStringVal("Email", user)))) {
            if (!isValidEmail(contactRecord.getStringVal("Email", user))) {
                emailErr = true;
            }
        }

        if (!(contactRecord.getLastSavedStringVal("Name").equals(contactRecord.getStringVal("Name", user)))) {
            if (contactRecord.getStringVal("Name", user).contains(",")) {
                nameErr = true;
            }
        }
    }

    private void developerValidation(DataRecord developerRecord) throws java.rmi.RemoteException, NotFound {
        if (!(developerRecord.getLastSavedStringVal("Name").equals(developerRecord.getStringVal("Name", user)))) {
            if (developerRecord.getStringVal("Name", user).contains(",")) {
                nameErr = true;
            }
        }
    }

    private PluginResult assignmentValidation(DataRecord assignmentRecord) throws Throwable {
        if (assignmentRecord.getLastSavedStringVal("Developer").equals(assignmentRecord.getStringVal("Developer", user))) {
            return null;
        }

        AssignmentModel assignment = instMan.addExistingRecordOfType(assignmentRecord, AssignmentModel.class);
        String currentDeveloper = assignment.getDeveloper();
        boolean isEmptyInput = currentDeveloper == null || currentDeveloper.isEmpty();

        DataRecord sprintDr = assignmentRecord.getParentList(user).get(0);
        RecordModel sprint = instMan.addExistingRecord(sprintDr);

        Map<String, DeveloperModel> developersByName = loadDevelopersByName();
        DeveloperModel currDev = developersByName.get(currentDeveloper);

        List<DataRecord> allAssignmentRecords = sprintDr.getChildListOfType("Assignment", user);
        Map<String, Integer> assignmentDevNameMap = buildDeveloperAssignmentCounts(assignmentRecord, allAssignmentRecords, isEmptyInput);

        relationshipMan.loadChildren(sprint, DeveloperModel.DATA_TYPE_NAME);
        Collection<DeveloperModel> allSprintDevs = sprint.get(Children.ofType(DeveloperModel.class));

        if (currDev == null && !isEmptyInput) {
            return new PluginResult(false);
        }

        if (isEmptyInput) {
            return handleDeveloperCleared(assignmentRecord, sprint, developersByName, assignmentDevNameMap);
        }
        return handleDeveloperAssigned(assignmentRecord, sprint, currDev, developersByName, allSprintDevs, assignmentDevNameMap);
    }

    private Map<String, DeveloperModel> loadDevelopersByName() throws Throwable {
        List<DataRecord> devRecords = dataRecordManager.getAllRecordsOfType("Developer", user);
        List<DeveloperModel> developers = instMan.addExistingRecordsOfType(devRecords, DeveloperModel.class);
        Map<String, DeveloperModel> developersByName = new HashMap<>();
        for (DeveloperModel developer : developers) {
            developersByName.put(developer.getName(), developer);
        }
        return developersByName;
    }

    private Map<String, Integer> buildDeveloperAssignmentCounts(DataRecord assignmentRecord, List<DataRecord> allAssignmentRecords, boolean isEmptyInput) throws java.rmi.RemoteException, NotFound {
        Map<String, Integer> assignmentDevNameMap = new HashMap<>();
        if (isEmptyInput) {
            assignmentDevNameMap.put(assignmentRecord.getLastSavedStringVal("Developer"), 1);
        } else {
            assignmentDevNameMap.put((String) assignmentRecord.getDataField("Developer", user), 1);
        }
        for (DataRecord record : allAssignmentRecords) {
            String name = (String) record.getLastSavedStringVal("Developer");
            if (assignmentDevNameMap.get(name) == null) {
                assignmentDevNameMap.put(name, 1);
            } else {
                assignmentDevNameMap.put(name, assignmentDevNameMap.get(name) + 1);
            }
        }
        return assignmentDevNameMap;
    }

    private PluginResult handleDeveloperCleared(DataRecord assignmentRecord, RecordModel sprint, Map<String, DeveloperModel> developersByName, Map<String, Integer> assignmentDevNameMap) throws Throwable {
        String previousDeveloper = assignmentRecord.getLastSavedStringVal("Developer");
        if (assignmentDevNameMap.get(previousDeveloper) < 3) {
            DeveloperModel devToRemove = developersByName.get(previousDeveloper);
            if (devToRemove != null) {
                sprint.remove(Children.refs(List.of(devToRemove)));
                recMan.storeChanges();
            }
            return new PluginResult(true);
        }
        return null;
    }

    private PluginResult handleDeveloperAssigned(DataRecord assignmentRecord, RecordModel sprint, DeveloperModel currDev, Map<String, DeveloperModel> developersByName, Collection<DeveloperModel> allSprintDevs, Map<String, Integer> assignmentDevNameMap) throws Throwable {
        Integer total = assignmentDevNameMap.get(currDev.getName());
        if (total < 2) {
            if (addDevToSprint(allSprintDevs, currDev)) {
                sprint.add(Children.refs(List.of(currDev)));
                String previousDeveloper = assignmentRecord.getLastSavedStringVal("Developer");
                if (removeOldDev(previousDeveloper, allSprintDevs, developersByName)) {
                    DeveloperModel oldDev = developersByName.get(previousDeveloper);
                    if (oldDev != null) {
                        sprint.remove(Children.refs(List.of(oldDev)));
                    }
                }
                recMan.storeChanges();
            }
            return new PluginResult(true);
        }
        return null;
    }

    private boolean removeOldDev(String developer, Collection<DeveloperModel> allSprintDevs, Map<String, DeveloperModel> developersByName) {
        DeveloperModel dev = developersByName.get(developer);
        if (dev == null) {
            return false;
        }
        for (DeveloperModel sprintDev : allSprintDevs) {
            if (sprintDev.equals(dev)) {
                return true;
            }
        }
        return false;
    }

    private boolean addDevToSprint(Collection<DeveloperModel> sprintDevs, DeveloperModel currDev) {
        for (DeveloperModel sprintDev : sprintDevs) {
            if (currDev.equals(sprintDev)) {
                return false;
            }
        }
        return true;
    }

    private PluginResult finalize(boolean emailErr, boolean nameErr, DataRecord contactRecord) throws ServerException, java.rmi.RemoteException, NotFound {
        // Build and display errors
        if (emailErr || nameErr) {
            StringBuilder sb = new StringBuilder();
            if (emailErr) {
                sb.append(contactRecord.getDataField("Email", user)).append(" is not a valid email!\n");
            }
            if (nameErr) {
                sb.append("Name entry cannot contain commas!");
            }
            displayError(sb.toString());
            return new PluginResult(false);
        }
        return new PluginResult(true);
    }
}
