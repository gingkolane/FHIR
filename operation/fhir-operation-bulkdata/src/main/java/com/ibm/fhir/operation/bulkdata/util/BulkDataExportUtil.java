/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.operation.bulkdata.util;

import static com.ibm.fhir.model.type.String.string;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;

import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.resource.OperationOutcome.Issue;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Parameters.Parameter;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.model.type.code.IssueSeverity;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.model.util.ModelSupport;
import com.ibm.fhir.operation.bulkdata.BulkDataConstants;
import com.ibm.fhir.operation.bulkdata.BulkDataConstants.ExportType;
import com.ibm.fhir.operation.bulkdata.model.PollingLocationResponse;
import com.ibm.fhir.server.operation.spi.FHIROperationContext;

/**
 * BulkData Util captures common methods
 */
public class BulkDataExportUtil {

    private BulkDataExportUtil() {
        // No Operation
    }

    /**
     * Check the Export Type is valid and converts to intermediate enum
     *
     * @param type
     * @param resourceType
     * @return
     */
    public static BulkDataConstants.ExportType checkExportType(FHIROperationContext.Type type,
            Class<? extends Resource> resourceType) {
        ExportType exportType = ExportType.INVALID;
        if (FHIROperationContext.Type.INSTANCE.equals(type) && "Group".equals(resourceType.getSimpleName())) {
            exportType = ExportType.GROUP;
        } else if (FHIROperationContext.Type.RESOURCE_TYPE.equals(type)
                && "Patient".equals(resourceType.getSimpleName())) {
            exportType = ExportType.PATIENT;
        } else if (FHIROperationContext.Type.SYSTEM.equals(type)) {
            exportType = ExportType.SYSTEM;
        }
        return exportType;
    }

    public static MediaType checkAndConvertToMediaType(Parameters parameters)
            throws FHIROperationException {
        /*
         * The format for the requested bulk data files to be generated as per [FHIR Asynchronous Request
         * Pattern](http://hl7.org/fhir/async.html). Defaults to application/fhir+ndjson. Servers SHALL support [Newline
         * Delimited JSON](http://ndjson.org), but MAY choose to support additional output formats. Servers SHALL accept
         * the full content type of application/fhir+ndjson as well as the abbreviated representations
         * application/ndjson and ndjson.
         */
        Optional<Parameter> parameter = parameters.getParameter().stream()
                .filter(p -> BulkDataConstants.PARAM_OUTPUT_FORMAT.equals(p.getName().getValue()))
                .findFirst();

        String mediaType = FHIRMediaType.APPLICATION_NDJSON;
        if (parameter.isPresent() && parameter.get().getValue().is(com.ibm.fhir.model.type.String.class)) {
            mediaType = retrieveOutputFormat(parameter.get().getValue().as(com.ibm.fhir.model.type.String.class).getValue());
        }

        return MediaType.valueOf(mediaType);
    }

    private static String retrieveOutputFormat(String requestedFormat) throws FHIROperationException {
        // If the parameter isn't passed, use application/fhir+ndjson
        String finalValue = FHIRMediaType.APPLICATION_NDJSON;

        if (requestedFormat != null) {
            // Normalize the NDJSON variants to MEDIA_TYPE_ND_JSON
            if (BulkDataConstants.NDJSON_VARIANTS.contains(requestedFormat)) {
                requestedFormat = FHIRMediaType.APPLICATION_NDJSON;
            }

            // We're checking that it's acceptable.
            if (!BulkDataConstants.EXPORT_FORMATS.contains(requestedFormat)) {
                // Workaround for Liberty/CXF replacing "+" with " "
                requestedFormat = requestedFormat.replaceAll(" ", "+");
            }

            if (BulkDataConstants.EXPORT_FORMATS.contains(requestedFormat)) {
                finalValue = requestedFormat;
            } else {
                throw buildOperationException("Invalid requested format: '" + requestedFormat + "'" , IssueType.INVALID);
            }
        }
        return finalValue;
    }

