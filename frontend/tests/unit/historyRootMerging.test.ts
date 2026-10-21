import {describe,it,expect} from 'vitest'
import {mergeRootPages} from '../../src/features/history/mergeRootPages'
const root=(kind:string,id:string,version=0)=>({id,version,input:{kind},displayName:`Stored ${kind} ${id} v${version}`})
describe('UX-Q03 typed root pagination under concurrent changes',()=>{
 it('returns no invented rows for empty pages and preserves one-page order',()=>{expect(mergeRootPages([])).toEqual([]);expect(mergeRootPages([{results:[]}])).toEqual([]);const rows=[root('DISH','1'),root('SINGLE','2')];expect(mergeRootPages([{results:rows}])).toEqual(rows)})
 it('deduplicates an offset-shifted root appearing on two pages while keeping the first-seen order',()=>{const a=root('DISH','1'),b=root('SINGLE','2'),c=root('SINGLE','3');expect(mergeRootPages([{results:[a,b]},{results:[b,c]}])).toEqual([a,b,c])})
 it('uses the highest saved version without moving the root or letting an older page overwrite it',()=>{const old=root('WORKOUT','1'),current=root('WORKOUT','1',2),other=root('QUICK','2');expect(mergeRootPages([{results:[old,other]},{results:[current]}])).toEqual([current,other]);expect(mergeRootPages([{results:[current,other]},{results:[old]}])).toEqual([current,other])})
 it('never merges distinct kinds with a colliding numeric database ID',()=>{const rows=['SINGLE','DISH','QUICK','WORKOUT'].map(kind=>root(kind,'9'));expect(mergeRootPages([{results:rows},{results:rows}])).toEqual(rows)})
 it('retains distinct identifiers above the JavaScript safe integer boundary as exact strings',()=>{const rows=[root('SINGLE','9007199254740992'),root('SINGLE','9007199254740993')];expect(mergeRootPages([{results:rows}])).toEqual(rows)})
 it('does not mutate immutable server page arrays or snapshot objects',()=>{const a=Object.freeze(root('DISH','1')),b=Object.freeze(root('DISH','1',1)),pages=Object.freeze([Object.freeze({results:Object.freeze([a])}),Object.freeze({results:Object.freeze([b])})]);expect(mergeRootPages(pages)).toEqual([b]);expect(pages[0].results[0]).toBe(a);expect(a.version).toBe(0)})
})
