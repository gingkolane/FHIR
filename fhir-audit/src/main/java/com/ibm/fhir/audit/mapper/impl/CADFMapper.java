/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.audit.mapper.impl;

import static com.ibm.fhir.audit.AuditLogServiceConstants.DEFAULT_AUDIT_GEO_CITY;
import static com.ibm.fhir.audit.AuditLogServiceConstants.DEFAULT_AUDIT_GEO_COUNTRY;
import static com.ibm.fhir.audit.AuditLogServiceConstants.DEFAULT_AUDIT_GEO_STATE;
import static com.ibm.fhir.audit.AuditLogServiceConstants.PROPERTY_AUDIT_GEO_CITY;
import static com.ibm.fhir.audit.AuditLogServiceConstants.PROPERTY_AUDIT_GEO_COUNTRY;
import static com.ibm.fhir.audit.AuditLogServiceConstants.PROPERTY_AUDIT_GEO_STATE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.ibm.fhir.audit.beans.AuditLogEntry;
import com.ibm.fhir.audit.beans.FHIRContext;
import com.ibm.fhir.audit.cadf.CadfAttachment;
import com.ibm.fhir.audit.cadf.CadfCredential;
import com.ibm.fhir.audit.cadf.CadfEndpoint;
import com.ibm.fhir.audit.cadf.CadfEvent;
import com.ibm.fhir.audit.cadf.CadfGeolocation;
import com.ibm.fhir.audit.cadf.CadfResource;
import com.ibm.fhir.audit.cadf.enums.Action;
import com.ibm.fhir.audit.cadf.enums.EventType;
import com.ibm.fhir.audit.cadf.enums.Outcome;
import com.ibm.fhir.audit.cadf.enums.ResourceType;
import com.ibm.fhir.audit.mapper.Mapper;
import com.ibm.fhir.config.PropertyGroup;

/**
 * This class is a Cadf/EventStream/COS based implementation of the FHIR server
 * AuditLogService interface
 */
public class CADFMapper implements Mapper {
    private static final String CLASSNAME = CADFMapper.class.getName();
    private static final Logger logger = java.util.logging.Logger.getLogger(CLASSNAME);

    // FHIR Operation To CADF
    private static final Map<String, Action> FHIR_TO_CADF = new HashMap<String, Action>() {
        private static final long serialVersionUID = 1L;
        {
            put("C", Action.create);
            put("R", Action.read);
            put("U", Action.update);
            put("D", Action.delete);
        }
    };

    private CadfEvent eventObject = null;

    private String hostname = null;
    private String geoCity = null;
    private String geoState = null;
    private String geoCountry = null;

    @Override
    public Mapper init(PropertyGroup auditLogProperties) throws Exception {
        // this may be unreliable on Windows and other systems.
        hostname = System.getenv("HOSTNAME");
        geoCity = auditLogProperties.getStringProperty(PROPERTY_AUDIT_GEO_CITY, DEFAULT_AUDIT_GEO_CITY);
        geoState = auditLogProperties.getStringProperty(PROPERTY_AUDIT_GEO_STATE, DEFAULT_AUDIT_GEO_STATE);
        geoCountry = auditLogProperties.getStringProperty(PROPERTY_AUDIT_GEO_COUNTRY, DEFAULT_AUDIT_GEO_COUNTRY);
        return this;
    }

    @Override
    public Mapper map(AuditLogEntry entry) throws Exception {
        eventObject = createCadfEvent(entry);
        return this;
    }

    @Override
    public String serialize() throws Exception {
        return CadfEvent.Writer.generate(eventObject);
    }

    public CadfEvent createCadfEvent(AuditLogEntry logEntry) throws IllegalStateException, IOException {
        final String METHODNAME = "createCadfEvent";
        logger.entering(CLASSNAME, METHODNAME);

        CadfResource observerRsrc = new CadfResource.Builder("fhir-server", ResourceType.compute_node)
                .geolocation(new CadfGeolocation.Builder(geoCity, geoState, geoCountry, null).build())
                .name("IBM FHIR Server - Audit")
                .host(hostname)
                .build();

        CadfEvent event = null;
        Outcome cadfEventOutCome;

        // For CADF we don't log specific event types.
        if ( logEntry.getContext() != null
                && logEntry.getContext().getAction() != null
                && logEntry.getContext().getApiParameters() != null) {
            // Define resources
            CadfResource initiator =
                    new CadfResource.Builder(logEntry.getTenantId() + "@" + logEntry.getComponentId(),
                            ResourceType.compute_machine)
                                    .geolocation(
                                            new CadfGeolocation.Builder(geoCity, geoState, geoCountry, null).build())
                                    .credential(
                                            new CadfCredential.Builder("user-" + logEntry.getUserName()).build())
                                    .host(logEntry.getComponentIp()).build();
            CadfResource target =
                    new CadfResource.Builder(
                            logEntry.getContext().getData() == null || logEntry.getContext().getData().getId() == null
                                    ? UUID.randomUUID().toString()
                                    : logEntry.getContext().getData().getId(),
                            ResourceType.data_database)
                                    .geolocation(
                                            new CadfGeolocation.Builder(geoCity, geoState, geoCountry, null).build())
                                    .address(
                                            new CadfEndpoint(logEntry.getContext().getApiParameters().getRequest(), "",
                                                    ""))
                                    .build();

            FHIRContext fhirContext = new FHIRContext(logEntry.getContext());
            fhirContext.setClient_cert_cn(logEntry.getClientCertCn());
            fhirContext.setClient_cert_issuer_ou(logEntry.getClientCertIssuerOu());
            fhirContext.setEventType(logEntry.getEventType());
            fhirContext.setLocation(logEntry.getLocation());
            fhirContext.setDescription(logEntry.getDescription());

            if (logEntry.getContext().getEndTime() == null ||
                    logEntry.getContext().getStartTime().equalsIgnoreCase(logEntry.getContext().getEndTime())) {
                cadfEventOutCome = Outcome.pending;
            } else if (logEntry.getContext().getApiParameters().getStatus() < 400) {
                cadfEventOutCome = Outcome.success;
            } else {
                cadfEventOutCome = Outcome.failure;
            }

            event = new CadfEvent.Builder(
                            logEntry.getContext().getRequestUniqueId() == null ? UUID.randomUUID().toString()
                                    : logEntry.getContext().getRequestUniqueId(),
                                    EventType.activity, logEntry.getTimestamp(),
                                    FHIR_TO_CADF.getOrDefault(logEntry.getContext().getAction(), Action.unknown),
                                    cadfEventOutCome)
                            .observer(observerRsrc)
                            .initiator(initiator)
                            .target(target)
                            .tag(logEntry.getCorrelationId())
                            .attachment(new CadfAttachment("application/json",
                                            FHIRContext.FHIRWriter.generate(fhirContext)))
                            .build();
        }

        logger.exiting(CLASSNAME, METHODNAME);
        return event;
    }
}