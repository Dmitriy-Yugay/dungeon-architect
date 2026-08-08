# Project rules

## General

- Use Kotlin idioms.
- Do not use Java-style patterns when Kotlin alternatives exist.
- Prefer simple architecture over premature abstraction.
- Explain important design decisions.

## Game

- Keep gameplay systems separated.
- Avoid hard-coded values.
- Use configuration files for balancing values.

## Code

- Write tests for non-visual logic.
- Keep classes small.
- Use relevant, well-maintained frameworks and tools when they improve
  correctness, iteration speed, testing, observability, or maintainability.
- Prefer a focused dependency with a clear project benefit over building and
  maintaining an equivalent custom framework.
- Record the reason for significant new dependencies and keep them behind the
  layer that needs them.
- Use Kotest, including property-based testing, when generated cases express a
  gameplay invariant more clearly or thoroughly than example-only tests.
