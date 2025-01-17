/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test;

import static com.ibm.fhir.model.type.String.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Encounter;
import com.ibm.fhir.model.resource.Endpoint;
import com.ibm.fhir.model.resource.Location;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Organization;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.resource.Procedure;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.ContactPoint;
import com.ibm.fhir.model.type.Date;
import com.ibm.fhir.model.type.DateTime;
import com.ibm.fhir.model.type.Decimal;
import com.ibm.fhir.model.type.Duration;
import com.ibm.fhir.model.type.HumanName;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.Period;
import com.ibm.fhir.model.type.Reference;
import com.ibm.fhir.model.type.Uri;
import com.ibm.fhir.model.type.code.AdministrativeGender;
import com.ibm.fhir.model.type.code.ContactPointSystem;
import com.ibm.fhir.model.type.code.EncounterStatus;
import com.ibm.fhir.model.type.code.ProcedureStatus;
import com.ibm.fhir.model.type.code.ResourceType;

/**
 * The tests execute the reverse chained behavior in order to exercise reference chains.
 */
public class SearchReverseChainTest extends FHIRServerTestBase {
    private String patient1Id;
    private String patient2Id;
    private String procedure1Id;
    private String procedure2Id;
    private String organization1Id;
    private String organization2Id;
    private String encounter1Id;
    private String encounter2Id;
    private String endpointId;
    private String locationId;
    private Instant now = Instant.now();
    private String tag = Long.toString(now.toEpochMilli());

