package org.verapdf.rest.resources;


import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

public class VerificationHealthCheck extends HealthCheck {
    private final Client client;
    private final String endpointUrl;

    public VerificationHealthCheck(Environment environment, String endpointUrl) {
        ClientConfig config = new ClientConfig();
        config.register(MultiPartFeature.class);
        this.client = new JerseyClientBuilder()
                .withConfig(config)
                .property(ClientProperties.CONNECT_TIMEOUT, 5000)
                .property(ClientProperties.READ_TIMEOUT, 15000)
                .build();
        this.endpointUrl = endpointUrl;
    }

    @Override
    protected Result check() throws Exception {
        try {
            Path pdfFilePath = Paths.get("test/test_document.pdf");
            byte[] pdfBytes = Files.readAllBytes(pdfFilePath);

            FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
            formDataMultiPart.field("file", pdfBytes, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            Response response = client.target(endpointUrl)
                    .path("/validate/ua1")
                    .request(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA));

            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                return Result.healthy();
            } else {
                // You can extract and log additional information from the response if needed
                String responseDetails = extractResponseDetails(response);
                return Result.unhealthy("Received non-successful response: " + response.getStatus() + "\n" + responseDetails);

//                return Result.unhealthy("Received non-successful response: " + response.getStatus());
            }
        } catch (Exception e) {
            return Result.unhealthy("Exception occurred: " + e.getMessage());
        }

    }
    private String extractResponseDetails(Response response) {
        // Extract and return additional information from the response
        StringBuilder details = new StringBuilder();

        // Append response headers
        response.getHeaders().forEach((name, values) ->
                details.append("Header: ").append(name).append(" = ").append(values).append("\n"));

        // Append response entity content
        String entityContent = response.readEntity(String.class);
        details.append("Entity Content: ").append(entityContent).append("\n");

        return details.toString();
    }
}

