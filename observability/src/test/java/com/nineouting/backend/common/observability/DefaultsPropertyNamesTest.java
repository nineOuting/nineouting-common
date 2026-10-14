package com.nineouting.backend.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring은 이름이 틀린 설정을 경고 없이 무시한다. defaults.yml의 모든 키가
 * 실제 Spring Boot 설정(configuration metadata)에 있는지 확인한다.
 */
class DefaultsPropertyNamesTest {

    private static final String DEFAULTS = "observability/defaults.yml";
    private static final List<String> METADATA_FILES = List.of(
            "META-INF/spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void defaults_yml의_키는_모두_Spring_Boot에_존재한다() throws IOException {
        Set<String> known = knownPropertyNames();

        List<String> unknown = defaultsKeys().stream()
                .filter(key -> !known.contains(key))
                .toList();

        assertThat(unknown).as("Spring Boot에 없는 설정 이름").isEmpty();
    }

    private Set<String> knownPropertyNames() throws IOException {
        Set<String> names = new HashSet<>();
        ClassLoader classLoader = getClass().getClassLoader();
        for (String file : METADATA_FILES) {
            for (URL url : Collections.list(classLoader.getResources(file))) {
                try (InputStream in = url.openStream()) {
                    JsonNode properties = jsonMapper.readTree(in).path("properties");
                    properties.forEach(property -> names.add(property.path("name").asString()));
                }
            }
        }
        return names;
    }

    private List<String> defaultsKeys() throws IOException {
        List<String> keys = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULTS)) {
            assertThat(in).as(DEFAULTS).isNotNull();
            for (Object document : new Yaml().loadAll(in)) {
                flatten("", (Map<?, ?>) document, keys);
            }
        }
        return keys;
    }

    private static void flatten(String prefix, Map<?, ?> map, List<String> keys) {
        map.forEach((key, value) -> {
            String path = prefix.isEmpty() ? key.toString() : prefix + "." + key;
            if (value instanceof Map<?, ?> child) {
                flatten(path, child, keys);
            } else {
                keys.add(path);
            }
        });
    }
}
