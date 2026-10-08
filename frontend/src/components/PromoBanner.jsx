import { useState } from 'react'
import { Link } from 'react-router-dom'

const FALLBACK = '/images/promo-fallback.svg'

/**
 * Split promo banner: copy on a dark panel + product/lifestyle image.
 */
export default function PromoBanner({ eyebrow, title, text, image, imageAlt, cta }) {
  const [failedImage, setFailedImage] = useState(null)
  const source = image && failedImage !== image ? image : FALLBACK

  return (
    <div className="promo-banner">
      <div className="promo-body">
        <p className="eyebrow">{eyebrow}</p>
        <h2>{title}</h2>
        <p>{text}</p>
        <Link className="primary-button" to={cta.to}>
          {cta.label}
        </Link>
      </div>
      <div className="promo-media">
        <img
          src={source}
          alt={imageAlt}
          loading="lazy"
          onError={() => {
            if (image && source !== FALLBACK) setFailedImage(image)
          }}
        />
      </div>
    </div>
  )
}
