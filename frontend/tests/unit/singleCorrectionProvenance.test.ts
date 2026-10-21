import { it, expect } from 'vitest';
import { draftFromOwnedRoot } from '../../src/features/history/reuseEntry';
import { rootCorrectionInput, rootCorrectionDirty, prepareRootCorrection } from '../../src/features/entry/rootCorrectionIntent';
import { matchesRootCorrection } from '../../src/features/entry/rootMutationState';
import { savedSingleV2 } from '../fixtures/entryRootsV2';
import { foodDraft } from '../fixtures/entry';
import { standaloneFoods } from '../../src/features/entry/draftSelectors';
import type { FoodPreviewV2 } from '../../src/api/contracts';
it.each(['EXACT', 'RELATED', 'APPROXIMATE', 'USER_SELECTED'] as const)('retains saved %s single selection provenance through correction and exact returned-state proof', async (selectionKind) => {
    const root = savedSingleV2();
    if (root.input.kind !== 'SINGLE')
        throw Error();
    root.input.selectionKind = selectionKind;
    root.input.searchMetadataRevision = 'published-metadata-r1';
    const draft = await draftFromOwnedRoot('1', root, new AbortController().signal, async (id) => ({ ...standaloneFoods(foodDraft())[0].selectedReference!, referenceId: id, per100g: { energyKj: 1000, proteinGrams: 10, fatGrams: 4, carbsGrams: 20 } }));
    expect(rootCorrectionInput(draft)).toMatchObject({ entry: { selectionKind, searchMetadataRevision: 'published-metadata-r1' } });
    expect(rootCorrectionDirty(draft)).toBe(false);
    if (draft.kind !== 'food' || draft.entries[0].entryKind !== 'SINGLE')
        throw Error();
    draft.entries[0].quantityGramsInput = '200';
    const nutrients = { energyKj: 2000, proteinGrams: 20, fatGrams: 8, carbsGrams: 40 }, preview: FoodPreviewV2 = { entries: [{ ...root.preview, leaves: root.preview.leaves.map(leaf => ({ ...leaf, inputGrams: 200, consumedGrams: 200, nutrients })), totals: nutrients }], totals: nutrients, fingerprint: 'a'.repeat(64), inputSignature: 'b'.repeat(64), calculationPolicy: 'Controlled synthetic correction fixture' };
    const intent = prepareRootCorrection(draft, '1', preview, '2026-10-18');
    if (intent.kind !== 'food')
        throw Error();
    const returned = { ...root, version: root.version + 1, input: { ...root.input, quantityGrams: 200 }, preview: preview.entries[0] };
    expect(matchesRootCorrection(intent, returned)).toBe(true);
    expect(matchesRootCorrection(intent, { ...returned, input: { ...returned.input, searchMetadataRevision: 'different-revision' } })).toBe(false);
});
