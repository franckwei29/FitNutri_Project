import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { registerAndLogin, completeQuestionnaire } from './browser-journeys.mjs';
import { assertE2EEnvironment, syntheticIdentity } from './owned-db-verification.mjs';
import type { SubmissionReceiptV2, FoodRootV2 } from '../../src/api/contracts';
export const baseURL = process.env.FITNUTRI_E2E_BASE_URL ?? 'http://localhost:15173';
export async function newOwner(page: Page, prefix: string) {
    assertE2EEnvironment();
    const identity = syntheticIdentity(prefix);
    await registerAndLogin(page, identity, { baseURL });
    await completeQuestionnaire(page);
    return identity;
}
export async function select(page: Page, label: string, name: string) {
    await page.getByRole('combobox', { name: label, exact: true }).click();
    await page.getByRole('option', { name, exact: true }).click();
}
export async function beginFood(page: Page, date = '2020-01-02') {
    await page.goto(baseURL + '/add');
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Add food', exact: true })).toBeVisible();
    await page.getByLabel('Record date', { exact: true }).fill(date);
    await select(page, 'Default meal', 'Lunch');
}
export async function reference(page: Page, label: string, query: string, name: string) {
    await page.getByRole('combobox', { name: label, exact: true }).fill(query);
    const choice = page.getByRole('option').filter({ has: page.getByText(name, { exact: true }) });
    await expect(choice).toHaveCount(1);
    await choice.click();
}
export async function addFood(page: Page, query = 'rice', name = 'White rice, cooked', grams = '150') {
    await reference(page, 'Find food to add', query, name);
    await page.getByLabel('Grams to add', { exact: true }).fill(grams);
    await page.getByRole('button', { name: 'Add to meal', exact: true }).click();
    await expect(page.getByLabel('Grams to add', { exact: true })).toHaveValue('');
}
export async function foodReview(page: Page) {
    await page.getByRole('button', { name: 'Review food entries', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Review your record.', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Confirm & save', exact: true })).toBeEnabled();
}
export async function confirmFood(page: Page, double = false): Promise<SubmissionReceiptV2> {
    const response = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname === '/api/v2/food/batches');
    if (double)
        await page.getByRole('button', { name: 'Confirm & save', exact: true }).evaluate((button: HTMLButtonElement) => {
            button.click();
            button.click();
        });
    else
        await page.getByRole('button', { name: 'Confirm & save', exact: true }).click();
    const ack = await response;
    expect(ack.status()).toBe(201);
    const proof = await ack.json() as SubmissionReceiptV2;
    expect(proof.schemaVersion).toBe(2);
    await expect(page).toHaveURL(baseURL + '/dashboard');
    await expect(page.getByRole('heading', { name: 'A little progress, every day.', exact: true })).toBeVisible();
    return proof;
}
export async function foodRoots(page: Page, date = '2020-01-02') {
    const response = await page.request.get(baseURL + '/api/v2/food/entries', { params: { from: date, to: date, page: 0, size: 20 } });
    expect(response.status()).toBe(200);
    return await response.json() as {
        results: FoodRootV2[];
        totalMatches: number;
    };
}
export async function history(page: Page, date = '2020-01-02', type = 'food') {
    await page.goto(`${baseURL}/history?from=${date}&to=${date}&type=${type}`);
    await expect(page.getByRole('heading', { name: 'Your history, in perspective.', exact: true })).toBeVisible();
}
export async function makeDish(page: Page, portion: 'PERSONAL' | 'FRACTION' | 'PERCENT' | 'SERVINGS' = 'FRACTION', name = 'Synthetic browser recipe') {
    await page.getByRole('button', { name: 'Record by ingredients', exact: true }).click();
    await page.getByLabel('Dish name', { exact: true }).fill(name);
    if (portion !== 'PERSONAL') {
        await page.getByRole('button', { name: 'Use full recipe amounts', exact: true }).click();
        await page.getByRole('button', { name: 'Apply mode change', exact: true }).click();
    }
    const choices = [['rice', 'White rice, cooked', '301'], ['chicken', 'Chicken breast, lean, raw', '99'], ['apple', 'Fuji apple, unpeeled, raw', '80'], ['Cavendish banana', 'Cavendish banana, peeled, raw', '60']];
    for (let i = 0; i < choices.length; i++) {
        if (i)
            await page.getByRole('button', { name: 'Add ingredient', exact: true }).click();
        await reference(page, `Find ingredient ${i + 1}`, choices[i][0], choices[i][1]);
        await page.getByLabel(`${portion === 'PERSONAL' ? 'Grams in my portion' : 'Used in the full recipe (g)'} ${i + 1}`, { exact: true }).fill(choices[i][2]);
    }
    if (portion === 'FRACTION')
        await page.getByRole('button', { name: '1/3 of recipe', exact: true }).click();
    else if (portion === 'PERCENT') {
        await page.getByRole('button', { name: 'Custom percentage', exact: true }).click();
        await page.getByLabel('Percentage eaten', { exact: true }).fill('33.3');
    }
    else if (portion === 'SERVINGS') {
        await page.getByRole('button', { name: 'Servings', exact: true }).click();
        await page.getByLabel('Total servings', { exact: true }).fill('3');
        await page.getByLabel('Servings eaten', { exact: true }).fill('1');
    }
    await page.getByRole('button', { name: 'Add dish to meal', exact: true }).click();
}
