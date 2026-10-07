import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cancelOrder, fetchOrders, placeOrder } from '../services/api'

const formatPrice = (price) => `₹${Number(price).toLocaleString('en-IN')}`

export default function CheckoutPage() {
  const [orders, setOrders] = useState([])
  const [placedOrder, setPlacedOrder] = useState(null)
  const [error, setError] = useState('')
  const [placing, setPlacing] = useState(false)
  const [cancelling, setCancelling] = useState(null)

  useEffect(() => {
    fetchOrders().then(setOrders).catch(() => setError('Could not load your order history.'))
  }, [])

  async function handlePlaceOrder() {
    setError('')
    setPlacing(true)
    try {
      const order = await placeOrder()
      setPlacedOrder(order)
      setOrders((current) => [order, ...current])
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Could not place this order. Check your cart and stock.')
    } finally {
      setPlacing(false)
    }
  }

  async function handleCancelOrder(orderId) {
    setError('')
    setCancelling(orderId)
    try {
      const cancelled = await cancelOrder(orderId)
      setOrders((current) => current.map((order) => order.id === orderId ? cancelled : order))
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'This order can no longer be cancelled.')
    } finally {
      setCancelling(null)
    }
  }

  return <main className="simple-page site-shell"><nav className="nav-bar"><Link className="brand" to="/"><span>F</span> FITGEAR</Link><Link className="back-link" to="/">Back to collection</Link></nav><section className="checkout-layout"><div><p className="eyebrow">CHECKOUT</p><h1>Make it<br /><em>official.</em></h1><p className="checkout-copy">Your order is placed with cash-on-delivery ready for the next step. Inventory is checked and reserved in one transaction.</p>{placedOrder ? <div className="confirmation"><p className="eyebrow">ORDER CONFIRMED</p><h2>Order #{placedOrder.id}</h2><p>{formatPrice(placedOrder.total)} · {placedOrder.status}</p><Link className="primary-button" to="/">Continue shopping <span>↗</span></Link></div> : <button className="primary-button" type="button" onClick={handlePlaceOrder} disabled={placing}>{placing ? 'Placing order...' : 'Place order'} <span>↗</span></button>}{error && <p className="form-error">{error}</p>}</div><div className="order-history"><p className="eyebrow">YOUR ORDERS</p>{orders.length ? orders.map((order) => <article className="history-item" key={order.id}><div><strong>Order #{order.id}</strong><span>{new Date(order.createdAt).toLocaleDateString()} · {order.status}</span></div><div className="history-actions"><b>{formatPrice(order.total)}</b>{(order.status === 'PLACED' || order.status === 'CONFIRMED') && <button type="button" onClick={() => handleCancelOrder(order.id)} disabled={cancelling === order.id}>{cancelling === order.id ? 'Cancelling...' : 'Cancel'}</button>}</div></article>) : <p className="empty-state">No orders yet.</p>}</div></section></main>
}
