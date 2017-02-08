/*
 *                                            Bosch SI Example Code License
 *                                              Version 1.0, January 2016
 *
 * Copyright 2017 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH
 * BOSCH SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT
 * LIMITATION FOR INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF
 * THE GERMAN PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN
 * UNAFFECTED BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF
 * LIABILITY ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S
 * EMPLOYEES, REPRESENTATIVES AND ORGANS.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.ExampleUser;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.client.ThingsClientFactory;
import com.bosch.cr.integration.client.configuration.CredentialsAuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.MessageSerializerConfiguration;
import com.bosch.cr.integration.client.configuration.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.TwinConfiguration;
import com.bosch.cr.integration.client.messages.MessageSerializerRegistry;
import com.bosch.cr.integration.client.messages.MessageSerializers;
import com.bosch.cr.integration.client.messaging.HubMessagingProviderConfiguration;
import com.bosch.cr.integration.client.messaging.MessagingProviders;
import com.bosch.cr.integration.client.messaging.ThingsWsMessagingProviderConfiguration;
import com.bosch.cr.integration.things.ThingHandle;
import com.bosch.cr.integration.things.ThingIntegration;
import com.bosch.cr.integration.twin.Twin;

/**
 * Instantiates an {@link IntegrationClient} and connects to the Bosch IoT Things service. It also initializes
 * {@link ThingIntegration} and {@link ThingHandle} instances for reuse in tests that extend this base class.
 */
public abstract class ExamplesBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExamplesBase.class);

    private static final String SOLUTION_ID = "<your-solution-id>";
    protected static final String CLIENT_ID = SOLUTION_ID + ":example";
    private static final String SOLUTION_API_TOKEN_THINGS = "<your-solution-api-token-registered-in-things-service>";
    private static final String SOLUTION_API_TOKEN_HUB = "<your-solution-api-token-registered-in-hub-service>";
    private static final String SOLUTION_DEFAULT_NAMESPACE = "com.your.namespace";

    private static final String USER_NAME = "<your-user-name";
    private static final String PASSWORD = "<your-password>";

    private static final URL KEYSTORE_LOCATION = ExamplesBase.class.getResource("/CRClient.jks");
    private static final String ALIAS = "<your-key-alias>";
    private static final String KEYSTORE_PASSWORD = "<your-keystore-password";
    private static final String ALIAS_PASSWORD = "<your-alias-password>";

    // optionally configure a proxy server
    // public static final String PROXY_HOST = "proxy.server.com";
    // public static final int PROXY_PORT = 8080;

    protected final IntegrationClient integrationClient;
    protected final Twin twin;
    protected final String myThingId;
    protected final ThingHandle myThing;

    /**
     * Constructor.
     */
    public ExamplesBase() {
        // Build a credential authentication configuration if you want to directly connect to the IoT Things service
        // via its websocket channel
        final CredentialsAuthenticationConfiguration credentialsAuthenticationConfiguration =
                CredentialsAuthenticationConfiguration
                        .newBuilder()
                        .username(USER_NAME)
                        .password(PASSWORD)
                        .build();

        final ThingsWsMessagingProviderConfiguration thingsWsMessagingProviderConfiguration = MessagingProviders
                .thingsWebsocketProviderBuilder()
                .authenticationConfiguration(credentialsAuthenticationConfiguration)
                .build();

        // or alternatively, build a key-based authentication configuration for communicating with IoT Things service
        // over IoT Hub
        final PublicKeyAuthenticationConfiguration publicKeyAuthenticationConfiguration =
                PublicKeyAuthenticationConfiguration
                        .newBuilder()
                        .clientId(CLIENT_ID)
                        .keyStoreLocation(KEYSTORE_LOCATION)
                        .keyStorePassword(KEYSTORE_PASSWORD)
                        .alias(ALIAS)
                        .aliasPassword(ALIAS_PASSWORD)
                        .build();

        final HubMessagingProviderConfiguration hubMessagingProviderConfiguration = MessagingProviders
                .hubProviderBuilder(SOLUTION_API_TOKEN_HUB)
                .authenticationConfiguration(publicKeyAuthenticationConfiguration)
                .build();

        // Optionally configure a proxy server
        /*final ProxyConfiguration proxyConfiguration = ProxyConfiguration.newBuilder()
                .proxyHost(PROXY_HOST)
                .proxyPort(PROXY_PORT)
                .build();*/


        final TwinConfiguration twinConfiguration = ThingsClientFactory.twinConfigurationBuilder()
                .apiToken(SOLUTION_API_TOKEN_THINGS)
                .defaultNamespace(SOLUTION_DEFAULT_NAMESPACE)
                .providerConfiguration(thingsWsMessagingProviderConfiguration /* or hubMessagingProviderConfiguration*/)
                //.proxyConfiguration(proxyConfiguration)
                .build();

        LOGGER.info("Creating integration client ...");

        // Create a new integration client object to start interacting with IoT Things service
        integrationClient = ThingsClientFactory.newInstance(twinConfiguration);

        // Create a new twin client for managing things
        twin = integrationClient.twin();

        try {
            // and start consuming events
            twin.startConsumption().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Error creating Things Client.", e);
        }

        this.myThingId = ":myThing_" + UUID.randomUUID().toString();
        this.myThing = twin.forId(myThingId);
    }

    /**
     * Sets up a serializer/deserializer for the {@link ExampleUser} model class which uses JAXB in order to serialize
     * and deserialize messages which should directly be mapped to this type.
     *
     * @param serializerConfiguration the initial MessageSerializerConfiguration to adjust.
     */
    private void setupCustomMessageSerializer(final MessageSerializerConfiguration serializerConfiguration) {
        final JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(ExampleUser.class);
        } catch (final JAXBException e) {
            throw new RuntimeException("Could not setup JAXBContext", e);
        }

        final MessageSerializerRegistry serializerRegistry = serializerConfiguration.getMessageSerializerRegistry();

        serializerRegistry.registerMessageSerializer(
                MessageSerializers.of(ExampleUser.USER_CUSTOM_CONTENT_TYPE, ExampleUser.class, "*", //
                        (exampleUser, charset) ->
                        {
                            try {
                                final Marshaller marshaller = jaxbContext.createMarshaller();
                                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                                marshaller.marshal(exampleUser, os);
                                return ByteBuffer.wrap(os.toByteArray());
                            } catch (final JAXBException e) {
                                throw new RuntimeException("Could not serialize", e);
                            }
                        }, (byteBuffer, charset) ->
                        {
                            try {
                                final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                                final ByteArrayInputStream is = new ByteArrayInputStream(byteBuffer.array());
                                return (ExampleUser) jaxbUnmarshaller.unmarshal(is);
                            } catch (final JAXBException e) {
                                throw new RuntimeException("Could not deserialize", e);
                            }
                        }));
    }

    /**
     * Destroys the client and waits for its graceful shutdown.
     */
    public void terminate() {
        integrationClient.destroy();
    }
}
