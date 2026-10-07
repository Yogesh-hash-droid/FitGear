import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import ProductCard from '../components/ProductCard'
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
              <div className={`product-detail-visual visual-${product.id % 4}`}>
                <span>{product.category || 'FITGEAR'}</span>
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} />
                ) : (
                  <div className="visual-shape" />
                )}
              </div>

              <div className="product-detail-copy">
                <p className="eyebrow">{product.category || 'EQUIPMENT'}</p>
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
              </div>
            </div>

            {related.length > 0 && (
              <div className="related-section">
                <div className="section-heading">
                  <div>
                    <p className="eyebrow">MORE FROM {product.category?.toUpperCase()}</p>
                    <h2>You might also need.</h2>
                  </div>
                </div>
                <div className="product-grid">
                  {related.map((item, index) => (
                    <ProductCard key={item.id} product={item} index={index} />
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
