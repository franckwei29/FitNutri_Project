import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {createHash} from 'node:crypto'
import {dirname,resolve} from 'node:path'
import {fileURLToPath} from 'node:url'
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../../frontend/tests/legacy-demo')
test('all five archived Demo source/test files retain their verified baseline bytes',()=>{const manifest=JSON.parse(readFileSync(resolve(root,'source-manifest.json'),'utf8'));assert.deepEqual(new Set(manifest.files.map(f=>f.path)),new Set(['src/domain.ts','src/seed.ts','src/store.ts','tests/domain.test.ts','tests/service.test.ts']));for(const file of manifest.files){const bytes=readFileSync(resolve(root,file.path));assert.equal(bytes.length,file.bytes,file.path);assert.equal(createHash('sha256').update(bytes).digest('hex'),file.sha256,file.path)}})
