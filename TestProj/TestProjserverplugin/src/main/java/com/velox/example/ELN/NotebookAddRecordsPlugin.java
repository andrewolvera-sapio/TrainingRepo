/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example.ELN;

import com.velox.api.clientcallback.DataRecordSelectionCriteria;
import com.velox.api.datarecord.DataRecord;
import com.velox.api.datatype.DataTypeDefinition;
import com.velox.api.datatype.TemporaryDataType;
import com.velox.api.datatype.fielddefinition.VeloxFieldDefinition;
import com.velox.api.eln.experimententry.ExperimentTableEntry;
import com.velox.api.eln.notebookexperiment.NotebookExperiment;
import com.velox.api.plugin.EnbPluginResult;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.NotebookExperimentMainToolbarPlugin;
import com.velox.api.plugin.invocation.context.NotebookExperimentMainToolbarContext;
import com.velox.api.plugin.invocation.context.OnNotebookExperimentMainToolbarContext;
import com.velox.api.report.CustomReport;
import com.velox.api.report.CustomReportManager;
import com.velox.api.report.RawTerm;
import com.velox.api.servermanager.DataTypeManager;
import com.velox.api.util.ServerException;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;
import com.velox.sapio.commons.exemplar.procedural.elements.Step;
import com.velox.sapio.commons.exemplar.report.CustomReportUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotebookAddRecordsPlugin extends ExemplarVeloxServerPlugin<NotebookExperimentMainToolbarContext> implements NotebookExperimentMainToolbarPlugin {
    @Override
    public String getLine1Text() {
        return "Add";
    }
    @Override
    public String getLine2Text() {
        return "Record";
    }
    @Override
    public boolean onNotebookExperimentMainToolbar(OnNotebookExperimentMainToolbarContext ctx) throws Throwable {
        return ctx.getNotebookExperiment() != null && !ctx.getNotebookExperiment().getStatus(user).equals("Completed");
    }
    @Override
    public int getOrder() {
        return 0;
    }
    @Override
    protected PluginResult run(NotebookExperimentMainToolbarContext ctx) throws Throwable
    {
        NotebookExperiment exp = ctx.getNotebookExperiment();
        DataTypeManager dTMan = dataMgmtServer.getDataTypeManager(user);
        List<String> dataTypeNameList = dTMan.getDataTypeNameList();

        List experimentEntryList = clientCallback.showListDialog("Data Types", dataTypeNameList, false, user);
        if (experimentEntryList == null || experimentEntryList.isEmpty()) {
            return new EnbPluginResult(true);
        }

        Integer selection = clientCallback.showOptionDialog("New or Existing", "Choose to create a new record, or select and existing", new String[] {"New", "Existing", "Cancel"}, 0, true);
        if (selection == null) {
            return new EnbPluginResult(true);
        }

        List<DataRecord> recordList = null;
        String dataTypeName = experimentEntryList.get(0).toString();

        if (selection == 0) {
            String result = clientCallback.showInputDialog("How many records to create: ");
            if (result == null || result.isEmpty()) {
                return new EnbPluginResult(true);
            }
            int numCreate = Integer.parseInt(result);

            recordList = dataRecordManager.addDataRecords(dataTypeName, numCreate, user);
            dataRecordManager.storeAndCommit("Created records", clientCallback.getClientCallbackRMI(), user);

            createTableEntryWithRecords(exp, dataTypeName, recordList);
            return new EnbPluginResult(true);
        }

        else if (selection == 1) {
            CustomReportManager crm = dataMgmtServer.getCustomReportManager(user);
            CustomReport report = new CustomReport();
            DataTypeDefinition dataTypeDef = dTMan.getDataTypeDefinition(dataTypeName);

            report.setSearchDataType(dataTypeName);
            report.addTerm(RawTerm.field(dataTypeName, "RecordId").greaterThan().value("0"));
            report.addColumn(dataTypeName, "RecordId");

            Map<String, VeloxFieldDefinition<?>> allFields = dataTypeDef.getVeloxFieldDefinitionMapWithExtensions(user);
            List<String> fieldNames = allFields.values().stream()
                    .filter(f -> !f.isSystemField())           // skip RecordId system metadata if you want user fields only
                    .map(VeloxFieldDefinition::getDataFieldName)
                    .sorted()
                    .toList();

            for (String fieldName : fieldNames) {
                report.addColumn(dataTypeName, fieldName);
            }

            report.setPageSize(100);
            report.setPageNumber(0);

            CustomReport results = crm.runCustomReport(report, user);

            List<Map<String, Object>> displayRows = CustomReportUtil.getResultFieldNameListCopy(results);
            if (displayRows.isEmpty()) {
                return new EnbPluginResult(true);
            }


            TemporaryDataType temporaryDataType = dataTypeDef.getTemporaryDataType("Default", user);

            DataRecordSelectionCriteria criteria = DataRecordSelectionCriteria.builder()
                    .records(displayRows)
                    .temporaryDataType(temporaryDataType)
                    .multiSelect(true)
                    .build();

            List<Map<String, Object>> selectedRows = clientCallback.showDataRecordSelectionDialog(
                    "Choose Existing Records",
                    criteria,
                    user);
            if (selectedRows == null || selectedRows.isEmpty()) {
                return new EnbPluginResult(true);
            }

            List<Object> recordIds = new ArrayList<>();
            for (Map<String, Object> row : selectedRows) {
                recordIds.add(row.get("RecordId"));
            }
            recordList = dataRecordManager.queryDataRecords(dataTypeName, "RecordId", recordIds, user);
            createTableEntryWithRecords(exp, dataTypeName, recordList);
            return new EnbPluginResult(true);
        }


        return new EnbPluginResult(true);
    }
    @Override
    public byte[] getIcon() {
        return getIcon("complete-workflow.svg");
    }
    @Override
    public String getDescription() {
        return "This button will add records to an entry";
    }

    public void createTableEntryWithRecords(NotebookExperiment exp, String dataTypeName, List<DataRecord> records) throws ServerException, RemoteException {
        Step activeStep1 = activeStep;
        int prevStep = activeStep1.getOrder();

        ExperimentTableEntry tableEntry = exp.addExperimentTableEntry("Created Records", prevStep + 1, dataTypeName, user);

        exp.addRecordsToTableEntry(tableEntry, records, user);
        exp.setExperimentEntry(tableEntry, user);
        experimentManager.storeNotebookExperiment(exp, clientCallback.getClientCallbackRMI(), user);
    }

    public List<DataRecord> mapListToDRList(List<Map<String, Object>> mapList) {
        List<DataRecord> mapToList = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            for (Object value : map.values()) {
                mapToList.add((DataRecord) value); // Explicitly cast Object to DataRecord
            }
        }
        return mapToList;
    }

}
