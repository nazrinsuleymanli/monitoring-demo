# monitoring-demo

A fully local observability stack: a Spring Boot app whose metrics are scraped by
**Prometheus** and visualized in **Grafana** — the real metrics pipeline you'd run
in production, minus the cloud.

```
Spring Boot app ──(/actuator/prometheus)──► Prometheus ──► Grafana dashboards
   (Micrometer)         exposes metrics        scrapes        visualizes
```

## What each piece does

- **Micrometer** — the metrics library inside the app. Spring auto-measures HTTP
  requests (`http.server.requests`); the app also defines a custom counter
  (`demo.hello.count`).
- **Actuator** — exposes those metrics at `/actuator/prometheus` in Prometheus format.
- **Prometheus** — a time-series database that *pulls* (scrapes) that endpoint every
  5s and stores the numbers over time.
- **Grafana** — queries Prometheus (with PromQL) and draws dashboards.

## Run it

```bash
cd monitoring-demo
docker compose up --build        # builds the app image, starts all 3 containers
```

Wait ~30s, then open:

| URL | What |
|-----|------|
| http://localhost:8080/hello | the app (also /slow and /error) |
| http://localhost:8080/actuator/prometheus | raw metrics the app exposes |
| http://localhost:9090 | Prometheus UI |
| http://localhost:3000 | Grafana (login: `admin` / `admin`) |

## Generate some traffic

So the dashboards have data to show. In PowerShell:

```powershell
for ($i=1; $i -le 200; $i++) { curl.exe -s localhost:8080/hello > $null }
for ($i=1; $i -le 50;  $i++) { curl.exe -s localhost:8080/slow  > $null }
for ($i=1; $i -le 20;  $i++) { curl.exe -s localhost:8080/error > $null }
```

## See it in Grafana

1. Open http://localhost:3000, log in `admin` / `admin`.
2. The **Prometheus** datasource is already configured (provisioned automatically).
3. Left menu → **Dashboards → New → New dashboard → Add visualization** → pick
   **Prometheus**.
4. In the query box, paste one of these **PromQL** queries:

| Goal | PromQL |
|------|--------|
| Total /hello calls | `demo_hello_count_total` |
| Request rate (per sec) | `rate(http_server_requests_seconds_count[1m])` |
| Avg latency | `rate(http_server_requests_seconds_sum[1m]) / rate(http_server_requests_seconds_count[1m])` |
| 95th percentile latency | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le))` |
| Error rate (5xx) | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))` |
| JVM heap used | `jvm_memory_used_bytes{area="heap"}` |

5. Run the traffic commands again and watch the graphs move.

## Check Prometheus is scraping

http://localhost:9090 → **Status → Targets** → `spring-app` should be **UP**.
Or run a query in the Prometheus UI, e.g. `demo_hello_count_total`.

## Tear down

```bash
docker compose down
```

## Optional: add Sentry (error tracking)

Grafana/Prometheus answer *"how is the system performing?"* (metrics). **Sentry**
answers *"what exception broke, where, and how often?"* — it's a hosted service, so
it needs a free account rather than a local container.

1. Create a free project at sentry.io → copy the **DSN**.
2. Add the dependency to `pom.xml`:
   ```xml
   <dependency>
     <groupId>io.sentry</groupId>
     <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
     <version>7.14.0</version>
   </dependency>
   ```
3. Add to `application.yml`:
   ```yaml
   sentry:
     dsn: https://your-dsn-here@sentry.io/123
     traces-sample-rate: 1.0
   ```
4. Hit `/error` a few times → the `RuntimeException` appears in your Sentry
   dashboard with the full stack trace and context.

## Stack

Spring Boot 3.4 · Micrometer · Prometheus · Grafana · Docker Compose