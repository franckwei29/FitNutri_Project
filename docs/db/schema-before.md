# Observed legacy schema — D00

Observed in dedicated PostgreSQL16.15 database fitnutri_integration_baseline at loopback55432; generated from original117348a entities and inspected using read-only PostgreSQL catalog queries, not guessed from annotations. Machine inventory: schema-before.json. This does not certify any unknown shared/local existing database.

Six tables: app_user, user_profile, weight_entry, food_reference, food_record, exercise_record. User IDs bigint identity; profile shares user PK/FK. food_reference PK is public_food_key varchar(255), **actual legacy nutrient columns carbsg/proteing/fatg/fiberg/sugarg**. Weight/date uniqueness missing. Record dates timestamptz, numeric values double precision, exercise duration integer, source enum PHOTO (target IMAGE must explicitly map in later migration).

No Flyway history. Four user FKs; no FK referencing food_reference. app_user unique email case-sensitive. Counts: accounts/profiles/weights/records0; food_reference1588 from original AFCD loader in isolated test context. This is not verified AUSNUT data. No original user data read or changed.

Shared database baseline adoption remains prohibited until its own schema inventory, conflicts, explicit legacy timezone and verified backup/restore are reviewed. Original data with unknown relationships must stop adoption.
