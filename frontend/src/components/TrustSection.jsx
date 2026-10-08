import FeatureCard from './FeatureCard'

const QUALITY_ICON = (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M12 3l7 3v5c0 4.5-3 8.4-7 10-4-1.6-7-5.5-7-10V6l7-3z" />
    <path d="M9 12l2 2 4-4" />
  </svg>
)

const DELIVERY_ICON = (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M3 7h11v10H3z" />
    <path d="M14 10h4l3 3v4h-7" />
    <circle cx="7" cy="18" r="1.8" />
    <circle cx="17.5" cy="18" r="1.8" />
  </svg>
)

const PAYMENT_ICON = (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="3" y="6" width="18" height="13" rx="2" />
    <path d="M3 10h18" />
    <path d="M7 15h4" />
  </svg>
)

const TRAINING_ICON = (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M6.5 9v6M4 10.5v3M17.5 9v6M20 10.5v3M6.5 12h11" />
  </svg>
)

const FEATURES = [
  {
    icon: QUALITY_ICON,
    title: 'Quality equipment',
    text: 'Steel, rubber and upholstery chosen for hard, repeated use — gear that holds up session after session.',
  },
  {
    icon: DELIVERY_ICON,
    title: 'Fast delivery',
    text: 'Free shipping on selected fitness equipment, dispatched from our warehouse to your door.',
  },
  {
    icon: PAYMENT_ICON,
    title: 'Secure payments',
    text: 'Pay online with Razorpay — card, UPI and netbanking — or choose cash on delivery.',
  },
  {
    icon: TRAINING_ICON,
    title: 'Built for training',
    text: 'From dumbbells to full home gyms, every product is selected for real strength and conditioning work.',
  },
]

/**
 * Four-point trust row shown on the homepage.
 */
export default function TrustSection() {
  return (
    <div className="trust-grid">
      {FEATURES.map((feature) => (
        <FeatureCard key={feature.title} {...feature} />
      ))}
    </div>
  )
}
