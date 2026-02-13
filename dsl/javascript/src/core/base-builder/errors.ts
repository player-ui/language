/**
 * Error codes for functional builder errors.
 * Use these codes for programmatic error handling.
 */
export const ErrorCodes = {
  /** Context is missing when required */
  MISSING_CONTEXT: "FLUENT_MISSING_CONTEXT",
  /** Invalid branch type in ID generation */
  INVALID_BRANCH: "FLUENT_INVALID_BRANCH",
  /** Template produced no output */
  TEMPLATE_NO_OUTPUT: "FLUENT_TEMPLATE_NO_OUTPUT",
  /** Invalid path in value resolution */
  INVALID_PATH: "FLUENT_INVALID_PATH",
} as const;

/**
 * Type for error codes
 */
export type ErrorCode = (typeof ErrorCodes)[keyof typeof ErrorCodes];

/**
 * Custom error class for functional builder errors.
 * Provides structured error information with error codes.
 */
export class FunctionalError extends Error {
  readonly code: ErrorCode;
  readonly context?: Record<string, unknown>;

  constructor(
    code: ErrorCode,
    message: string,
    context?: Record<string, unknown>,
  ) {
    const contextStr = context ? ` Context: ${JSON.stringify(context)}` : "";
    super(`[${code}] ${message}${contextStr}`);
    this.name = "FunctionalError";
    this.code = code;
    this.context = context;

    // Maintains proper stack trace in V8 environments
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, FunctionalError);
    }
  }
}

/**
 * Creates a FunctionalError with the given code and message.
 * This is a convenience function for creating errors.
 *
 * @param code - The error code from ErrorCodes
 * @param message - Human-readable error message
 * @param context - Optional context object with additional details
 * @returns A FunctionalError instance
 *
 * @example
 * throw createFunctionalError(
 *   ErrorCodes.MISSING_CONTEXT,
 *   "Context is required for template resolution",
 *   { templateCount: 3 }
 * );
 */
export function createFunctionalError(
  code: ErrorCode,
  message: string,
  context?: Record<string, unknown>,
): FunctionalError {
  return new FunctionalError(code, message, context);
}
