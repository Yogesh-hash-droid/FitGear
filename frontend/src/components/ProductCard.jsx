import { Link } from 'react-router-dom'
import { formatPrice } from '../utils/format'

export default function ProductCard({ product, index = 0, onAdd, adding = false }) {
  const outOfStock = Number(product.stock) === 0

  return (
    <article className="product-card">
      <Link className={`product-visual visual-${index % 4}`} to={`/products/${product.id}`}>
        <span>{product.category || 'FITGEAR'}</span>
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} loading="lazy" />
        ) : (
          <div className="visual-shape" />
        )}
      </Link>
      <div className="product-info">
        <div>
          <Link className="product-title" to={`/products/${product.id}`}>
            <h3>{product.name}</h3>
          </Link>
          <p>{product.description}</p>
        </div>
        <strong>{formatPrice(product.price)}</strong>
      </div>
      <button
        className="add-button"
        type="button"
        disabled={outOfStock || adding}
        onClick={() => onAdd?.(product)}
      >
        {outOfStock ? 'Out of stock' : adding ? 'Adding…' : <>Add to bag <span>+</span></>}
      </button>
    </article>
  )
}