    public static FHIROperationException buildOperationException(String errMsg, IssueType issueType) {
        FHIROperationException operationException = new FHIROperationException(errMsg);

        List<Issue> issues = new ArrayList<>();
        issues.add(Issue.builder()
                .code(issueType)
                .diagnostics(string(errMsg))
                .severity(IssueSeverity.ERROR)
                .build());

        operationException.setIssues(issues);
        return operationException;
    }

    /**
     * @param parameters
     * @return
     * @throws FHIROperationException
     */
    public static Instant checkAndExtractSince(Parameters parameters) {
        /*
         * Resources will be included in the response if their state has changed after the supplied time (e.g. if
         * Resource.meta.lastUpdated is later than the supplied _since time).
         */
        if (parameters != null) {
            for (Parameters.Parameter parameter : parameters.getParameter()) {
                // Parameter name is non-null (required per spec).
                if (BulkDataConstants.PARAM_SINCE.equals(parameter.getName().getValue())) {
                    if (parameter.getValue() != null) {
                        if (parameter.getValue().is(com.ibm.fhir.model.type.String.class)) {
                            return Instant.of(parameter.getValue().as(com.ibm.fhir.model.type.String.class).getValue());
                        } else if (parameter.getValue().is(com.ibm.fhir.model.type.Instant.class)) {
                            return Instant
                                    .of(parameter.getValue().as(com.ibm.fhir.model.type.Instant.class).getValue());
                        }
                    }
                    // No matching type found
                    return null;
                }
            }
        }
        return null;
    }

    public static List<String> checkAndValidateTypes(Parameters parameters) throws FHIROperationException {
        /*
         * Only resources of the specified resource types(s) SHALL be included in the response. If this parameter is
         * omitted, the server SHALL return all supported resources within the scope of the client authorization. For
         * Patient- and Group-level requests, the Patient Compartment SHOULD be used as a point of reference for
         * recommended resources to be returned. However, other resources outside of the patient compartment that are
         * helpful in interpreting the patient data (such as Organization and Practitioner) may also be returned.
         * Servers unable to support _type SHOULD return an error and OperationOutcome resource so clients can re-submit
         * a request omitting the _type parameter. Resource references MAY be relative URLs with the format <resource
         * type>/<id>, or absolute URLs with the same structure rooted in the base URL for the server from which the
         * export was performed. References will be resolved by looking for a resource with the specified type and id
         * within the file set. For example _type=Practitioner could be used to bulk data extract all Practitioner
         * resources from a FHIR endpoint.
         */
        List<String> result = new ArrayList<>();
        if (parameters != null) {
            for (Parameters.Parameter parameter : parameters.getParameter()) {
                // The model makes sure getName is never non-null.
                if (BulkDataConstants.PARAM_TYPE.equals(parameter.getName().getValue())) {
                    if (parameter.getValue() != null) {
                        String types =
                                parameter.getValue().as(com.ibm.fhir.model.type.String.class).getValue();
                        for (String type : types.split(",")) {
                            // Type will never be null here.
                            if (!type.isEmpty() && ModelSupport.isResourceType(type)) {
                                result.add(type);
                            } else {
                                throw buildOperationException(
                                        "invalid resource type sent as a parameter to $export operation", IssueType.INVALID);
                            }
                        }
                    } else {
                        throw buildOperationException(
                                "invalid resource type sent as a parameter to $export operation", IssueType.INVALID);
                    }
                }
            }
        }
        return result;
    }

