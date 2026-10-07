import { useCallback } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuth } from './context/useAuth'
import { useToast } from './context/useToast'
import { useOrderEvents } from './services/sse'
import './App.css'

/**
 * Shared app shell: every route renders inside Navbar + Footer.
 * The signed-in user's live order stream lives here so a single
 * SSE connection serves the whole app; order pages listen for the
 * `fitgear:order-event` window event to refresh themselves.
 */
export default function App() {
  const { user } = useAuth()
  const { push } = useToast()

  const handleOrderEvent = useCallback(
    (event) => {
      if (event?.type !== 'ORDER_STATUS_CHANGED') return
      const payload = event.payload || {}
      push(payload.message || `Order #${payload.orderId} updated.`, 'live')
      window.dispatchEvent(new CustomEvent('fitgear:order-event', { detail: payload }))
    },
    [push],
  )

  useOrderEvents(user, handleOrderEvent)

  return <Outlet />
}
