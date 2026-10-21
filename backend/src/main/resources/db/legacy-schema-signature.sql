SELECT jsonb_build_object(
 'columns',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.table_name,x.ordinal_position) FROM
   (SELECT table_name,column_name,udt_name,character_maximum_length,numeric_precision,numeric_scale,datetime_precision,
           is_nullable,column_default,ordinal_position,is_identity,identity_generation
    FROM information_schema.columns WHERE table_schema='public' AND table_name<>'flyway_schema_history') x),
 'constraints',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.table_name,x.name) FROM
   (SELECT c.relname table_name,co.conname name,co.contype type,co.convalidated validated,pg_get_constraintdef(co.oid) definition
    FROM pg_constraint co JOIN pg_class c ON c.oid=co.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='public' AND c.relname<>'flyway_schema_history') x),
 'indexes',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.tablename,x.indexname) FROM
   (SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND tablename<>'flyway_schema_history') x)
)::text;
