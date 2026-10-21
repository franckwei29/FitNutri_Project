# FitNutri frontend

React and TypeScript frontend for the Spring Boot API. Follow the [fresh-checkout guide](../docs/getting-started.md) for database initialization, environment configuration, catalogue imports and startup.

From the repository root, install locked dependencies with `npm --prefix frontend ci`, start development with `npm --prefix frontend run dev`, and build with `npm --prefix frontend run build`. The development URL is http://localhost:5173 and /api is proxied to port 8080.

Run `test:unit`, `test:component` and `test:contract` through npm for the corresponding suites. Browser tests require the separate environment described in tests/e2e/README.md.