    @Test(groups = { "server-search-reverse-chain" })
    public void testCreateEndpoint() throws Exception {
        WebTarget target = getWebTarget();

       // Build a new Endpoint.
        Endpoint endpoint = TestUtil.getMinimalResource(ResourceType.ENDPOINT, Format.JSON);
        endpoint = endpoint.toBuilder().name(of(tag)).build();

        // Call the 'create' API.
        Entity<Endpoint> entity = Entity.entity(endpoint, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Endpoint").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the endpoint's logical id value.
        endpointId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new Endpoint and verify it.
        response = target.path("Endpoint/" + endpointId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEndpoint"})
    public void testCreateOrganization1() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Organization.
        Organization organization = TestUtil.getMinimalResource(ResourceType.ORGANIZATION, Format.JSON);
        organization = organization.toBuilder()
                .name(of(tag))
                .endpoint(Reference.builder().reference(of("Endpoint/" + endpointId)).build())
                .build();

        // Call the 'create' API.
        Entity<Organization> entity = Entity.entity(organization, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Organization").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the organization's logical id value.
        organization1Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new organization and verify it.
        response = target.path("Organization/" + organization1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEndpoint"})
    public void testCreateOrganization2() throws Exception {
        WebTarget target = getWebTarget();

       // Build a new Organization.
        Organization organization = TestUtil.getMinimalResource(ResourceType.ORGANIZATION, Format.JSON);
        organization = organization.toBuilder()
                .name(of(tag))
                .endpoint(Reference.builder().reference(of("Endpoint/" + endpointId)).build())
                .build();

        // Call the 'create' API.
        Entity<Organization> entity = Entity.entity(organization, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Organization").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the organization's logical id value.
        organization2Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new organization and verify it.
        response = target.path("Organization/" + organization2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateOrganization1"})
    public void testCreatePatient1() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = TestUtil.getMinimalResource(ResourceType.PATIENT, Format.JSON);
        patient = patient.toBuilder()
                .gender(AdministrativeGender.MALE)
                .name(HumanName.builder()
                    .given(of("1" + tag))
                    .build())
                .meta(Meta.builder()
                    .tag(Coding.builder()
                        .code(Code.of(tag))
                        .build())
                    .build())
                .generalPractitioner(Reference.builder().reference(of("PractitionerRole/" + tag)).build())
                .managingOrganization(Reference.builder().reference(of("Organization/" + organization1Id)).build())
                .birthDate(Date.of(now.toString().substring(0,10)))
                .telecom(ContactPoint.builder().system(ContactPointSystem.PHONE).value(of("1" + tag)).build())
               .build();

        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the patient's logical id value.
        patient1Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/" + patient1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreatePatient1", "testCreateOrganization2"})
    public void testCreatePatient2() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Patient and then call the 'create' API.
        Patient patient = TestUtil.getMinimalResource(ResourceType.PATIENT, Format.JSON);
        patient = patient.toBuilder()
                .gender(AdministrativeGender.FEMALE)
                .name(HumanName.builder()
                    .given(of("2" + tag))
                    .build())
                .meta(Meta.builder()
                    .tag(Coding.builder()
                        .code(Code.of(tag))
                        .build())
                    .build())
                .generalPractitioner(Reference.builder().reference(of("Practitioner/" + tag)).build())
                .managingOrganization(Reference.builder().reference(of("Organization/" + organization2Id)).build())
                .birthDate(Date.of(now.minus(1, ChronoUnit.DAYS).toString().substring(0,10)))
                .telecom(ContactPoint.builder().system(ContactPointSystem.PHONE).value(of("2" + tag)).build())
                .build();

        Entity<Patient> entity = Entity.entity(patient, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Patient").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the patient's logical id value.
        patient2Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new patient and verify it.
        response = target.path("Patient/" + patient2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreatePatient1"})
    public void testCreateProcedure1() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Procedure and add subject reference to patient.
        Procedure procedure = TestUtil.getMinimalResource(ResourceType.PROCEDURE, Format.JSON);
        procedure = procedure.toBuilder()
                .status(ProcedureStatus.COMPLETED)
                .subject(Reference.builder().reference(of("Patient/" + patient1Id)).build())
                .basedOn(Reference.builder().reference(of("CarePlan/" + tag)).build())
                .performed(DateTime.of(now.toString()))
                .instantiatesUri(Uri.of("1" + tag))
                .code(CodeableConcept.builder().coding(Coding.builder().code(Code.of("1" + tag)).build()).build())
                .build();

        // Call the 'create' API.
        Entity<Procedure> entity = Entity.entity(procedure, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Procedure").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the procedure's logical id value.
        procedure1Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new procedure and verify it.
        response = target.path("Procedure/" + procedure1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreatePatient2"})
    public void testCreateProcedure2() throws Exception {
        WebTarget target = getWebTarget();

        Reference reference = Reference.builder().reference(of("Patient/" + patient2Id)).build();

        // Build a new Procedure and add subject reference to patient.
        Procedure procedure = TestUtil.getMinimalResource(ResourceType.PROCEDURE, Format.JSON);
        procedure = procedure.toBuilder()
                .status(ProcedureStatus.COMPLETED)
                .subject(reference)
                .basedOn(Reference.builder().reference(of("ServiceRequest/" + tag)).build())
                .performed(DateTime.of(now.minus(1, ChronoUnit.DAYS).toString()))
                .instantiatesUri(Uri.of("2" + tag))
                .code(CodeableConcept.builder().coding(Coding.builder().code(Code.of("2" + tag)).build()).build())
                .build();

        // Call the 'create' API.
        Entity<Procedure> entity = Entity.entity(procedure, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Procedure").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the procedure's logical id value.
        procedure2Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new procedure and verify it.
        response = target.path("Procedure/" + procedure2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateOrganization1"})
    public void testCreateEncounter1() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Encounter and add reason-reference reference to procedure.
        Encounter encounter = TestUtil.getMinimalResource(ResourceType.ENCOUNTER, Format.JSON);
        encounter = encounter.toBuilder()
                .status(EncounterStatus.FINISHED)
                .reasonReference(Reference.builder().reference(of("Procedure/" + procedure1Id)).build())
                .serviceProvider(Reference.builder().reference(of("Organization/" + organization1Id)).build())
                .period(Period.builder().start(DateTime.of(now.toString())).end(DateTime.of(now.toString())).build())
                .length(Duration.builder().system(Uri.of("http://unitsofmeasure.org")).code(Code.of("s")).value(Decimal.of("1" + tag)).build())
                .type(CodeableConcept.builder().coding(Coding.builder().code(Code.of("1" + tag)).build()).build())
                .build();

        // Call the 'create' API.
        Entity<Encounter> entity = Entity.entity(encounter, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Encounter").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the encounter's logical id value.
        encounter1Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new encounter and verify it.
        response = target.path("Encounter/" + encounter1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure2", "testCreateOrganization2"})
    public void testCreateEncounter2() throws Exception {
        WebTarget target = getWebTarget();

        // Build a new Encounter and add reason-reference reference to procedure.
        Encounter encounter = TestUtil.getMinimalResource(ResourceType.ENCOUNTER, Format.JSON);
        encounter = encounter.toBuilder()
                .status(EncounterStatus.FINISHED)
                .reasonReference(Reference.builder().reference(of("Procedure/" + procedure2Id)).build())
                .serviceProvider(Reference.builder().reference(of("Organization/" + organization2Id)).build())
                .period(Period.builder().start(DateTime.of(now.minus(1, ChronoUnit.DAYS).toString())).end(DateTime.of(now.minus(1, ChronoUnit.DAYS).toString())).build())
                .length(Duration.builder().system(Uri.of("http://unitsofmeasure.org")).code(Code.of("s")).value(Decimal.of("2" + tag)).build())
                .type(CodeableConcept.builder().coding(Coding.builder().code(Code.of("2" + tag)).build()).build())
                .build();

        // Call the 'create' API.
        Entity<Encounter> entity = Entity.entity(encounter, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Encounter").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the encounter's logical id value.
        encounter2Id = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new encounter and verify it.
        response = target.path("Encounter/" + encounter2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateOrganization1"})
    public void testCreateLocation() throws Exception {
        WebTarget target = getWebTarget();

        Location location = TestUtil.readExampleResource("json/spec/location-example.json");
        location = location.toBuilder()
                .managingOrganization(Reference.builder().reference(of("Organization/" + organization1Id)).build())
                .build();

        // Call the 'create' API.
        Entity<Location> entity = Entity.entity(location, FHIRMediaType.APPLICATION_FHIR_JSON);
        Response response = target.path("Location").request().post(entity, Response.class);
        assertResponse(response, Response.Status.CREATED.getStatusCode());

        // Get the location's logical id value.
        locationId = getLocationLogicalId(response);

        // Next, call the 'read' API to retrieve the new Location and verify it.
        response   = target.path("Location/" + locationId).request(FHIRMediaType.APPLICATION_FHIR_JSON).get();
        assertResponse(response, Response.Status.OK.getStatusCode());
    }

    @AfterClass
    public void testDeleteResources() {
        WebTarget target = getWebTarget();
        if (patient1Id != null) {
            Response response   = target.path("Patient/" + patient1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (patient2Id != null) {
            Response response   = target.path("Patient/" + patient2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (procedure1Id != null) {
            Response response   = target.path("Procedure/" + procedure1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (procedure2Id != null) {
            Response response   = target.path("Procedure/" + procedure2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (organization1Id != null) {
            Response response   = target.path("Organization/" + organization1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (organization2Id != null) {
            Response response   = target.path("Organization/" + organization2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (encounter1Id != null) {
            Response response   = target.path("Encounter/" + encounter1Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (encounter2Id != null) {
            Response response   = target.path("Encounter/" + encounter2Id).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (endpointId != null) {
            Response response   = target.path("Endpoint/" + endpointId).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
        if (locationId != null) {
            Response response   = target.path("Location/" + locationId).request(FHIRMediaType.APPLICATION_FHIR_JSON).delete();
            assertResponse(response, Response.Status.OK.getStatusCode());
        }
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithTypeError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("")
                .queryParam("_type", "Patient")
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "system search not supported with _has");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithParseError1() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "An incorrect number of components were specified for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithParseError2() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "An incorrect number of components were specified for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithParseError3() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:extra:_has:Encounter:reason-reference:status", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "An incorrect number of components were specified for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithResourceTypeNotValidError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:BadType:subject:code", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Resource type 'BadType' is not valid for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmNotFoundError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:badSearchParm:code", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter 'badSearchParm' for resource type 'Procedure' was not found.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmNotFoundError2() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:badSearchParm", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter 'badSearchParm' for resource type 'Procedure' was not found.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmNotReferenceError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:code:code", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter 'code' is not of type reference for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmTargetTypeNotValidError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:encounter:code", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter 'encounter' target types do not include expected type 'Patient' for '_has' (reverse chain) search.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmUndefinedModifierError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:code:badModifier", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Undefined Modifier: 'badModifier'");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmUnsupportedModifierError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:code:contains", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Unsupported type/modifier combination: 'token'/'contains'");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithSearchParmIsResultParmError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:_total", "1")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter '_total' for resource type 'Procedure' was not found.");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithChainedSearchParmBadModifierError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:encounter:contains.status", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Modifier: 'contains' not allowed on chained parameter");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithChainedSearchParmNotReferenceError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:code.status", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Type: 'token' not allowed on chained parameter");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithChainedSearchParmResourceTypeNotValidError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:part-of:Condition.status", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Modifier resource type [Condition] is not allowed for search parameter [part-of] of resource type [Procedure].");
    }

    @Test(groups = { "server-search-reverse-chain" })
    public void testSearchReverseChainWithChainedSearchParmResourceTypeNotSpecifiedError() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:part-of.status", "test")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.BAD_REQUEST.getStatusCode());
        assertExceptionOperationOutcome(response.readEntity(OperationOutcome.class),
                "Search parameter: 'part-of' must have resource type name modifier");
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainMultipleResults() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(patient2Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainSingleResult() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_id", procedure1Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainMultipleResults() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:status", EncounterStatus.FINISHED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(patient2Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainSingleResult() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:_id", encounter2Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainORSearch() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:_id", encounter1Id + "," + encounter2Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(patient2Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainANDSearch() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:_id", encounter1Id, encounter2Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(0, bundle.getEntry().size());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithChainedSearchParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:service-provider.name", tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(patient2Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithChainedSearchParmOfId() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:service-provider._id", organization1Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithMultipleChainedSearchParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:service-provider.endpoint.name", tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(patient2Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithStringParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("given", "1" + tag)
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithTokenParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("telecom", "1" + tag)
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithReferenceParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("general-practitioner", "Practitioner/" + tag)
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithDateParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_tag", tag)
                .queryParam("birthdate", now.minus(1, ChronoUnit.DAYS).toString().substring(0,10))
                .queryParam("_has:Procedure:subject:status", ProcedureStatus.COMPLETED.getValue())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithUriParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:instantiates-uri", "1" + tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithDateTimeParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:date", now.minus(1, ChronoUnit.DAYS).toString())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithReferenceParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:based-on", "CarePlan/" + tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithTokenParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:code", "2" + tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithTokenParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:type", "1" + tag)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithReferenceParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:service-provider", "Organization/" + organization2Id)
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithDateTimeParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:date", now.toString())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient1Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateEncounter1", "testCreateEncounter2"})
    public void testSearchMultipleReverseChainWithQuantityParmLast() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:_has:Encounter:reason-reference:length", "2" + tag + "|http://unitsofmeasure.org|s")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(patient2Id, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = { "testCreateLocation" })
    public void SearchMultipleReverseChainWithLocationNear() throws Exception {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Endpoint")
                .queryParam("_has:Organization:endpoint:_has:Location:organization:near", "42.256500|-83.694810|11.20|km")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(endpointId, bundle.getEntry().get(0).getResource().getId());
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithInclude() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:instantiates-uri", "1" + tag)
                .queryParam("_include", "Patient:organization")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(organization1Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateProcedure1", "testCreateProcedure2"})
    public void testSearchSingleReverseChainWithRevInclude() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Patient")
                .queryParam("_has:Procedure:subject:instantiates-uri", "1" + tag)
                .queryParam("_revinclude", "Procedure:patient")
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());
        List<String> resourceIds = new ArrayList<>();
        for (Bundle.Entry entry : bundle.getEntry()) {
            resourceIds.add(entry.getResource().getId());
        }
        assertTrue(resourceIds.contains(patient1Id));
        assertTrue(resourceIds.contains(procedure1Id));
    }

    @Test(groups = { "server-search-reverse-chain" }, dependsOnMethods = {"testCreateLocation"})
    public void testSearchSingleReverseChainWithLastUpdatedParm() {
        WebTarget target = getWebTarget();
        Response response =
                target.path("Organization")
                .queryParam("_id", organization1Id)
                .queryParam("_has:Location:organization:_lastUpdated", "gt" + now.minus(1, ChronoUnit.DAYS).toString())
                .request(FHIRMediaType.APPLICATION_FHIR_JSON)
                .get();
        assertResponse(response, Response.Status.OK.getStatusCode());
        Bundle bundle = response.readEntity(Bundle.class);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
        assertEquals(organization1Id, bundle.getEntry().get(0).getResource().getId());
    }

}