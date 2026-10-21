import { expect } from '@playwright/test';
import type { Page, Locator } from '@playwright/test';
import { baseURL } from './v3-journeys';
import type { ExerciseRootV2, SubmissionReceiptV2 } from '../../src/api/contracts';
export async function beginExercise(page: Page, date = '2020-01-02', gym = false) {
    await page.goto(baseURL + '/add');
    await page.getByRole('button', { name: /Exercise Log your physical activity/ }).click();
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Add exercise', exact: true })).toBeVisible();
    if (gym)
        await page.getByRole('button', { name: 'Gym workout', exact: true }).click();
    await page.getByLabel('Record date', { exact: true }).fill(date);
}
export async function activity(target: Page | Locator, label: string, query: string, name: string) {
    await target.getByRole('combobox', { name: label, exact: true }).fill(query);
    await ('page' in target ? target.page() : target).getByRole('option', { name: `Activity: ${name}`, exact: true }).click();
}
export async function exerciseReview(page: Page, gym = false) {
    await page.getByRole('button', { name: gym ? 'Review workout' : 'Review exercise entry', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Review your record.', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Confirm & save', exact: true })).toBeEnabled();
}
export async function confirmExercise(page: Page): Promise<SubmissionReceiptV2> {
    const received = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname === '/api/v2/exercise/entries');
    await page.getByRole('button', { name: 'Confirm & save', exact: true }).click();
    const response = await received;
    expect(response.status()).toBe(201);
    const proof = await response.json() as SubmissionReceiptV2;
    await expect(page).toHaveURL(baseURL + '/dashboard');
    await expect(page.getByRole('heading', { name: 'A little progress, every day.', exact: true })).toBeVisible();
    return proof;
}
export async function exerciseRoots(page: Page, date = '2020-01-02') {
    const response = await page.request.get(baseURL + '/api/v2/exercise/entries', { params: { from: date, to: date, page: 0, size: 20 } });
    expect(response.status()).toBe(200);
    return await response.json() as {
        results: ExerciseRootV2[];
        totalMatches: number;
    };
}
