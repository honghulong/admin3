export interface OcrField {
  field: string;
  value: string;
  rect?: number[] | null;
  sourceIndex?: number | null;
  sourceType?: string;
}

export interface MappingResolveResult {
  value: string;
  matchedField?: string;
  reason: string;
  fallback: boolean;
}

export function createMappingAnchor(targetField: string, sourceField: string, fields: OcrField[]) {
  const source = fields.find(item => item.field === sourceField);
  const mapping: any = {
    targetField,
    sourceField,
    sourceType: source?.sourceType || inferSourceType(sourceField),
    sourceText: source?.value || '',
    sourceRect: source?.rect || null,
    sourceIndex: source?.sourceIndex ?? null
  };

  const tableLabel = source?.sourceType === 'table_cell' ? findNearestLeft(source, fields, 'table_cell', 0.45) : null;
  if (tableLabel) {
    mapping.spatialAnchor = {
      labelText: tableLabel.value,
      labelField: tableLabel.field,
      labelRect: tableLabel.rect,
      relation: 'right-neighbor',
      sameRow: true,
      nearest: true
    };
  }

  const lineAnchor = createLineAnchor(source, fields);
  if (lineAnchor) mapping.lineAnchor = lineAnchor;
  return mapping;
}

export function resolveMappedValue(mapping: any, fields: OcrField[], latest: Record<string, string>): MappingResolveResult | undefined {
  const lineMatch = resolveByLineAnchor(mapping, fields);
  if (lineMatch) return lineMatch;

  const cellMatch = resolveByTableCellAnchor(mapping, fields);
  if (cellMatch) return cellMatch;

  const rectMatch = resolveBySourceRect(mapping, fields);
  if (rectMatch) return rectMatch;

  if (latest[mapping.sourceField] !== undefined) {
    return {
      value: latest[mapping.sourceField],
      matchedField: mapping.sourceField,
      reason: `fallback to ${mapping.sourceField}`,
      fallback: true
    };
  }
  return undefined;
}

function createLineAnchor(source: OcrField | undefined, fields: OcrField[]) {
  if (!source || source.field === 'plain_text' || !Array.isArray(source.rect)) return null;
  const lines = fields.filter(item => item.sourceType === 'ocr_line' && item.field !== source.field && Array.isArray(item.rect));
  const left = findNearestLeft(source, lines, 'ocr_line', 0.35);
  if (left) {
    return {
      anchorText: left.value,
      anchorField: left.field,
      anchorRect: left.rect,
      relation: 'right-neighbor',
      targetText: source.value,
      targetRect: source.rect,
      targetField: source.field
    };
  }
  const right = findNearestRight(source, lines, 'ocr_line', 0.35);
  if (right) {
    return {
      anchorText: right.value,
      anchorField: right.field,
      anchorRect: right.rect,
      relation: 'left-neighbor',
      targetText: source.value,
      targetRect: source.rect,
      targetField: source.field
    };
  }
  return null;
}

function resolveByLineAnchor(mapping: any, fields: OcrField[]): MappingResolveResult | undefined {
  const anchor = mapping?.lineAnchor;
  if (!anchor?.anchorText || !anchor?.relation) return undefined;

  const lines = fields.filter(item => item.sourceType === 'ocr_line' && Array.isArray(item.rect));
  const matchedAnchor = lines
    .filter(item => normalizeText(item.value) === normalizeText(anchor.anchorText))
    .sort((a, b) => rectDistance(a.rect, anchor.anchorRect) - rectDistance(b.rect, anchor.anchorRect))[0];
  if (!matchedAnchor) return undefined;

  const candidate = anchor.relation === 'right-neighbor'
    ? findNearestRight(matchedAnchor, lines.filter(item => item.field !== matchedAnchor.field), 'ocr_line', 0.35)
    : findNearestLeft(matchedAnchor, lines.filter(item => item.field !== matchedAnchor.field), 'ocr_line', 0.35);
  if (!candidate) return undefined;

  return {
    value: candidate.value,
    matchedField: candidate.field,
    reason: `matched line anchor "${anchor.anchorText}" ${anchor.relation} -> ${candidate.field}`,
    fallback: false
  };
}

