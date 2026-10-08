import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import ProductCard from '../components/ProductCard'
import ProductImage from '../components/ProductImage'
import ProductReviews from '../components/ProductReviews'
import SectionHeader from '../components/SectionHeader'
import { errorMessage, fetchProduct, fetchProducts } from '../services/api'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'
import { formatPrice } from '../utils/format'

export default function ProductDetailPage() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(null)
  const [related, setRelated] = useState([])
  const [quantity, setQuantity] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [adding, setAdding] = useState(false)
  const { add } = useCart()
  const { push } = useToast()

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    setProduct(null)
    setQuantity(1)

    fetchProduct(productId)
      .then((data) => {
        if (!active) return
        setProduct(data)
        if (data.category) {
          fetchProducts({ category: data.category, size: 4, page: 0, sort: 'id,asc' })
            .then((page) => {
              if (active) {
                setRelated((page.content || []).filter((item) => item.id !== data.id).slice(0, 4))
              }
            })
            .catch(() => {})
        }
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load this product.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [productId])

  async function handleAdd() {
    if (!product) return
    setAdding(true)
    try {
      await add(product, quantity)
      push(`${product.name} added to your bag.`, 'success')
      navigate('/cart')
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not add to bag.'), 'error')
    } finally {
      setAdding(false)
    }
  }

  async function handleRelatedAdd(item) {
    try {
      await add(item, 1)
      push(`${item.name} added to your bag.`, 'success')
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not add to bag.'), 'error')
    }
  }

  const outOfStock = product ? Number(product.stock) === 0 : false

  return (
    <div className="site-shell">
      <Navbar />

      <section className="product-detail-page">
        <Link className="back-link" to="/products">← Back to equipment</Link>

        {loading ? (
          <p className="empty-state">Loading product...</p>
        ) : error || !product ? (
          <div className="empty-state">
            <p className="form-error" role="alert">{error || 'Product not found.'}</p>
            <Link className="back-link" to="/products">Browse the catalog →</Link>
          </div>
        ) : (
          <>
            <div className="product-detail-layout">
              <div className="product-detail-visual">
                <ProductImage
                  src={product.imageUrl}
                  name={product.name}
                  category={product.category}
                  loading="eager"
                />
                <span className={`product-badge ${outOfStock ? 'badge-out' : ''}`}>
                  {outOfStock ? 'Sold out' : product.category || 'FitGear'}
                </span>
              </div>

              <div className="product-detail-copy">
                <p className="eyebrow">{product.category || 'Equipment'}</p>
                <h1>{product.name}</h1>
                <p className="detail-description">{product.description}</p>
                <strong className="detail-price">{formatPrice(product.price)}</strong>

                <p className={`stock-note ${outOfStock ? 'out' : Number(product.stock) < 6 ? 'low' : ''}`}>
                  {outOfStock
                    ? 'Out of stock'
                    : Number(product.stock) < 6
                      ? `Only ${product.stock} left in stock`
                      : `${product.stock} in stock`}
                </p>

                <div className="quantity-row">
                  <label htmlFor="detail-quantity">Quantity</label>
                  <div className="quantity-stepper">
                    <button
                      type="button"
                      onClick={() => setQuantity((value) => Math.max(1, value - 1))}
                      disabled={quantity <= 1}
                      aria-label="Decrease quantity"
                    >
                      −
                    </button>
                    <span id="detail-quantity">{quantity}</span>
                    <button
                      type="button"
                      onClick={() => setQuantity((value) => Math.min(product.stock || 99, value + 1))}
                      disabled={quantity >= Number(product.stock)}
                      aria-label="Increase quantity"
                    >
                      +
                    </button>
                  </div>
                </div>

                <button
                  className="primary-button"
                  type="button"
                  disabled={outOfStock || adding}
                  onClick={handleAdd}
                >
                  {outOfStock ? 'Unavailable' : adding ? 'Adding…' : <>Add to bag <span>+</span></>}
                </button>

                <ul className="pdp-points">
                  <li>Free shipping on selected fitness equipment</li>
                  <li>Cash on delivery or secure online payment</li>
                  <li>Live order tracking from dispatch to delivery</li>
                </ul>
              </div>
            </div>

            <div className="pdp-sections">
              <section className="info-section" aria-labelledby="info-heading">
                <h2 id="info-heading">Product information</h2>
                <p>{product.description}</p>
                <dl className="info-facts">
                  <div><dt>Category</dt><dd>{product.category || '—'}</dd></div>
                  <div><dt>Price</dt><dd>{formatPrice(product.price)}</dd></div>
                  <div>
                    <dt>Availability</dt>
                    <dd>{outOfStock ? 'Out of stock' : `${product.stock} in stock`}</dd>
                  </div>
                </dl>
              </section>

              <section className="info-section" aria-labelledby="shipping-heading">
                <h2 id="shipping-heading">Shipping &amp; delivery</h2>
                <ul>
                  <li>Free shipping on selected fitness equipment</li>
                  <li>Cash on delivery available at checkout</li>
                  <li>Live order status updates in your account</li>
                </ul>
              </section>

              <section className="info-section" aria-labelledby="why-heading">
                <h2 id="why-heading">Why buy from FitGear</h2>
                <ul>
                  <li>Equipment chosen for hard, repeated training</li>
                  <li>Secure Razorpay checkout — card, UPI, netbanking</li>
                  <li>Every order tracked live, start to finish</li>
                </ul>
              </section>
            </div>

            <ProductReviews key={product.id} product={product} />

            {related.length > 0 && (
              <div className="related-section">
                <SectionHeader
                  eyebrow={`More from ${product.category || 'the catalog'}`}
                  title="You might also need."
                />
                <div className="product-grid">
                  {related.map((item) => (
                    <ProductCard key={item.id} product={item} onAdd={handleRelatedAdd} />
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </section>

      <Footer />
    </div>
  )
}
