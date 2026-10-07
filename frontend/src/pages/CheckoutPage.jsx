import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'
import {
  createCheckout,
  errorMessage,
  fetchPaymentConfig,
  placeOrder,
  verifyPayment,
} from '../services/api'
import { formatPrice } from '../utils/format'

const emptyAddress = {
  receiverName: '',
  phone: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  pincode: '',
}

function loadRazorpayScript() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) {
      resolve(window.Razorpay)
      return
    }
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload = () => resolve(window.Razorpay)
    script.onerror = () => reject(new Error('Could not load Razorpay checkout.'))
    document.body.appendChild(script)
  })
}

export default function CheckoutPage() {
  const { items, subtotal, refresh, clear } = useCart()
  const { push } = useToast()
  const navigate = useNavigate()

  const [address, setAddress] = useState(emptyAddress)
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [paymentEnabled, setPaymentEnabled] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchPaymentConfig()
      .then((config) => setPaymentEnabled(Boolean(config.razorpayEnabled)))
      .catch(() => setPaymentEnabled(false))
  }, [])

  function update(field) {
    return (event) => setAddress((current) => ({ ...current, [field]: event.target.value }))
  }

  async function handlePlaceOrder(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)

    try {
      const order = await placeOrder({ address, paymentMethod })
      await refresh()

      if (paymentMethod === 'COD') {
        await clear()
        push('Order placed. Pay on delivery!', 'success')
        navigate(`/orders/${order.id}`, { state: { confirmed: true } })
        return
      }

      // Razorpay flow: create the payment, open the modal, verify on success.
      const checkout = await createCheckout(order.id)
      const Razorpay = await loadRazorpayScript()

      const options = {
        key: checkout.keyId,
        currency: checkout.currency,
        name: 'FitGear',
        description: `Order #${order.id}`,
        amount: checkout.amountPaise,
        order_id: checkout.razorpayOrderId,
        prefill: {
          name: checkout.customerName || address.receiverName,
          email: checkout.customerEmail,
        },
        theme: { color: '#f05932' },
        handler: async (response) => {
          try {
            await verifyPayment({
              orderId: order.id,
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
            await clear()
            push('Payment verified. Order confirmed!', 'success')
            navigate(`/orders/${order.id}`, { state: { confirmed: true } })
          } catch (verifyError) {
            setError(errorMessage(verifyError, 'Payment verification failed. Contact support with your payment ID.'))
          }
        },
        modal: {
          ondismiss: () => {
            setError('Payment cancelled. You can retry from the order page.')
            push('Payment cancelled.', 'info')
            navigate(`/orders/${order.id}`)
          },
        },
      }

      const rzp = new Razorpay(options)
      rzp.on('payment.failed', (response) => {
        setError(response.error?.description || 'Payment failed. Please try again.')
        push('Payment failed.', 'error')
      })
      rzp.open()
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not place the order.'))
      await refresh()
    } finally {
      setSubmitting(false)
    }
  }

  if (!items.length) {
    return (
      <div className="site-shell">
        <Navbar />
        <section className="simple-page-content">
          <p className="eyebrow">CHECKOUT</p>
          <h1>Nothing to check out.</h1>
          <p>Add some equipment to your bag first.</p>
          <Link className="primary-button" to="/products">Browse equipment <span>↘</span></Link>
        </section>
        <Footer />
      </div>
    )
  }

  return (
    <div className="site-shell">
      <Navbar />

      <form className="checkout-page" onSubmit={handlePlaceOrder}>
        <div className="checkout-main">
          <p className="eyebrow">CHECKOUT</p>
          <h1>Almost<br /><em>there.</em></h1>

          <section className="checkout-section" aria-labelledby="address-heading">
            <h2 id="address-heading">Shipping address</h2>
            <div className="form-grid">
              <label>
                Receiver name
                <input required maxLength="120" value={address.receiverName} onChange={update('receiverName')} autoComplete="name" />
              </label>
              <label>
                Phone
                <input required maxLength="15" value={address.phone} onChange={update('phone')} autoComplete="tel" pattern="[0-9+\-\s]{8,15}" />
              </label>
              <label className="span-2">
                Address line 1
                <input required maxLength="255" value={address.line1} onChange={update('line1')} autoComplete="address-line1" />
              </label>
              <label className="span-2">
                Address line 2 <small>(optional)</small>
                <input maxLength="255" value={address.line2} onChange={update('line2')} autoComplete="address-line2" />
              </label>
              <label>
                City
                <input required maxLength="120" value={address.city} onChange={update('city')} autoComplete="address-level2" />
              </label>
              <label>
                State
                <input required maxLength="120" value={address.state} onChange={update('state')} autoComplete="address-level1" />
              </label>
              <label>
                PIN code
                <input required maxLength="10" value={address.pincode} onChange={update('pincode')} autoComplete="postal-code" pattern="[0-9]{4,10}" />
              </label>
            </div>
          </section>

          <section className="checkout-section" aria-labelledby="payment-heading">
            <h2 id="payment-heading">Payment method</h2>
            <div className="payment-options">
              <label className={`payment-option ${paymentMethod === 'RAZORPAY' ? 'selected' : ''}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="RAZORPAY"
                  checked={paymentMethod === 'RAZORPAY'}
                  disabled={!paymentEnabled}
                  onChange={() => setPaymentMethod('RAZORPAY')}
                />
                <span>
                  <strong>Razorpay</strong>
                  <small>Card, UPI, netbanking — secure checkout</small>
                  {!paymentEnabled && <em>Razorpay is not configured on this deployment.</em>}
                </span>
              </label>
              <label className={`payment-option ${paymentMethod === 'COD' ? 'selected' : ''}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="COD"
                  checked={paymentMethod === 'COD'}
                  onChange={() => setPaymentMethod('COD')}
                />
                <span>
                  <strong>Cash on delivery</strong>
                  <small>Pay when your order arrives</small>
                </span>
              </label>
            </div>
          </section>

          {error && <p className="form-error" role="alert">{error}</p>}
        </div>

        <aside className="order-summary checkout-summary">
          <h2>Your order</h2>
          <div className="summary-items">
            {items.map((item) => (
              <div className="summary-item" key={item.id}>
                <span>{item.name} × {item.quantity}</span>
                <span>{formatPrice(item.price * item.quantity)}</span>
              </div>
            ))}
          </div>
          <div className="summary-line"><span>Subtotal</span><span>{formatPrice(subtotal)}</span></div>
          <div className="summary-line"><span>Shipping</span><span>Free</span></div>
          <div className="summary-line summary-total"><span>Total</span><strong>{formatPrice(subtotal)}</strong></div>
          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Placing order…' : paymentMethod === 'COD' ? 'Place order' : 'Pay now'} <span>↗</span>
          </button>
          <p className="summary-note">Stock is reserved and payment captured securely.</p>
        </aside>
      </form>

      <Footer />
    </div>
  )
}
