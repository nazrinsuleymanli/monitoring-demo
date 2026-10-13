package com.demo;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

/**
 * Tiny app for a metrics demo.
 * - /hello  : increments a custom counter (demo.hello.count)
 * - /slow   : sleeps a random time, so http latency metrics vary
 * - /error  : throws, so error-rate metrics appear
 * All HTTP calls are auto-measured by Micrometer as http.server.requests.
 */
@SpringBootApplication
@RestController
public class MonitoringDemoApplication {

    private final Counter helloCounter;
    private final Random random = new Random();

    // MeterRegistry is injected by Spring — this is where all metrics are registered
    public MonitoringDemoApplication(MeterRegistry registry) {
        this.helloCounter = Counter.builder("demo.hello.count")
                .description("Number of /hello calls")
                .register(registry);
    }

    @GetMapping("/hello")
    public String hello() {
        helloCounter.increment();          // custom metric: counts calls
        return "Hello!\n";
    }

    @GetMapping("/slow")
    public String slow() throws InterruptedException {
        Thread.sleep(random.nextInt(600));  // random latency → interesting percentiles
        return "done\n";
    }

    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("boom");  // 500 → shows up in error-rate metrics
    }

    public static void main(String[] args) {
        SpringApplication.run(MonitoringDemoApplication.class, args);
    }
}