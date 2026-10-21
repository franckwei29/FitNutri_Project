# ADR-01 — Local integration and source preservation

Status: accepted for local implementation by the user's explicit v2.0 execution request; independent team review/integration merge approval PENDING.

Retain React/TS/Vite/MUI, Java21/Boot4.1.1/Spring Security Session and PG16. Reuse Demo presentation with source provenance; replace its store rather than copying its database into a facade. Use one source of business truth in server services and immutable saved snapshots. No remote publication is authorised. New technical receipt table rationale is v2.0 ADR-03; implementation and tests may be prepared on the personal branch, while shared data migrations require schema/backup/conflict gates.
