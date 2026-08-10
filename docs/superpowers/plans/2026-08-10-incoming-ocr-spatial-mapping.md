# Incoming OCR Spatial Mapping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make incoming-document OCR mapping assign fields by spatial neighbor relationships instead of unstable `line.00x` sequence numbers, and record match reasons.

**Architecture:** Extract pure OCR mapping helpers from `incoming-document-list.vue` into `admin3-ui/src/utils/incoming-ocr-mapping.ts`. Mapping creation records the chosen line plus nearest neighbor anchors; assignment resolves by anchor relation first and falls back to legacy `sourceField` only when spatial matching fails.

**Tech Stack:** Vue 3, TypeScript 4.6, Vite 3, existing OCR JSON shape `pages[].lines[]`.

## Global Constraints

- Keep changes focused on incoming-document OCR mapping only.
- Do not change OCR service response format or database schema in this step.
- Preserve existing saved mappings by keeping `sourceField`, `sourceText`, `sourceRect`, and `sourceIndex` fallback behavior.
- Add assignment history in front-end state for visibility/debugging.

---

### Task 1: Extract Pure Mapping Helpers

**Files:**
- Create: `admin3-ui/src/utils/incoming-ocr-mapping.ts`
- Modify: `admin3-ui/src/views/incoming-document-list.vue`

**Interfaces:**
- Produces: `buildOcrGroups(raw)`, `createMappingAnchor(targetField, sourceField, fields)`, `resolveMappedValue(mapping, fields, latest)`.

- [ ] Move existing parsing and geometry helper logic into the utility file.
- [ ] Import those helpers from the Vue page.
- [ ] Verify build still passes.

### Task 2: Add Spatial Neighbor Rule for OCR Lines

**Files:**
- Modify: `admin3-ui/src/utils/incoming-ocr-mapping.ts`
- Test: focused node/script or build verification using representative records from scan 9 and scan 11.

**Interfaces:**
- Produces mapping property `lineAnchor` with `{anchorText, anchorField, anchorRect, relation, targetRect}`.
- Produces assignment result `{value, reason, matchedField, fallback}`.

- [ ] Write a failing check: rule made from original `title <- line.011` resolves value from shifted scan where title value is `line.010` and label is `line.011`.
- [ ] Implement nearest OCR line neighbor matching for right/left/above/below.
- [ ] Prefer spatial line match before legacy `sourceField` fallback.
- [ ] Verify focused check passes.

### Task 3: Surface Assignment History

**Files:**
- Modify: `admin3-ui/src/views/incoming-document-list.vue`

**Interfaces:**
- Consumes: `resolveMappedValue` assignment result.
- Produces: `mappingApplyHistory` entries shown or available in console/state.

- [ ] Store one history item per assigned target field.
- [ ] Include target field, assigned value, matched field, reason, and whether fallback was used.
- [ ] Keep UI minimal: log history to console and preserve state for future display.

### Task 4: Verification

**Files:**
- Verify: `admin3-ui`

- [ ] Run focused check.
- [ ] Run `npm run build` in `admin3-ui`.
- [ ] Inspect `git diff` and confirm no unrelated rewrites.
