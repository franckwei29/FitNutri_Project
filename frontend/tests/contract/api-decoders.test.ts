import { describe, expect, it } from 'vitest'
import { decodeResponse } from '../../src/api/validators'
import { CurrentUserSchema, NutrientsSchema, ProfileSettingsRequestSchema, ExerciseUpdateRequestSchema, SubmissionReceiptSchema, FoodBatchRequestSchema, RegisterRequestSchema } from '../../src/api/schemas'
import examples from '../../../docs/api/examples/write-requests.json'

describe('F01 canonical contract decoding', () => {
  const user={id:'9007199254740993',email:'synthetic@example.test',displayName:'Synthetic',role:'USER',enabled:true,version:0}
  it('preserves opaque large string IDs and rejects numeric IDs/unknown enum', () => {
    expect(decodeResponse(CurrentUserSchema,user).id).toBe('9007199254740993')
    expect(()=>decodeResponse(CurrentUserSchema,{...user,id:9007199254740992})).toThrow('Unexpected server response')
    expect(()=>decodeResponse(CurrentUserSchema,{...user,role:'ROOT'})).toThrow()
  })
  it('requires null fields, rejects nonfinite values and HTML fallback', () => {
    const nutrients={energyKj:null,proteinGrams:0,fatGrams:null,carbsGrams:null}
    expect(decodeResponse(NutrientsSchema,nutrients)).toEqual(nutrients)
    expect(()=>decodeResponse(NutrientsSchema,{...nutrients,energyKj:Infinity})).toThrow()
    expect(()=>decodeResponse(NutrientsSchema,{})).toThrow()
    expect(()=>decodeResponse(CurrentUserSchema,'<html>proxy fallback</html>')).toThrow()
  })
  it('does not retain password hashes or invalid receipts', () => {
    expect(()=>decodeResponse(CurrentUserSchema,{...user,passwordHash:'secret'})).toThrow()
    expect(()=>decodeResponse(SubmissionReceiptSchema,{submissionId:'invalid',records:[]})).toThrow()
  })
  it('accepts explicit null goals and false consent, never omitted goals', () => {
    const settings=examples.find(x=>x.schema==='ProfileSettingsRequest')?.value
    expect(settings).toBeTruthy()
    const parsed=decodeResponse(ProfileSettingsRequestSchema,settings)
    expect(parsed.goalType).toBeNull()
    expect(parsed.qaConsentGranted).toBe(false)
    const missing: Record<string, unknown>={...parsed}
    delete missing.goalType
    expect(()=>decodeResponse(ProfileSettingsRequestSchema,missing)).toThrow()
  })
  it('validates the shared two-layer request fixture and rejects empty batches', () => {
    const payload=examples.find(x=>x.schema==='FoodBatchRequest')?.value
    expect(decodeResponse(FoodBatchRequestSchema,payload).items).toHaveLength(1)
    expect(()=>decodeResponse(FoodBatchRequestSchema,{...payload,items:[]})).toThrow()
  })
  it('rejects undeclared writable role and nutrient fields instead of forwarding them', () => {
    const registration=examples.find(x=>x.schema==='RegisterRequest')?.value
    expect(()=>decodeResponse(RegisterRequestSchema,{...registration,role:'ADMIN'})).toThrow()
    const payload=decodeResponse(FoodBatchRequestSchema,examples.find(x=>x.schema==='FoodBatchRequest')?.value)
    expect(()=>decodeResponse(FoodBatchRequestSchema,{...payload,items:payload.items.map(x=>({...x,calories:999}))})).toThrow()
  })

})

describe('B12 correction input contract',()=>{
  it('allows an omitted no-op exercise fingerprint while retaining required integer version and strict authority fields',()=>{
    const input={expectedVersion:0,exerciseTypeId:'11',durationMinutes:10,recordDate:'2020-01-02',zone:'Australia/Sydney'}
    expect(ExerciseUpdateRequestSchema.safeParse(input).success).toBe(true)
    expect(ExerciseUpdateRequestSchema.safeParse({...input,calculationFingerprint:null}).success).toBe(true)
    expect(ExerciseUpdateRequestSchema.safeParse({...input,expectedVersion:0.9}).success).toBe(false)
    expect(ExerciseUpdateRequestSchema.safeParse({...input,estimatedKj:999}).success).toBe(false)
  })
})
