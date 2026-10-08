const FALLBACK_BY_CATEGORY = {
  Dumbbells: '/images/products/dumbbells.svg',
  Barbells: '/images/products/barbells.svg',
  'Weight Plates': '/images/products/weight-plates.svg',
  Benches: '/images/products/benches.svg',
  Racks: '/images/products/racks.svg',
  'Home Gym': '/images/products/home-gym.svg',
  Strength: '/images/products/strength.svg',
  Cardio: '/images/products/cardio.svg',
  Recovery: '/images/products/recovery.svg',
  Accessories: '/images/products/accessories.svg',
}

const DEFAULT_FALLBACK = '/images/products/strength.svg'

/** Local equipment illustration used when a product image is missing. */
export function categoryFallback(category) {
  return FALLBACK_BY_CATEGORY[category] || DEFAULT_FALLBACK
}
