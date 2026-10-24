/** Safe integer expression parser. No eval. Returns a normalized form or null. */
export function normalizeExpr(raw: string): string | null {
  const compact = raw.replace(/\s+/g, "").replace(/×/g, "*").replace(/÷/g, "/").replace(/−/g, "-");
  if (!/^[0-9+\-*/()]+$/.test(compact)) return null;
  if (compact.length < 3 || compact.length > 24) return null;
  if (!/[+*/-]/.test(compact)) return null;
  if (evaluate(compact) === null) return null;
  return compact;
}

export function prettyExpr(canonical: string): string {
  return canonical.replace(/\*/g, "×").replace(/\//g, "÷").replace(/-/g, "−");
}

function evaluate(s: string): number | null {
  let i = 0;

  function peek(): string | undefined {
    return s[i];
  }

  function parseExpr(): number | null {
    let left = parseTerm();
    if (left === null) return null;
    while (peek() === "+" || peek() === "-") {
      const op = s[i];
      i += 1;
      const right = parseTerm();
      if (right === null) return null;
      left = op === "+" ? left + right : left - right;
    }
    return left;
  }

  function parseTerm(): number | null {
    let left = parseFactor();
    if (left === null) return null;
    while (peek() === "*" || peek() === "/") {
      const op = s[i];
      i += 1;
      const right = parseFactor();
      if (right === null) return null;
      if (op === "/" && right === 0) return null;
      left = op === "*" ? left * right : left / right;
    }
    return left;
  }

  function parseFactor(): number | null {
    if (peek() === "(") {
      i += 1;
      const value = parseExpr();
      if (value === null || peek() !== ")") return null;
      i += 1;
      return value;
    }
    if (!peek() || !/[0-9]/.test(peek() as string)) return null;
    let n = 0;
    let digits = 0;
    while (peek() && /[0-9]/.test(peek() as string)) {
      n = n * 10 + Number(s[i]);
      i += 1;
      digits += 1;
      if (digits > 6) return null;
    }
    return n;
  }

  const value = parseExpr();
  if (value === null || i !== s.length || !Number.isFinite(value)) return null;
  return value;
}
