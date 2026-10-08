import { Link } from 'react-router-dom'
import ProductImage from './ProductImage'

/**
 * Image-led category tile linking to the catalog filtered by that category.
 */
export default function CategoryCard({ name, image, blurb }) {
  return (
    <Link
      className="category-card"
      to={`/products?category=${encodeURIComponent(name)}`}
      aria-label={`Shop ${name}`}
    >
      <div className="category-media">
        <ProductImage src={image} name={name} category={name} />
      </div>
      <div className="category-body">
        <h3>{name}</h3>
        <span>{blurb ? `${blurb} · ` : ''}Shop now →</span>
      </div>
    </Link>
  )
}
