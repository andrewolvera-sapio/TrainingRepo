/*
 * Copyright (C) 2005 - Sapio Sciences <support@sapiosciences.com>
 * ====================================================================
 * This software is the property of Sapio Sciences.
 * ====================================================================
 */
package com.velox.example;

import com.velox.api.plugin.PluginResult;
import com.velox.api.plugin.invocation.ActionMenuPlugin;
import com.velox.api.plugin.invocation.context.ActionMenuContext;
import com.velox.sapio.commons.exemplar.classloadersupport.ExemplarResourceLoader;
import com.velox.sapio.commons.exemplar.plugin.veloxplugin.ExemplarVeloxServerPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Hello from Sapio Sciences!
 * This is an example server-side plugin. See plugin concept guide for details.
 */
public class ExampleServerSidePlugin extends ExemplarVeloxServerPlugin<ActionMenuContext> implements ActionMenuPlugin {
	
	@Override
	public List<String> getSectionNamePath() {
		return Arrays.asList("Demo","Example");
	}
	
	@Override
	public String getLine1Text() {
		return "Demo";
	}
	
	@Override
	public String getLine2Text() {
		return "Example";
	}
	
	@Override
	protected PluginResult run(ActionMenuContext actionMenuContext) throws Throwable {
		displayInfo("Hello World!");
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

//	@Override
//	public PluginResult runPlugin(ActionMenuContext ctx) throws Throwable {
//		return null;
//	}
}
