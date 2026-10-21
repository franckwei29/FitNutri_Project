# FitNutri Assistant

**ELEC5619 Group:** Lab_practical_05_Group_6

An AI-assisted fitness and nutrition planner for adults living in Australia. The current application includes grouped food/dish records, Quick exercise and Workouts, history, summaries and v3.2 save feedback.

## Start on your own computer

Follow the [fresh-checkout setup guide](docs/getting-started.md). Use JDK21, Node24 and PostgreSQL16. Copy `.env.example` to a private `.env`, select your own database and password, export the configuration, initialize V1–V13 on a new empty database, and import the verified food/exercise catalogues before starting normal development.

The default development URL is **http://localhost:5173**, with the backend at **127.0.0.1:8080**. No existing teammate database, account, personal Docker container or review server is required. Optional AI features need your own backend-only key/model configuration and FFmpeg for transcription.

```sh
# After completing configuration, schema initialization and imports in the guide:
./scripts/dev.sh backend
# In another terminal at the repository root:
./scripts/dev.sh frontend
```

## Tests and reference data

Use the guide's separate testing configuration and `.env.test.example`; do not run database tests against your application database. `scripts/check.sh` provides static, frontend, data and backend gates. Browser tests additionally require their documented isolated stack. The [handover validation report](docs/verification/team-handover-validation.md) records observed results and limitations rather than assuming all installations passed.

Normalized reference data and immutable source manifests are tracked in `backend/src/main/resources/data`. Preparation scripts download and verify official data. The small pinned [Compendium age-evidence snapshot](scripts/data/evidence/README.md) preserves the exact original source hash when the live page changes. Private caches, dependencies and generated build artifacts are recreated locally.

## Architecture and project evidence

- [API contract and examples](docs/api/openapi-mvp.yaml)
- [Development and verification details](docs/development.md)
- [Current UI acceptance checklist](docs/verification/ux-v3-2-uat.md)
- [Retained domain/security specification](docs/implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md)
- [v3.2 presentation requirements](docs/implementation/FitNutri_Centered_Feedback_and_Action_Style_Execution_Plan_v3_2.md)

Historical reports retain their original commit IDs, ports, test counts and pending human sign-offs. Ports15173/18080/18082 and the old8787/V8 runtime describe previous local reviews, not prerequisites for new installations. Developer tests, live-provider acceptance, peer review and human UAT are distinct.

## Project Overview

FitNutri Assistant is a responsive web-based Fitness & Nutrition Planner for adults living in Australia who want to lose body fat, gain muscle, or maintain a healthy lifestyle. It is designed around the food environment, nutrition references, and health guidance relevant to everyday life in Australia. Users can record food intake and physical activity, review daily and historical trends, and inspect factual recorded summaries, with optional general non-medical AI Q&A on desktop and mobile browsers.

## Real-World Problem

The project addresses two connected problems. First, food and exercise tracking can be repetitive and time-consuming, making consistent self-monitoring difficult. Australian research has identified time-consuming entry and concerns about information accuracy and credibility as barriers to using diet-related applications. Second, nutrition values produced directly by general-purpose AI can be difficult to verify because they may rely on unstated assumptions about portions, ingredients, and cooking methods.

FitNutri Assistant uses AI to simplify input while treating recognised Australian food datasets as the source of nutrition values. Users confirm uncertain details, and the backend maps confirmed foods to Food Standards Australia New Zealand data before calculating nutrients. This makes records faster to enter and their calculations more transparent and traceable.

## Main Features

- Secure accounts, role-based access, profiles, and goal-oriented onboarding.
- Simple manual recording of food intake and physical activity.
- Implemented photo candidates and actual-audio transcription/extraction flows, always followed by shared Review and explicit Confirm; live-provider/microphone acceptance pending.
- Server-side nutrition calculation from verified versioned AUSNUT2023 references; older unverified legacy references/records remain traceable.
- A responsive dashboard for daily records, energy information, and historical trends.
- Factual recorded summaries, optional consent-restricted general Q&A, and minimal versioned administrator account management.

## High-Level Goals

1. Reduce the time and effort required to record daily food intake and exercise.
2. Produce nutrition calculations that are linked to identifiable Australian food records rather than unexplained AI-generated values.
3. Deliver a secure, responsive application that helps users understand their habits through summaries and trends.

## Team Roles and Responsibilities

Primary roles define accountability rather than exclusive ownership. Each member leads one area, but all team members are expected to contribute to requirements, feature implementation, integration, testing, documentation, and final delivery.

| Team Member | Primary Role | Key Responsibilities |
|---|---|---|
| **Yuankang Wei** | Product Owner | Defines product vision and user needs, prioritises the backlog, controls scope, and accepts completed product outcomes. |
| **Shu-Chi Lee** | Frontend Lead | Leads responsive frontend architecture, interface implementation, usability, accessibility, and API integration. |
| **Haoran Liao** | Scrum Master and Backend Lead | Facilitates Scrum and Jira; leads backend, API, database, and security work; and owns food-data and AI-model technology selection. |
| **Aowei Lin** | Testing and QA Lead | Leads test strategy, test cases, defect tracking, system and acceptance testing, and release-quality verification. |

## RACI Matrix

The following RACI matrix clarifies how responsibility is shared across the project's major workstreams. It prevents the primary roles above from being interpreted as isolated work: the named lead is accountable for the outcome, while other members still participate in cross-functional delivery.

- **R - Responsible:** performs or contributes directly to the work. A workstream may have multiple responsible members.
- **A - Accountable:** has final ownership of the outcome and approves key decisions. Each workstream has one accountable lead.
- **C - Consulted:** provides relevant knowledge or feedback before a decision or action is completed.
- **I - Informed:** is kept updated on progress or decisions. The current high-level allocation mainly uses R, A, and C because all four members are active contributors.

| Workstream | Yuankang Wei | Shu-Chi Lee | Haoran Liao | Aowei Lin |
|---|:---:|:---:|:---:|:---:|
| Product vision, user requirements, and backlog | A/R | R | R | R |
| UI/UX and responsive frontend development | R | A/R | R | R |
| Backend, APIs, and database development | R | R | A/R | R |
| Food-data source and AI-model technology selection | C | C | A/R | C |
| Testing, quality assurance, and defect management | R | R | R | A/R |
| Scrum coordination and Jira management | C | C | A/R | C |
| System integration, product acceptance, and release readiness | A/R | R | R | R |

## References

- Fewings, A., Vandelanotte, C., Irwin, C., Ting, C., Williams, E., & Khalesi, S. (2022). *The use and acceptability of diet-related apps and websites in Australia: Cross-sectional study*. Digital Health, 8. [https://doi.org/10.1177/20552076221139091](https://doi.org/10.1177/20552076221139091)
- Food Standards Australia New Zealand. *Australian Food Composition Database - Release 3* and *AUSNUT 2023 Data Files*. [AFCD](https://www.foodstandards.gov.au/science-data/food-nutrient-databases/afcd/about-afcd) | [AUSNUT 2023](https://www.foodstandards.gov.au/science-data/food-nutrient-databases/ausnut/data-files)
- Fridolfsson, J., Sjöberg, E., Thiwång, M., & Pettersson, S. (2025). *Performance Evaluation of 3 Large Language Models for Nutritional Content Estimation from Food Images*. Current Developments in Nutrition, 9(10), 107556. [https://doi.org/10.1016/j.cdnut.2025.107556](https://doi.org/10.1016/j.cdnut.2025.107556)
