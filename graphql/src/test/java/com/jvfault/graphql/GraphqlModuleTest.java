package com.jvfault.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-graphql 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("GraphQL 模块测试")
class GraphqlModuleTest {

    private static final String SDL = ""
            + "type Query {\n"
            + "  greeting(name: String!): String\n"
            + "  numbers: [Int]\n"
            + "}\n"
            + "type Mutation {\n"
            + "  add(a: Int, b: Int): Int\n"
            + "}\n";

    @Test
    @DisplayName("SDL 装配 schema 并执行查询")
    void testQueryExecution() {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", type -> type
                        .dataFetcher("greeting", env -> "Hello, " + env.getArgument("name") + "!")
                        .dataFetcher("numbers", env -> java.util.Arrays.asList(1, 2, 3)))
                .build();

        GraphQLEndpoint endpoint = new GraphQLEndpoint(SDL, wiring);
        Map<String, Object> result = endpoint.execute("{ greeting(name: \"jvfault\") }");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals("Hello, jvfault!", data.get("greeting"));
        assertFalse(result.containsKey("errors"));
    }

    @Test
    @DisplayName("变量传递与 mutation")
    void testVariablesAndMutation() {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", type -> type.dataFetcher("greeting", env -> "hi"))
                .type("Mutation", type -> type
                        .dataFetcher("add", env -> {
                            Integer a = env.getArgument("a");
                            Integer b = env.getArgument("b");
                            return a + b;
                        }))
                .build();

        GraphQLEndpoint endpoint = new GraphQLEndpoint(SDL, wiring);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("x", 19);
        variables.put("y", 23);
        Map<String, Object> result = endpoint.execute(
                "mutation Add($x: Int, $y: Int) { add(a: $x, b: $y) }", variables, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertEquals(42, data.get("add"));
    }

    @Test
    @DisplayName("非法查询返回 errors")
    void testInvalidQuery() {
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", type -> type.dataFetcher("greeting", env -> "hi"))
                .build();
        GraphQLEndpoint endpoint = new GraphQLEndpoint(SDL, wiring);

        Map<String, Object> result = endpoint.execute("{ nonExistentField }");
        assertTrue(result.containsKey("errors"));
    }
}
