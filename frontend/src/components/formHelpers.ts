// Presentation helpers reused from Demo ui.tsx.
export function focusError() {
  requestAnimationFrame(() => {
    document
      .querySelector<HTMLElement>('[aria-invalid="true"], .Mui-error input')
      ?.focus();
  });
}
export const messageOf = (e: unknown) =>
  e instanceof Error ? e.message : "Something went wrong. Please try again.";