    public static List<String> checkAndValidateTypeFilters(Parameters parameters) throws FHIROperationException {
        /*
         * To request finer-grained filtering, a client MAY supply a _typeFilter parameter alongside the _type
         * parameter. The value of the _typeFilter parameter is a comma-separated list of FHIR REST API queries that
         * further restrict the results of the query. Servers MAY further limit the data returned to a specific client
         * in accordance with local considerations (e.g. policies or regulations). Understanding _typeFilter is OPTIONAL
         * for FHIR servers; clients SHOULD be robust to servers that ignore _typeFilter. Note for client developers:
         * Because both _typeFilter and _since can restrict the results returned, the interaction of these parameters
         * may be surprising. Think carefully through the implications when constructing a query with both of these
         * parameters. As the _typeFilter is experimental and optional, we have not yet determined expectation for
         * _include, _revinclude, or support for any specific search parameters.
         */
        List<String> result = new ArrayList<>();
        if (parameters != null) {
            for (Parameters.Parameter parameter : parameters.getParameter()) {
                if (BulkDataConstants.PARAM_TYPE_FILTER.equals(parameter.getName().getValue())) {
                    if (parameter.getValue() != null && parameter.getValue().is(com.ibm.fhir.model.type.String.class)) {
                        String typeFilters =
                                parameter.getValue().as(com.ibm.fhir.model.type.String.class).getValue();

                        for (String typeFilter : typeFilters.split(",")) {
                            // Type will never be null here, just check for blanks
                            if (!typeFilter.isEmpty()) {
                                result.add(typeFilter);
                            } else {
                                throw buildOperationException(
                                        "invalid typeFilter sent as a parameter to $export operation", IssueType.INVALID);
                            }
                        }
                        return result;
                    }
                    // Result must have NOT been returned.
                    throw buildOperationException(
                            "invalid typeFilter parameter type sent to $export operation", IssueType.INVALID);
                }
            }
        }
        return result;
    }

    public static Parameters getOutputParametersWithJson(PollingLocationResponse resource) throws Exception {
        Parameters.Builder parametersBuilder = Parameters.builder();
        parametersBuilder
                .parameter(Parameter.builder().name(string("return"))
                        .value(string(PollingLocationResponse.Writer.generate(resource))).build());
        return parametersBuilder.build();
    }

    /**
     * checks and validates the job.
     *
     * @param parameters
     * @return
     * @throws FHIROperationException
     */
    public static String checkAndValidateJob(Parameters parameters) throws FHIROperationException {
        if (parameters != null) {
            for (Parameters.Parameter parameter : parameters.getParameter()) {
                if (BulkDataConstants.PARAM_JOB.equals(parameter.getName().getValue())
                        && parameter.getValue() != null && parameter.getValue().is(com.ibm.fhir.model.type.String.class)) {
                    String job = decryptBatchJobId(parameter.getValue().as(com.ibm.fhir.model.type.String.class).getValue(), BulkDataConstants.BATCHJOBID_ENCRYPTION_KEY);

                    // The job is never going to be empty or null as STRING is never empty at this point.
                    if (job.contains("/") || job.contains("?")) {
                        throw new FHIROperationException("job passed is invalid and is not supported");
                    }
                    // Don't look at any other parameters.
                    return job;
                }
            }
        }

        throw new FHIROperationException("no job identifier is passed");
    }


    public static String encryptBatchJobId(String strToEncrypt, SecretKeySpec key) {
        // Encrypt and UrlEncode the batch job id.
        if (key == null) {
            return strToEncrypt;
        } else {
            try {
                // Use light weight encryption without salt to simplify both the encryption/decryption and also config.
                Cipher cp = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cp.init(Cipher.ENCRYPT_MODE, key);

                // Encrypt the job id, base64-encode it, and replace all `/` chars with the less problematic `_` char
                String encodedJobId = Base64.getEncoder().withoutPadding().encodeToString(cp.doFinal(strToEncrypt.getBytes("UTF-8"))).replaceAll("/", "_");
                // The encrypted job id is used in the polling content location url directly, so urlencode here.
                return java.net.URLEncoder.encode(encodedJobId, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                return strToEncrypt;
            }
        }
    }

    public static String decryptBatchJobId(String strToDecrypt, SecretKeySpec key) {
        // Decrypt to get the batch job id.
        if (key == null) {
            return strToDecrypt;
        } else {
            try {
                // Use light weight encryption without salt to simplify both the encryption/decryption and also config.
                Cipher cp = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cp.init(Cipher.DECRYPT_MODE, key);
                // The encrypted job id has already been urldecoded by liberty runtime before reaching this function,
                // so, we don't do urldecode here.
                return new String(cp.doFinal(Base64.getDecoder().decode(strToDecrypt.replaceAll("_", "/"))), "UTF-8");
            } catch (Exception e) {
                return strToDecrypt;
            }
        }
    }
}
