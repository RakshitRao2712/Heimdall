package com.scraper.leadscraper.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Durable storage for the existing single-instance workspace stores. */
@Component
@ConditionalOnProperty(name = "heimdall.storage.postgres.enabled", havingValue = "true")
public class PostgresDocuments {
    private final String url;
    private final Properties credentials = new Properties();

    public PostgresDocuments(@Value("${HEIMDALL_JDBC_URL}") String url,
            @Value("${HEIMDALL_DB_USER}") String user, @Value("${HEIMDALL_DB_PASSWORD}") String password) {
        if (!url.startsWith("jdbc:postgresql://") || url.contains("?") || user.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Configure the PostgreSQL JDBC URL without query parameters, user and password.");
        }
        this.url = url;
        credentials.setProperty("user", user);
        credentials.setProperty("password", password);
        credentials.setProperty("sslmode", "verify-full");
        credentials.setProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        credentials.setProperty("connectTimeout", "10");
        credentials.setProperty("socketTimeout", "20");
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS heimdall_documents (
                    namespace text NOT NULL,
                    document_id text NOT NULL,
                    payload bytea NOT NULL,
                    PRIMARY KEY (namespace, document_id)
                )
                """);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize Heimdall PostgreSQL storage (SQL state: "
                    + e.getSQLState() + ").");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, credentials);
    }

    public byte[] read(String namespace, String id) throws IOException {
        try (var connection = connect(); var statement = connection.prepareStatement(
                "SELECT payload FROM heimdall_documents WHERE namespace = ? AND document_id = ?")) {
            statement.setString(1, namespace);
            statement.setString(2, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? rows.getBytes(1) : null;
            }
        } catch (SQLException e) {
            throw new IOException("Could not read workspace data.");
        }
    }

    public List<Document> list(String namespace) throws IOException {
        var documents = new ArrayList<Document>();
        try (var connection = connect(); var statement = connection.prepareStatement(
                "SELECT document_id, payload FROM heimdall_documents WHERE namespace = ?")) {
            statement.setString(1, namespace);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) documents.add(new Document(rows.getString(1), rows.getBytes(2)));
            }
            return documents;
        } catch (SQLException e) {
            throw new IOException("Could not list workspace data.");
        }
    }

    public void write(String namespace, String id, byte[] payload) throws IOException {
        try (var connection = connect(); var statement = connection.prepareStatement("""
                INSERT INTO heimdall_documents (namespace, document_id, payload) VALUES (?, ?, ?)
                ON CONFLICT (namespace, document_id) DO UPDATE SET payload = EXCLUDED.payload
                """)) {
            statement.setString(1, namespace);
            statement.setString(2, id);
            statement.setBytes(3, payload);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Could not save workspace data.");
        }
    }

    public Properties readProperties(String namespace, String id) throws IOException {
        var properties = new Properties();
        byte[] payload = read(namespace, id);
        if (payload != null) properties.load(new ByteArrayInputStream(payload));
        return properties;
    }

    public void writeProperties(String namespace, String id, Properties properties) throws IOException {
        var output = new ByteArrayOutputStream();
        properties.store(output, null);
        write(namespace, id, output.toByteArray());
    }

    public record Document(String id, byte[] payload) {}
}
