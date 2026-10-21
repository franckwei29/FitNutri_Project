import {execFileSync,spawn} from 'node:child_process'
import {randomUUID} from 'node:crypto'
import assert from 'node:assert/strict'
export function syntheticIdentity(prefix='q02'){assert.match(prefix,/^[a-z0-9-]+$/);return {email:`e2e-${prefix}-${randomUUID()}@example.test`,password:'E2eSynthetic123',displayName:`Synthetic E2E ${prefix}`}}
export function assertE2EEnvironment(){const database=process.env.FITNUTRI_E2E_DB_NAME,container=process.env.TEST_DB_DOCKER_CONTAINER,user=process.env.TEST_DB_USERNAME;assert.match(database??'',/^fitnutri_(?:integration_[a-z0-9_]+|it_[0-9a-f]{32})$/,'Explicit isolated FITNUTRI_E2E_DB_NAME is required.');assert.match(container??'',/^fitnutri-integration-[a-z0-9-]+$/,'Explicit owned PostgreSQL test container is required.');assert.equal(user,'fitnutri_test','Use the explicit isolated test role.');return {database,container,user}}
/** SELECT only, fixed SQL and psql-quoted variable; no data cleanup, credentials or real-user querying. */
export function inspectOwnedSyntheticData(email){assert.match(email,/^e2e-[a-z0-9-]+@example\.test$/);const {database,container,user}=assertE2EEnvironment();const sql=`
WITH owner AS (SELECT id FROM app_user WHERE email=:'case_email')
SELECT json_build_object(
 'schemaVersion',(SELECT max(version::integer) FROM flyway_schema_history WHERE success),
 'ownerId',(SELECT id::text FROM owner),
 'foodRecords',(SELECT coalesce(json_agg(json_build_object('id',r.id::text,'clientEntryId',r.client_entry_id::text,'recordDate',r.recorded_date,'quantityGrams',r.quantity_grams,'energyKj',r.energy_kj,'mealType',r.meal_type,'version',r.version,'deleted',r.deleted_at IS NOT NULL) ORDER BY r.id),'[]'::json) FROM food_record r WHERE r.user_id=(SELECT id FROM owner)),
 'exerciseRecords',(SELECT coalesce(json_agg(json_build_object('id',r.id::text,'clientEntryId',r.client_entry_id::text,'recordDate',r.recorded_date,'durationMinutes',r.duration_minutes,'estimatedKj',r.estimated_kj,'estimateStatus',r.estimate_status,'version',r.version,'deleted',r.deleted_at IS NOT NULL) ORDER BY r.id),'[]'::json) FROM exercise_record r WHERE r.user_id=(SELECT id FROM owner)),
 'receipts',(SELECT coalesce(json_agg(json_build_object('submissionId',s.submission_id::text,'kind',s.operation,'requestSha256',s.request_sha256,'records',s.result_json->'records') ORDER BY s.id),'[]'::json) FROM submission_receipt s WHERE s.user_id=(SELECT id FROM owner)),
 'weights',(SELECT coalesce(json_agg(json_build_object('recordDate',w.entry_date,'weightKg',w.weight_kg,'version',w.version) ORDER BY w.entry_date),'[]'::json) FROM weight_entry w WHERE w.user_id=(SELECT id FROM owner)),
 'profile',(SELECT json_build_object('onboardingCompleted',p.onboarding_completed,'qaConsentGranted',p.health_safety_consent,'goalType',p.goal,'dailyTargetKj',p.daily_target_kj) FROM user_profile p WHERE p.user_id=(SELECT id FROM owner))
);`
 const output=execFileSync('docker',['exec','-i',container,'psql','-X','-v','ON_ERROR_STOP=1','-U',user,'-d',database,'-At','-v',`case_email=${email}`],{input:sql,encoding:'utf8',timeout:15000});const state=JSON.parse(output.trim());assert.ok(state.schemaVersion>=8,'Reviewed canonical schema required.');assert.ok(state.ownerId,'The real UI account must exist in the explicitly selected isolated database.');return state
}

/** Holds only the generated test owner's row. Disconnect/COMMIT always releases it. */
export async function acquireSyntheticOwnerLock(email) {
  assert.match(email,/^e2e-[a-z0-9-]+@example\.test$/)
  inspectOwnedSyntheticData(email) // Refuse missing or non-synthetic owners before taking any lock.
  const {database,container,user}=assertE2EEnvironment()
  const child=spawn('docker',['exec','-i',container,'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U',user,'-d',database,'-v',`case_email=${email}`],{stdio:['pipe','pipe','pipe']})
  let released=false,output=''
  const closed=new Promise((resolve,reject)=>{child.once('error',reject);child.once('close',code=>code===0?resolve():reject(Error('The isolated owner lock process failed.')))})
  // Observe a failed process immediately even before the lock-ready marker arrives.
  void closed.catch(()=>{})
  const ready=new Promise((resolve,reject)=>{
    const timer=setTimeout(()=>{child.kill();reject(Error('Timed out acquiring isolated owner lock.'))},15000)
    child.once('error',()=>{clearTimeout(timer);reject(Error('Could not start isolated owner lock.'))})
    child.once('close',()=>{clearTimeout(timer);reject(Error('Owner lock ended before readiness.'))})
    child.stdout.on('data',chunk=>{output+=chunk.toString();const match=output.match(/OWNER_LOCK_READY:(\d+)/);if(match){clearTimeout(timer);resolve(Number(match[1]))}})
    child.stderr.resume() // Never print arbitrary PostgreSQL diagnostics/identity values.
  })
  child.stdin.write("BEGIN;\nSELECT id FROM app_user WHERE email=:'case_email' FOR UPDATE;\nSELECT 'OWNER_LOCK_READY:' || pg_backend_pid();\n")
  const pid=await ready
  return {pid,release:async()=>{if(released)return;released=true;child.stdin.end('COMMIT;\n');const timer=setTimeout(()=>child.kill(),15000);try{await closed}finally{clearTimeout(timer)}}}
}
export function assertOriginalPostIsWaiting(lockPid) {
  assert.ok(Number.isSafeInteger(lockPid)&&lockPid>0)
  const {database,container,user}=assertE2EEnvironment()
  const sql=`SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND ${lockPid} = ANY(pg_blocking_pids(pid));`
  const count=Number(execFileSync('docker',['exec','-i',container,'psql','-XAt','-v','ON_ERROR_STOP=1','-U',user,'-d',database],{input:sql,encoding:'utf8',timeout:15000}).trim())
  assert.equal(count,1,'Exactly one real backend transaction must be waiting on the explicit test-owner lock.')
}
/** Minimal access state of only the generated synthetic owner, separate from private snapshots. */
export function inspectSyntheticAccountAccess(email) {
  assert.match(email,/^e2e-[a-z0-9-]+@example\.test$/)
  const {database,container,user}=assertE2EEnvironment()
  const sql="SELECT json_build_object('id',id::text,'role',role,'enabled',enabled,'version',version) FROM app_user WHERE email=:'case_email';"
  const value=execFileSync('docker',['exec','-i',container,'psql','-XAt','-v','ON_ERROR_STOP=1','-U',user,'-d',database,'-v',`case_email=${email}`],{input:sql,encoding:'utf8',timeout:15000})
  assert.ok(value.trim(),'Generated test owner must exist.');return JSON.parse(value.trim())
}
