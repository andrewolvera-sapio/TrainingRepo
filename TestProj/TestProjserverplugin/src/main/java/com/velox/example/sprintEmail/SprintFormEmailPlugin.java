/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example.sprintEmail;

import com.velox.api.datarecord.DataRecord;
import com.velox.api.datarecord.NotFound;
import com.velox.api.datatype.DataTypeDefinition;
import com.velox.api.datatype.TemporaryDataType;
import com.velox.api.datatype.datatypelayout.DataFormComponent;
import com.velox.api.datatype.datatypelayout.DataTypeLayout;
import com.velox.api.datatype.datatypelayout.DataTypeTabDefinition;
import com.velox.api.datatype.fielddefinition.FieldDefinitionPosition;
import com.velox.api.datatype.fielddefinition.VeloxDateFieldDefinition;
import com.velox.api.datatype.fielddefinition.VeloxFieldDefinition;
import com.velox.api.datatype.fielddefinition.VeloxStringFieldDefinition;
import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.FormToolbarPlugin;
import com.velox.api.plugin.invocation.context.FormToolbarContext;
import com.velox.api.plugin.invocation.context.OnFormToolbarContext;
import com.velox.api.servermanager.DataTypeManager;
import com.velox.api.util.ServerException;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.rmi.RemoteException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class SprintFormEmailPlugin extends ExemplarVeloxServerPlugin<FormToolbarContext> implements FormToolbarPlugin {
    @Override
    public String getLine1Text() {
        return "Email";
    }
    @Override
    public String getLine2Text() {
        return "Contact";
    }

    @Override
    protected PluginResult run(FormToolbarContext ctx) throws Throwable {
        List<DataRecord> sprints = new ArrayList<>();
        sprints.add(ctx.getDataRecord());
        execute(sprints);
        return new PluginResult(true);
    }

    @Override
    public boolean onFormToolbar(OnFormToolbarContext ctx) throws Throwable {
        return ctx.getDataTypeName().equals("Sprint");

    }

    @Override
    public byte[] getIcon() {
        try {
            return getIcon("Your Path from src/main/resources for the image icon.");
        } catch (Exception e){

        }
        return new byte[0];
    }

    protected void execute(List<DataRecord> sprints) throws Throwable {
        try{
            //Get contact values from sprints
            List<String> allContacts = new ArrayList<>();
            for(DataRecord sprint: sprints) {
                String contactsString = sprint.getStringVal("Contact", user);
                if (contactsString.isEmpty()) {
                    throw new RuntimeException("Missing contact");
                }
                String[] contactNames = contactsString.split(",");
                allContacts.addAll(Arrays.asList(contactNames));
            }

            //For each contact name, get corresponding record and save emails
            List<String> emails = new ArrayList<>();
            for(String contactName: allContacts){
                DataRecord contactRecord = getContactRecord(contactName);
                String email = contactRecord.getStringVal("Email", user);
                if (!email.isEmpty()) { emails.add(email); }
            }

            //If emails list is empty, throw error
            if(emails.isEmpty()){
                throw new RuntimeException("No available emails");
            }

            //Ask for email contents
            DataTypeManager dtm = dataMgmtServer.getDataTypeManager(user);
            DataTypeDefinition emailDefinition = dtm.getDataTypeDefinition("Email");
            TemporaryDataType newEmail = new TemporaryDataType("Email", "Email", "Emails");
            Map<String, VeloxFieldDefinition<?>> emailFields = emailDefinition.getVeloxFieldDefinitionMap(user);
            Collection<VeloxFieldDefinition<?>> emailFieldsCollection = emailFields.values();

            //Field position variables for email fields
            FieldDefinitionPosition addressPosition = null;
            FieldDefinitionPosition sentFromPosition = null;
            FieldDefinitionPosition sentDatePosition = null;
            FieldDefinitionPosition bodyPosition = null;

            for (VeloxFieldDefinition<?> fieldDefinition : emailFieldsCollection){
                switch (fieldDefinition.getDataFieldName()){
                    case "AddressedTo":
                        // You can now edit the defaultValue of the field.
                        VeloxStringFieldDefinition addressedField = (VeloxStringFieldDefinition) fieldDefinition;
                        addressedField.setDefaultValue(String.join(";", emails));
                        addressPosition = addressedField.fieldPositionBuilder().formColumn(0).build();
                        break;
                    case "SentFrom":
                        VeloxStringFieldDefinition sentFormField = (VeloxStringFieldDefinition) fieldDefinition;
                        sentFormField.setDefaultValue(user.getEmailAddress());
                        sentFromPosition = sentFormField.fieldPositionBuilder().formColumn(0).build();
                        break;
                    case "SentDate":
                        VeloxDateFieldDefinition sentDateDefinition = (VeloxDateFieldDefinition) fieldDefinition;
                        sentDateDefinition.setDefaultValue(LocalDate.now().toString());
                        sentDatePosition = sentDateDefinition.fieldPositionBuilder().formColumn(0).build();
                        break;
                    case "Body":
                        VeloxStringFieldDefinition bodyDefinition = (VeloxStringFieldDefinition) fieldDefinition;
                        bodyPosition = bodyDefinition.fieldPositionBuilder().formColumn(0).build();
                }
            }
            newEmail.setVeloxFieldDefinitionList(emailFieldsCollection);

            //Format field entry
            List<FieldDefinitionPosition> fieldDefinitionPositions = Arrays.asList(addressPosition, sentFromPosition, sentDatePosition, bodyPosition);
            DataFormComponent dataFormComponent = new DataFormComponent("Email Data Form", "Email Data Form");
            dataFormComponent.setColumn(0);
            dataFormComponent.setColumnSpan(4);
            dataFormComponent.setHeight(4);
            dataFormComponent.setCollapsed(false);
            dataFormComponent.setHideHeading(false);
            dataFormComponent.setFieldDefinitionPositionList(fieldDefinitionPositions);

            //Format tab
            DataTypeTabDefinition dataTypeTabDefinition = new DataTypeTabDefinition("Email Tab", "Email Tab");
            dataTypeTabDefinition.setTabOrder(0);
            dataTypeTabDefinition.setDataTypeLayoutComponent(dataFormComponent);

            //Create data type layout
            DataTypeLayout dataTypeLayout = new DataTypeLayout("Email Data Type Layout", "Email Data Type Layout", "description");
            dataTypeLayout.setDataTypeTabDefinition(dataTypeTabDefinition);
            newEmail.setDataTypeLayout(dataTypeLayout);

            //Show field entry dialog about new email
            Map<String, Object> emailResult = clientCallback.showFieldEntryDialog("New Email: ", "Draft new email", newEmail, user);

            String addressedTo = (String) emailResult.get("AddressedTo");
            String sentFrom = (String) emailResult.get("SentFrom");
            String sentDate = Instant.ofEpochMilli((long) emailResult.get("SentDate")).toString();
            String messageBody = (String) emailResult.get("Body");
            String formattedEmail = "Addressed To: " + addressedTo + "\nSent From: " + sentFrom + "\nDate: " + sentDate + "\n\nMessage Body:\n" + messageBody;
            clientCallback.displayInfo(formattedEmail);

        }catch(RuntimeException e){
            List<Object> resultList = new ArrayList<>();
            resultList.add("Runtime error: " + e);
        }
    }

    private DataRecord getContactRecord(String contactName) throws ServerException, RemoteException, NotFound {
        List<DataRecord> contacts = dataRecordManager.getAllRecordsOfType("Contact", user);
        for (DataRecord contact: contacts) {
            if (contact.getDataField("Name", user).equals(contactName)) {
                return contact;
            }
        }
        return null;
    }
}