function resolveByTableCellAnchor(mapping: any, fields: OcrField[]): MappingResolveResult | undefined {
  const anchor = mapping?.spatialAnchor;
  if (!anchor?.labelText || anchor.relation !== 'right-neighbor') return undefined;

  const cells = fields.filter(item => item.sourceType === 'table_cell' && Array.isArray(item.rect));
  const label = cells
    .filter(item => normalizeText(item.value) === normalizeText(anchor.labelText))
    .sort((a, b) => rectDistance(a.rect, anchor.labelRect) - rectDistance(b.rect, anchor.labelRect))[0];
  if (!label) return undefined;

  const candidate = findNearestRight(label, cells.filter(item => item.field !== label.field), 'table_cell', 0.45);
  if (!candidate) return undefined;

  return {
    value: candidate.value,
    matchedField: candidate.field,
    reason: `matched table anchor "${anchor.labelText}" right-neighbor -> ${candidate.field}`,
    fallback: false
  };
}

function resolveBySourceRect(mapping: any, fields: OcrField[]): MappingResolveResult | undefined {
  if (!Array.isArray(mapping?.sourceRect)) return undefined;
  const type = mapping.sourceType || inferSourceType(mapping.sourceField || '');
  const candidates = fields
    .filter(item => item.sourceType === type && Array.isArray(item.rect))
    .filter(item => verticalOverlapRatio(item.rect!, mapping.sourceRect) > 0.45)
    .sort((a, b) => rectDistance(a.rect, mapping.sourceRect) - rectDistance(b.rect, mapping.sourceRect));
  const candidate = candidates[0];
  if (!candidate) return undefined;
  return {
    value: candidate.value,
    matchedField: candidate.field,
    reason: `matched sourceRect nearest ${type} -> ${candidate.field}`,
    fallback: false
  };
}

function findNearestLeft(source: OcrField, fields: OcrField[], type: string, minOverlap: number) {
  return findNearestHorizontal(source, fields, type, minOverlap, 'left');
}

function findNearestRight(source: OcrField, fields: OcrField[], type: string, minOverlap: number) {
  return findNearestHorizontal(source, fields, type, minOverlap, 'right');
}

function findNearestHorizontal(source: OcrField, fields: OcrField[], type: string, minOverlap: number, direction: 'left' | 'right') {
  const sourceRect = source.rect;
  if (!Array.isArray(sourceRect)) return null;
  return fields
    .filter(item => item.sourceType === type && Array.isArray(item.rect))
    .filter(item => {
      if (direction === 'left') return rectCenterX(item.rect) < rectCenterX(sourceRect);
      return rectCenterX(item.rect) > rectCenterX(sourceRect);
    })
    .filter(item => verticalOverlapRatio(item.rect!, sourceRect) > minOverlap)
    .sort((a, b) => {
      const gapA = direction === 'left' ? sourceRect[0] - a.rect![2] : a.rect![0] - sourceRect[2];
      const gapB = direction === 'left' ? sourceRect[0] - b.rect![2] : b.rect![0] - sourceRect[2];
      return gapA - gapB || rectDistance(a.rect, sourceRect) - rectDistance(b.rect, sourceRect);
    })[0] || null;
}

function inferSourceType(field: string) {
  if (field.startsWith('cell.')) return 'table_cell';
  if (field.startsWith('line.')) return 'ocr_line';
  if (field === 'plain_text') return 'plain_text';
  return 'legacy';
}

function normalizeText(value: any) {
  return String(value ?? '').replace(/\s+/g, ' ').trim();
}

function rectCenterX(rect: any) {
  return (rect[0] + rect[2]) / 2;
}

function rectCenterY(rect: any) {
  return (rect[1] + rect[3]) / 2;
}

function rectDistance(a: any, b: any) {
  if (!Array.isArray(a) || !Array.isArray(b)) return Number.MAX_SAFE_INTEGER;
  return Math.hypot(rectCenterX(a) - rectCenterX(b), rectCenterY(a) - rectCenterY(b));
}

function verticalOverlapRatio(a: number[], b: number[]) {
  const overlap = Math.max(0, Math.min(a[3], b[3]) - Math.max(a[1], b[1]));
  const minHeight = Math.max(1, Math.min(a[3] - a[1], b[3] - b[1]));
  return overlap / minHeight;
}
