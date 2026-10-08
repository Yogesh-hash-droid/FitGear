import { useState } from 'react'
import { categoryFallback } from '../utils/imageFallbacks'

/**
 * Product image with a local SVG fallback: if the remote image is missing
 * or fails to load, the equipment illustration for the product's category
 * is shown instead of a broken image. The failed source is tracked so a
 * changed src is always retried.
 */
export default function ProductImage({
  src,
  name = '',
  category = '',
  className = 'product-img',
  loading = 'lazy',
  ...rest
}) {
  const [failedSrc, setFailedSrc] = useState(null)
  const fallback = categoryFallback(category)
  const url = !src || failedSrc === src ? fallback : src

  return (
    <img
      className={className}
      src={url}
      alt={name}
      loading={loading}
      onError={() => {
        if (src && url !== fallback) setFailedSrc(src)
      }}
      {...rest}
    />
  )
}
