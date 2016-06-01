/*
 * Copyright 2016, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.cassandra;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.datastax.driver.core.exceptions.SyntaxError;


public class SchemaManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaManager.class);

    private static final String KEYSPACE_PLACEHOLDER = "$KEYSPACE$";
    private static final String REPLICATION_FACTOR_PLACEHOLDER = "$REPLICATION_FACTOR$";

    public static final int DEFAULT_REPLICATION_FACTOR = 1;

    private String m_keyspace;
    private Cluster m_cluster;
    private Session m_session;
    private int m_replicationFactor = DEFAULT_REPLICATION_FACTOR;

    @Inject
    public SchemaManager(@Named("cassandra.keyspace") String keyspace, @Named("cassandra.host") String host, @Named("cassandra.port") int port,
            @Named("cassandra.username") String username, @Named("cassandra.password") String password, @Named("cassandra.ssl") boolean ssl) {
        m_keyspace = keyspace;

        Builder builder = Cluster.builder()
                .withSSL()
                .withPort(port)
                .addContactPoints(host.split(","));
        if (username != null && password != null) {
            LOG.info("Using username: {} and password: XXXXXXXX", username);
            builder.withCredentials(username, password);
        }

        if (ssl) {
            LOG.info("Using SSL.");
            builder.withSSL();
        }
        m_cluster= builder.build();
        m_session = m_cluster.connect();
    }

    public void create(Schema schema) throws IOException {
        create(schema, true);
    }

    public void create(Schema schema, boolean ifNotExists) throws IOException {
        create(schema, ifNotExists, false);
    }

    public void create(Schema schema, boolean ifNotExists, boolean printOnly) throws IOException {

        checkNotNull(schema, "schema argument");
        InputStream stream = checkNotNull(schema.getInputStream(), "schema input stream");

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        String line, scrubbed;
        StringBuilder statement = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            scrubbed = scrub(line);
            statement.append(scrubbed);

            if (scrubbed.endsWith(";")) {
                // Substitute the actual keyspace name for any KEYSPACE macros.
                String queryString = statement.toString().replace(KEYSPACE_PLACEHOLDER, m_keyspace);
                queryString = queryString.replace(REPLICATION_FACTOR_PLACEHOLDER, Integer.toString(m_replicationFactor));

                if (printOnly) {
                    System.err.println(queryString);
                } else {
                    try {
                        m_session.execute(queryString);
                    }
                    catch (AlreadyExistsException e) {
                        if (ifNotExists) {
                            System.err.printf("%s; Nothing here to do%n", e.getLocalizedMessage());
                        }
                        else {
                            throw e;
                        }
                    }
                    catch (SyntaxError e) {
                        System.out.printf("ERROR: %s (query: \"%s\").%n", e.getLocalizedMessage(), queryString);
                        throw e;
                    }
                }
                statement = new StringBuilder();
            }
        }

    }

    private static String scrub(String line) {
        return line.replace("\\s+", "").replace("//.*$", "").replace(";.*$", ";");
    }

    @Override
    public void close() {
        m_cluster.close();
    }

    public int getReplicationFactor() {
        return m_replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        m_replicationFactor = replicationFactor;
    }
}
