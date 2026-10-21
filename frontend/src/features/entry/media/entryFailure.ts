import {ApiRequestError} from '../../../api/errors'
/** Canonical server field guidance is safe text; it explains validation with no raw transport details. */
export function entryFailureMessage(error:unknown,fields:string[]):string {
 if(error instanceof ApiRequestError){const detail=error.fieldErrors.find(item=>fields.some(field=>item.field===field||item.field.startsWith(field+'.')))?.message;if(detail)return detail;if(error.code==='REQUEST_TIMEOUT')return 'Processing timed out. Your current entry details are retained. You can try processing again.'}
 return error instanceof Error?error.message:'The request could not be completed. Try again or continue manually.'
}
