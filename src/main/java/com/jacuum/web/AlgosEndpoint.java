package com.jacuum.web;

import com.jacuum.algo.Algorithms;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public final class AlgosEndpoint {

    private final Algorithms algorithms;

    public AlgosEndpoint(final Algorithms algorithms) {
        this.algorithms = algorithms;
    }

    @GetMapping("/algos")
    public List<String> algos() {
        return this.algorithms.names();
    }

    @GetMapping("/avatars")
    public List<String> avatars() {
        return List.of("🤖", "🦾", "👾", "🚀", "🛸", "🦄", "🐢", "🦊", "🐱", "🐸");
    }
}
