/** Clone first so recursive freezing never mutates caller-owned editable inputs. */
export function freezeSubmissionPayload<T>(payload:T):T{const copy=structuredClone(payload);function freeze(value:unknown){if(value&&typeof value==='object'){Object.values(value).forEach(freeze);Object.freeze(value)}}freeze(copy);return copy}
