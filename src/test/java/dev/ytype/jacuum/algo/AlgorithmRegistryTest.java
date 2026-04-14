package dev.ytype.jacuum.algo;

import dev.ytype.jacuum.VacuumCleanerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = VacuumCleanerApplication.class)
class AlgorithmRegistryTest {

    @Autowired
    private AlgorithmRegistry registry;

    @Test
    void robotAlgoCarriesPrototypeScopeMetadata() {
        Scope scope = AnnotatedElementUtils.findMergedAnnotation(RobotAlgo.class, Scope.class);

        assertThat(scope).isNotNull();
        assertThat(scope.value()).isEqualTo(org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE);
    }

    @Test
    void sampleAlgorithmsDoNotDeclareExplicitScope() {
        assertThat(RandomWalkAlgorithm.class.isAnnotationPresent(Scope.class)).isFalse();
        assertThat(AlwaysLeftAlgorithm.class.isAnnotationPresent(Scope.class)).isFalse();
        assertThat(WallFollowerAlgorithm.class.isAnnotationPresent(Scope.class)).isFalse();
    }

    @Test
    void registryExposesAllSampleAlgorithmsAndFreshInstances() {
        Set<String> ids = registry.descriptors().stream()
                .map(AlgorithmDescriptor::id)
                .collect(Collectors.toSet());

        assertThat(ids).containsExactlyInAnyOrder("random-walk", "always-left", "wall-follower");

        Map<String, RobotAlgorithm> firstInstances = registry.descriptors().stream()
                .collect(Collectors.toMap(
                        AlgorithmDescriptor::id,
                        descriptor -> registry.create(descriptor.id())));

        Map<String, RobotAlgorithm> secondInstances = registry.descriptors().stream()
                .collect(Collectors.toMap(
                        AlgorithmDescriptor::id,
                        descriptor -> registry.create(descriptor.id())));

        assertThat(firstInstances).hasSameSizeAs(secondInstances);
        firstInstances.forEach((id, firstInstance) ->
                assertThat(firstInstance).isNotSameAs(secondInstances.get(id)));
    }
}
