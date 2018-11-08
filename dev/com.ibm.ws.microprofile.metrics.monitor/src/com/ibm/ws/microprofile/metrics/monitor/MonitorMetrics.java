/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

public class MonitorMetrics {

	private static final TraceComponent tc = Tr.register(MonitorMetrics.class);
	
	protected String objectName;
	private String mbeanStatsName;
	private MBeanServer mbs;
	private Set<String> metricNames;

	public MonitorMetrics(String objectName) {
		this.mbs = ManagementFactory.getPlatformMBeanServer();
		this.objectName = objectName;
		this.metricNames = new HashSet<String>();
	}

	public void createMetrics(SharedMetricRegistries sharedMetricRegistry, String[][] data) {
		
        MetricRegistry registry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
        
        for (String[] metricData : data) {
            String metricName = getMetricName(metricData[MappingTable.METRIC_NAME]);
            MetricType type = MetricType.valueOf(metricData[MappingTable.METRIC_TYPE]);
            if (MetricType.COUNTER.equals(type)) {
                MonitorCounter mc = metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ? 
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
                        new MonitorCounter(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
        		registry.register(
        			new Metadata(metricName, metricData[MappingTable.METRIC_DISPLAYNAME], metricData[MappingTable.METRIC_DESCRIPTION], type, metricData[MappingTable.METRIC_UNIT]), 
        			mc);            	
        		metricNames.add(metricName);
        		Tr.debug(tc, "Registered " + metricName);
        	} else if (MetricType.GAUGE.equals(type)) {
            	MonitorGauge<Number> mg =  metricData[MappingTable.MBEAN_SUBATTRIBUTE] == null ?
            		new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE]) :
            		new MonitorGauge<Number>(mbs, objectName, metricData[MappingTable.MBEAN_ATTRIBUTE], metricData[MappingTable.MBEAN_SUBATTRIBUTE]);
        		registry.register(
        			new Metadata(metricName, metricData[MappingTable.METRIC_DISPLAYNAME], metricData[MappingTable.METRIC_DESCRIPTION], type, metricData[MappingTable.METRIC_UNIT]), 
        			mg);
        		metricNames.add(metricName);
        		Tr.debug(tc, "Registered " + metricName);
            } else {
            	Tr.debug(tc, "Falied to register " + metricName + " because of invalid type " + type);
            }
        }		
	}

	private String getMBeanStatsName() {
    	if (mbeanStatsName == null) {
    		mbeanStatsName = "unknown";
            for (String subString : objectName.split(",")) {
                subString = subString.trim();
                if (subString.contains("name=")) {
                	mbeanStatsName  = subString.split("=")[1];
                	mbeanStatsName = mbeanStatsName.replaceAll(" ", "_"); 
                	mbeanStatsName = mbeanStatsName.replaceAll("/", "_");
                	break;
                }
            }
    	}
        return mbeanStatsName;
	}
	
	private String getMetricName(String name)  {
		return name.replace("%s", getMBeanStatsName());
	}

	public void unregisterMetrics(SharedMetricRegistries sharedMetricRegistry) {
		MetricRegistry registry = sharedMetricRegistry.getOrCreate(MetricRegistry.Type.VENDOR.getName());
		for (String metricName : metricNames) {
			boolean rc = registry.remove(metricName);
			Tr.debug(tc, "Unregistered " + metricName + " " + (rc ? "successfully" : "unsuccessfully"));
		}
		metricNames.clear();
	}
}
