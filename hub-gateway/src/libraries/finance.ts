/**
 * finance-mcp pilot library.
 *
 * Upstream: `finance-mcp` (#7 DL/mo, ~585), owned by the
 * `mcp-tools-lab` GitHub org. Market data, portfolio reporting,
 * personal finance calculators.
 *
 * IMPORTANT: finance-mcp NEVER executes trades, moves money, or
 * offers investment advice. All dispatchers stay read-only by
 * contract — see the "Financial actions" rule in the harness system
 * prompt. Any tool that looks like a trade or transfer must be
 * rejected in the handler, not just documented.
 */

import type {
  DispatchContext,
  ToolDefinition,
  ToolResult,
} from "../mcp/types.js";

export const TOOL_DEFINITIONS: readonly ToolDefinition[] = [
  {
    name: "finance__market_quote",
    description:
      "Look up the latest market quote for a ticker symbol across equities, ETFs, and crypto. Read-only.",
    inputSchema: {
      type: "object",
      properties: {
        symbol: { type: "string", description: "Ticker symbol (e.g. AAPL, BTC-USD)." },
        exchange: { type: "string" },
      },
      required: ["symbol"],
      additionalProperties: false,
    },
  },
  {
    name: "finance__portfolio_summary",
    description:
      "Summarize a portfolio from a list of holdings (symbol + quantity + cost basis). Returns mark-to-market value, unrealized P/L, allocation by asset class. NEVER places orders.",
    inputSchema: {
      type: "object",
      properties: {
        holdings: {
          type: "array",
          description: "List of {symbol, quantity, costBasis} objects.",
        },
        currency: { type: "string", description: "Display currency (USD, EUR, GBP)." },
      },
      required: ["holdings"],
      additionalProperties: false,
    },
  },
  {
    name: "finance__compound_interest",
    description:
      "Compute the future value of a savings plan with periodic contributions and a target annual rate.",
    inputSchema: {
      type: "object",
      properties: {
        principal: { type: "number" },
        monthlyContribution: { type: "number" },
        annualRatePercent: { type: "number" },
        years: { type: "number" },
      },
      required: ["principal", "annualRatePercent", "years"],
      additionalProperties: false,
    },
  },
];

export async function dispatchTool(
  toolName: string,
  _args: Record<string, unknown> | undefined,
  _ctx: DispatchContext = {},
): Promise<ToolResult> {
  return {
    content: [
      {
        type: "text",
        text:
          `finance-mcp pilot stub: ${toolName} is registered on the hub ` +
          `gateway but the upstream implementation is not yet vendored. ` +
          `finance-mcp is READ-ONLY by contract — no trades, no transfers. ` +
          `See hub-gateway/src/libraries/finance.ts for the wiring checklist.`,
      },
    ],
  };
}
