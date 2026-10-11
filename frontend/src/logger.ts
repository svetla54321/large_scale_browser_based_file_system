type Context = Record<string, unknown>

export const log = {
  debug(message: string, context?: Context) {
    if (import.meta.env.DEV) console.debug(message, context ?? '')
  },
  warn(message: string, context?: Context) {
    console.warn(message, context ?? '')
  },
  error(message: string, error?: unknown, context?: Context) {
    console.error(message, error, context ?? '')
  },
}
