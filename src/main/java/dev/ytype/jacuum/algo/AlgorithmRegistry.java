package dev.ytype.jacuum.algo;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AlgorithmRegistry {

    private final ListableBeanFactory beanFactory;
    private final Map<String, AlgorithmEntry> algorithms;

    public AlgorithmRegistry(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.algorithms = discoverAlgorithms(beanFactory);
    }

    public List<AlgorithmDescriptor> descriptors() {
        return algorithms.values().stream()
                .map(AlgorithmEntry::descriptor)
                .sorted(Comparator.comparing(AlgorithmDescriptor::id))
                .toList();
    }

    public RobotAlgorithm create(String id) {
        AlgorithmEntry entry = algorithms.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown algorithm id: " + id);
        }
        return beanFactory.getBean(entry.beanName(), RobotAlgorithm.class);
    }

    private static Map<String, AlgorithmEntry> discoverAlgorithms(ListableBeanFactory beanFactory) {
        Map<String, AlgorithmEntry> discovered = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanNamesForType(RobotAlgorithm.class, true, false)) {
            RobotAlgo robotAlgo = beanFactory.findAnnotationOnBean(beanName, RobotAlgo.class);
            if (robotAlgo == null) {
                continue;
            }
            AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
                    robotAlgo.id(),
                    robotAlgo.name(),
                    robotAlgo.description());
            AlgorithmEntry previous = discovered.put(robotAlgo.id(), new AlgorithmEntry(beanName, descriptor));
            if (previous != null) {
                throw new IllegalStateException("Duplicate algorithm id: " + robotAlgo.id());
            }
        }
        return Map.copyOf(discovered);
    }

    private record AlgorithmEntry(String beanName, AlgorithmDescriptor descriptor) {
        private AlgorithmEntry {
            Objects.requireNonNull(beanName, "beanName");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }
}
