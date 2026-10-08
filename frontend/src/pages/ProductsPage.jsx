import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import ProductCard from '../components/ProductCard'
import SkeletonCards from '../components/SkeletonCards'
import { errorMessage, fetchCategories, fetchProducts } from '../services/api'
import { useCart } from '../context/useCart'
import { useToast } from '../context/useToast'

const FALLBACK_CATEGORIES = [
  'Dumbbells',
  'Barbells',
  'Weight Plates',
  'Benches',
  'Racks',
  'Home Gym',
  'Strength',
  'Recovery',
  'Accessories',
  'Cardio',
]

const SORT_OPTIONS = [
  { value: 'name,asc', label: 'Name A–Z' },
  { value: 'name,desc', label: 'Name Z–A' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
]

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const category = searchParams.get('category') || ''
  const [searchInput, setSearchInput] = useState(searchParams.get('search') || '')
  const [search, setSearch] = useState(searchParams.get('search') || '')
  const [minPrice, setMinPrice] = useState('')
  const [maxPrice, setMaxPrice] = useState('')
  const [sort, setSort] = useState('name,asc')
  const [page, setPage] = useState(0)
  const [categories, setCategories] = useState(FALLBACK_CATEGORIES)
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const { add } = useCart()
  const { push } = useToast()

  // Debounce the search box into the query + URL.
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(searchInput)
      setSearchInput((current) => current)
      const next = new URLSearchParams(searchParams)
      if (searchInput) next.set('search', searchInput)
      else next.delete('search')
      setSearchParams(next, { replace: true })
    }, 300)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput])

  // Keep the box in sync when the URL search param changes externally
  // (for example the navbar search form navigating here).
  useEffect(() => {
    const incoming = searchParams.get('search') || ''
    if (incoming !== searchInput) setSearchInput(incoming)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  useEffect(() => {
    let active = true
    fetchCategories()
      .then((items) => {
        if (active && Array.isArray(items) && items.length) setCategories(items)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')

    const params = {
      page,
      size: 12,
      sort,
      search: search || undefined,
      category: category || undefined,
      minPrice: minPrice !== '' ? minPrice : undefined,
      maxPrice: maxPrice !== '' ? maxPrice : undefined,
    }

    fetchProducts(params)
      .then((result) => {
        if (active) setData(result)
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load products.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [search, category, minPrice, maxPrice, sort, page])

  // Reset to the first page whenever filters change.
  useEffect(() => {
    setPage(0)
  }, [search, category, minPrice, maxPrice, sort])

  function selectCategory(name) {
    const next = new URLSearchParams(searchParams)
    if (name && name !== category) next.set('category', name)
    else next.delete('category')
    setSearchParams(next)
  }

  function resetFilters() {
    setSearchInput('')
    setSearch('')
    setMinPrice('')
    setMaxPrice('')
    setSort('name,asc')
    setPage(0)
    setSearchParams(new URLSearchParams())
  }

  const totalPages = useMemo(
    () => Math.max(1, data.totalPages || Math.ceil((data.totalElements || 0) / 12)),
    [data],
  )

  async function handleAdd(product) {
    try {
      await add(product, 1)
      push(`${product.name} added to your bag.`, 'success')
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not add to bag.'), 'error')
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="section page-catalog">
        <div className="catalog-layout">
          <aside className="filter-sidebar" aria-label="Product filters">
            <div className="filter-block">
              <h3>Search</h3>
              <div className="filter-search">
                <input
                  id="product-search"
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder="Try “dumbbell”"
                />
              </div>
            </div>

            <div className="filter-block">
              <h3>Category</h3>
              <div className="filter-list" role="group" aria-label="Categories">
                <button
                  type="button"
                  className={!category ? 'active' : ''}
                  onClick={() => selectCategory('')}
                >
                  All equipment
                </button>
                {categories.map((name) => (
                  <button
                    key={name}
                    type="button"
                    className={category === name ? 'active' : ''}
                    onClick={() => selectCategory(name)}
                  >
                    {name}
                  </button>
                ))}
              </div>
            </div>

            <div className="filter-block">
              <h3>Price &amp; sort</h3>
              <div className="price-filters">
                <label>
                  Min ₹
                  <input
                    type="number"
                    min="0"
                    value={minPrice}
                    onChange={(event) => setMinPrice(event.target.value)}
                    placeholder="0"
                  />
                </label>
                <label>
                  Max ₹
                  <input
                    type="number"
                    min="0"
                    value={maxPrice}
                    onChange={(event) => setMaxPrice(event.target.value)}
                    placeholder="Any"
                  />
                </label>
                <label>
                  Sort by
                  <select value={sort} onChange={(event) => setSort(event.target.value)}>
                    {SORT_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
              </div>
            </div>

            <button className="clear-filter" type="button" onClick={resetFilters}>
              Clear filters
            </button>
          </aside>

          <div className="catalog-main">
            <div className="catalog-main-head">
              <div>
                <p className="eyebrow">The full catalog</p>
                <h1>{category || 'All equipment'}</h1>
              </div>
              <span className="catalog-count">
                {loading ? 'Loading…' : `${data.totalElements} products`}
              </span>
            </div>

            {error && <p className="form-error" role="alert">{error}</p>}

            {loading ? (
              <div className="product-grid">
                <SkeletonCards count={6} />
              </div>
            ) : data.content?.length ? (
              <div className="product-grid">
                {data.content.map((product) => (
                  <ProductCard key={product.id} product={product} onAdd={handleAdd} />
                ))}
              </div>
            ) : (
              <p className="empty-state">No products match those filters.</p>
            )}

            {totalPages > 1 && (
              <nav className="pagination" aria-label="Product pages">
                <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
                  ← Previous
                </button>
                <span>Page {page + 1} of {totalPages} · {data.totalElements} products</span>
                <button
                  type="button"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Next →
                </button>
              </nav>
            )}
          </div>
        </div>
      </section>

      <Footer />
    </div>
  )
}
