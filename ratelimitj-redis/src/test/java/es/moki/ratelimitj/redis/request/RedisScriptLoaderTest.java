package es.moki.ratelimitj.redis.request;


import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.lettuce.core.ScriptOutputType.VALUE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedisScriptLoaderTest {

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;

    @BeforeAll
    static void beforeAll() {
        client = RedisClient.create("redis://localhost");
        connection = client.connect();
    }

    @AfterAll
    @SuppressWarnings("FutureReturnValueIgnored")
    static void afterAll() {
        connection.sync().flushdb();
        client.shutdownAsync();
    }

    @Test
    @DisplayName("should load rate limit lua script into Redis")
    void shouldLoadScript() {
        RedisScriptLoader scriptLoader = new RedisScriptLoader(connection, "hello-world.lua");
        connection.sync().scriptFlush();

        String sha = scriptLoader.storedScript().block(Duration.ofSeconds(5)).getSha();
        assertThat(sha).isNotEmpty();
        assertThat(connection.sync().scriptExists(sha)).containsOnly(true);
    }

    @Test
    @DisplayName("should cache loaded sha")
    void shouldCache() {
        RedisScriptLoader scriptLoader = new RedisScriptLoader(connection, "hello-world.lua");

        assertThat(scriptLoader.storedScript().block(Duration.ofSeconds(5)).getSha()).isNotEmpty();

        connection.sync().scriptFlush();

        assertThat(scriptLoader.storedScript().block(Duration.ofSeconds(5)).getSha()).isNotEmpty();
    }

    @Test
    @DisplayName("should eagerly load rate limit lua script into Redis")
    void shouldEagerlyLoadScript() {
        RedisScriptLoader scriptLoader = new RedisScriptLoader(connection, "hello-world.lua", true);
        connection.sync().scriptFlush();

        String sha = scriptLoader.storedScript().block(Duration.ofSeconds(5)).getSha();
        assertThat(sha).isNotEmpty();

        assertThat(connection.sync().scriptExists(sha)).containsOnly(false);
    }

    @Test
    @DisplayName("should fail if script not found")
    void shouldFailedIfScriptNotFound() {

        Throwable exception = assertThrows(IllegalArgumentException.class,
                () -> new RedisScriptLoader(connection, "not-found-script.lua", true));
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("should fail if script not found")
    void shouldExecuteScript() {

        RedisScriptLoader scriptLoader = new RedisScriptLoader(connection, "hello-world.lua", true);
        String sha = scriptLoader.storedScript().block(Duration.ofSeconds(5)).getSha();

        Object result = connection.sync().evalsha(sha, VALUE);
        assertThat((String) result).isEqualTo("hello world");
    }
}