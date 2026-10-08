import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import ProductImage from '../components/ProductImage'
import { useAuth } from '../context/useAuth'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'
import { errorMessage } from '../services/api'
import { formatPrice } from '../utils/format'

export default function CartPage() {
  const { user } = useAuth()
  const { items, subtotal, itemCount, setQuantity, remove, clear, loading } = useCart()
  const { push } = useToast()
  const navigate = useNavigate()
  const [busyId, setBusyId] = useState(null)

  async function changeQuantity(item, quantity) {
    setBusyId(item.id)
    try {
      await setQuantity(item.id, quantity)
    } catch (error) {
      push(errorMessage(error, 'Could not update quantity.'), 'error')
    } finally {
      setBusyId(null)
    }
  }

  async function removeItem(item) {
    setBusyId(item.id)
    try {
      await remove(item.id)
      push(`${item.name} removed from your bag.`, 'info')
    } catch (error) {
      push(errorMessage(error, 'Could not remove item.'), 'error')
    } finally {
      setBusyId(null)
    }
  }

  async function clearAll() {
    if (!window.confirm('Empty your bag?')) return
    try {
      await clear()
      push('Your bag is now empty.', 'info')
    } catch (error) {
      push(errorMessage(error, 'Could not clear the bag.'), 'error')
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="cart-page simple-page-content">
        <p className="eyebrow">YOUR BAG</p>
        <h1>{itemCount ? 'Ready when you are.' : 'Your bag is empty.'}</h1>

        {!items.length ? (
          <div className="empty-state">
            <p>{loading ? 'Loading your bag…' : 'Fill it with equipment that earns its place.'}</p>
            <Link className="primary-button" to="/products">Browse equipment <span>↘</span></Link>
          </div>
        ) : (
          <div className="cart-layout">
            <div className="cart-table">
              <div className="cart-table-head">
                <span>Product</span>
                <span>Quantity</span>
                <span>Total</span>
                <span aria-label="Actions" />
              </div>
              {items.map((item) => (
                <div className="cart-row" key={item.id}>
                  <div className="cart-row-product">
                    <div className="cart-thumb">
                      <ProductImage src={item.imageUrl} name={item.name} category={item.category} />
                    </div>
                    <div>
                      <Link to={`/products/${item.id}`}><strong>{item.name}</strong></Link>
                      <span className="cart-row-meta">{item.category} · {formatPrice(item.price)} each</span>
                      {item.stock < item.quantity && (
                        <span className="stock-warning">Only {item.stock} in stock</span>
                      )}
                    </div>
                  </div>

                  <div className="quantity-stepper">
                    <button
                      type="button"
                      disabled={busyId === item.id || item.quantity <= 1}
                      onClick={() => changeQuantity(item, item.quantity - 1)}
                      aria-label={`Decrease ${item.name}`}
                    >
                      −
                    </button>
                    <span>{item.quantity}</span>
                    <button
                      type="button"
                      disabled={busyId === item.id || item.quantity >= item.stock}
                      onClick={() => changeQuantity(item, item.quantity + 1)}
                      aria-label={`Increase ${item.name}`}
                    >
                      +
                    </button>
                  </div>

                  <strong className="cart-row-total">{formatPrice(item.price * item.quantity)}</strong>

                  <button
                    className="remove-button"
                    type="button"
                    disabled={busyId === item.id}
                    onClick={() => removeItem(item)}
                  >
                    Remove
                  </button>
                </div>
              ))}

              <div className="cart-table-foot">
                <button className="remove-button" type="button" onClick={clearAll}>Empty bag</button>
                <Link className="back-link" to="/products">Continue shopping →</Link>
              </div>
            </div>

            <aside className="order-summary">
              <h2>Summary</h2>
              <div className="summary-line"><span>Items</span><span>{itemCount}</span></div>
              <div className="summary-line"><span>Subtotal</span><span>{formatPrice(subtotal)}</span></div>
              <div className="summary-line"><span>Shipping</span><span>Free</span></div>
              <div className="summary-line summary-total"><span>Total</span><strong>{formatPrice(subtotal)}</strong></div>

              <button
                className="primary-button"
                type="button"
                onClick={() => navigate(user ? '/checkout' : '/login', user ? undefined : { state: { from: '/checkout' } })}
              >
                {user ? 'Checkout' : 'Sign in to checkout'} <span>↗</span>
              </button>
              {!user && <p className="summary-note">An account is required to place an order.</p>}
            </aside>
          </div>
        )}
      </section>

      <Footer />
    </div>
  )
}
