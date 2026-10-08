import { Link } from 'react-router-dom'
import ProductImage from './ProductImage'
import { formatPrice } from '../utils/format'

export default function ProductCard({ product, onAdd, adding = false }) {
  const outOfStock = Number(product.stock) === 0

  return (
    <article className="product-card">
      <Link className="product-visual" to={`/products/${product.id}`} aria-label={product.name}>
        <ProductImage
          src={product.imageUrl}
          name={product.name}
          category={product.category}
        />
        <span className={`product-badge ${outOfStock ? 'badge-out' : ''}`}>
          {outOfStock ? 'Sold out' : product.category || 'FitGear'}
        </span>
      </Link>

      <div className="product-info">
        <Link className="product-title" to={`/products/${product.id}`}>
          <h3>{product.name}</h3>
        </Link>
        <p>{product.description}</p>
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
