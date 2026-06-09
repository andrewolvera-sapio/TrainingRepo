/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example.ELN;

import com.velox.api.datafielddefinition.EnbFieldSet;
import com.velox.api.eln.experimententry.EnbEntry;
import com.velox.api.eln.experimententry.ExperimentEntry;
import com.velox.api.eln.experimententry.ExperimentEntryCriteria;
import com.velox.api.eln.experimententry.ExperimentFormEntry;
import com.velox.api.eln.experimententry.ExperimentTableEntry;
import com.velox.api.eln.notebookexperiment.NotebookExperiment;
import com.velox.api.plugin.EnbPluginResult;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.NotebookExperimentGrabberPlugin;
import com.velox.api.plugin.invocation.context.NotebookExperimentGrabberContext;
import com.velox.api.servermanager.EnbPredefinedFieldManager;
import com.velox.api.util.PopupType;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.util.List;

public class NotebookExperimentGrabber extends ExemplarVeloxServerPlugin<NotebookExperimentGrabberContext> implements NotebookExperimentGrabberPlugin {

    private static final String GRABBER_DISPLAY_NAME = "Instrument Tracker";
    private static final String FIELD_SET_BASE_DATA_TYPE = "ELNExperimentDetail";
    private static final String FIELD_SET_NAME = "Instrument Tracking";
    private static final String ENTRY_NAME = "Instrument Tracking";
    private static final String FIELD_SET_ALREADY_EXISTS_TITLE = "Error: ";
    private static final String FIELD_SET_ALREADY_EXISTS_MESSAGE = "The Instrument Tracking Field Set is already used in this Notebook.";

    @Override
    public String getGrabberDisplayName() {
        return GRABBER_DISPLAY_NAME;
    }

    @Override
    protected PluginResult run(NotebookExperimentGrabberContext ctx) throws Throwable {
        try {
            EnbPredefinedFieldManager enbPredefinedFieldManager = dataMgmtServer.getEnbPredefinedFieldManager(user);
            EnbFieldSet instrumentTrackingFieldSet = enbPredefinedFieldManager.getEnbFieldSet(
                    FIELD_SET_BASE_DATA_TYPE, FIELD_SET_NAME);

            NotebookExperiment notebookExperiment = ctx.getNotebookExperiment();
            if (notebookExperiment == null) {
                return new PluginResult(false);
            }

            List<ExperimentEntry> experimentEntries = notebookExperiment.getExperimentEntryList(user);
            long fieldSetId = instrumentTrackingFieldSet.getFieldSetId();

            if (experimentEntries != null) {
                for (ExperimentEntry entry : experimentEntries) {
                    if (entry.getEnbEntryType() == EnbEntry.EnbEntryType.Table) {
                        ExperimentTableEntry tableEntry = (ExperimentTableEntry) entry;
                        if (tableEntry.getFieldSetIdList().contains(fieldSetId)) {
                            clientCallback.displayPopup(
                                    FIELD_SET_ALREADY_EXISTS_TITLE,
                                    FIELD_SET_ALREADY_EXISTS_MESSAGE,
                                    PopupType.Error);
                            return new PluginResult(false);
                        }
                    } else if (entry.getEnbEntryType() == EnbEntry.EnbEntryType.Form) {
                        ExperimentFormEntry formEntry = (ExperimentFormEntry) entry;
                        if (formEntry.getFieldSetIdList().contains(fieldSetId)) {
                            clientCallback.displayPopup(
                                    FIELD_SET_ALREADY_EXISTS_TITLE,
                                    FIELD_SET_ALREADY_EXISTS_MESSAGE,
                                    PopupType.Error);
                            return new PluginResult(false);
                        }
                    }
                }
            }

            String dataTypeName = instrumentTrackingFieldSet.getEnbDataTypeName();
            ExperimentEntryCriteria tableEntryCriteria = ExperimentEntryCriteria.builder()
                    .dataTypeName(dataTypeName)
                    .experimentEntryName(ENTRY_NAME)
                    .isShownInTemplate(true)
                    .build();
            tableEntryCriteria.addEnbFieldSet(instrumentTrackingFieldSet);

            ExperimentTableEntry tableEntry = notebookExperiment.addExperimentTableEntry(tableEntryCriteria, user);
            notebookExperiment.setExperimentEntry(tableEntry, user);

            return new EnbPluginResult(true, tableEntry);
        } catch (Exception e) {
            return new PluginResult(false);
        }
    }

    @Override
    public byte[] getIcon() {
        return getIcon("test-grabber.svg");
    }

}
