package com.jacuum.api;

import com.jacuum.algo.AlgoRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/algos")
public class AlgoController {

    private final AlgoRegistry registry;

    public AlgoController(AlgoRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<AlgoInfo> list() {
        return registry.listAlgos().entrySet().stream()
                .map(e -> new AlgoInfo(e.getKey(), e.getValue()))
                .toList();
    }

    public record AlgoInfo(String id, String name) {}
}
