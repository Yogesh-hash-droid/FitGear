import { useEffect, useRef } from 'react'
import { baseURL } from './api'

/**
 * Live order-status stream.
 *
 * The built-in EventSource cannot send an Authorization header, so this
 * implements the SSE wire protocol on top of fetch: reads the response
 * body as a stream, splits frames on blank lines, parses `data:` lines
 * and dispatches structured events. Reconnects with exponential backoff
 * (heartbeats every 25s from the server keep idle connections warm).
 *
 * @param {object|null} user  current user (null disables the stream)
 * @param {(event: {type: string, payload: object, timestamp: string}) => void} onEvent
 */
export function useOrderEvents(user, onEvent) {
  const onEventRef = useRef(onEvent)

  useEffect(() => {
    onEventRef.current = onEvent
  }, [onEvent])

  const userKey = user?.email

  useEffect(() => {
    if (!userKey) return undefined

    const token = localStorage.getItem('fitgear_token')
    if (!token) return undefined

    let stopped = false
    let controller = new AbortController()
    let retryDelay = 1000
    let retryTimer

    function parseFrame(frame) {
      const dataLines = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).replace(/^ /, ''))
      if (!dataLines.length) return
      try {
        const parsed = JSON.parse(dataLines.join('\n'))
        onEventRef.current?.(parsed)
      } catch {
        // Ignore malformed frames; keep the stream alive.
      }
    }

    async function connect() {
      while (!stopped) {
        try {
          const response = await fetch(`${baseURL}/notifications/stream`, {
            headers: {
              Accept: 'text/event-stream',
              Authorization: `Bearer ${token}`,
            },
            signal: controller.signal,
          })

          if (!response.ok || !response.body) {
            throw new Error(`stream failed with status ${response.status}`)
          }

          retryDelay = 1000
          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          let buffer = ''

          while (!stopped) {
            const { done, value } = await reader.read()
            if (done) break
            buffer += decoder.decode(value, { stream: true })

            let boundary = buffer.indexOf('\n\n')
            while (boundary !== -1) {
              const frame = buffer.slice(0, boundary)
              buffer = buffer.slice(boundary + 2)
              parseFrame(frame)
              boundary = buffer.indexOf('\n\n')
            }
          }
          if (stopped) break
          throw new Error('stream closed by server')
        } catch (error) {
          if (stopped || error?.name === 'AbortError') break
          await new Promise((resolve) => {
            retryTimer = setTimeout(resolve, retryDelay)
          })
          retryDelay = Math.min(retryDelay * 2, 30_000)
          controller = new AbortController()
        }
      }
    }

    connect()

    return () => {
      stopped = true
      clearTimeout(retryTimer)
      controller.abort()
    }
  }, [userKey])
}
