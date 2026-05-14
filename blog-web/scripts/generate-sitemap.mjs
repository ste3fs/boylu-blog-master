import { writeFile, mkdir } from 'node:fs/promises'
import path from 'node:path'

const API_BASE = process.env.SITEMAP_API_BASE || 'https://boylu.cn/boylu'
const SITE_BASE = process.env.SITE_BASE_URL || 'https://boylu.cn'
const OUTPUT = path.resolve(process.cwd(), 'public', 'sitemap.xml')

async function requestJson(url) {
  const res = await fetch(url, { headers: { Accept: 'application/json' } })
  if (!res.ok) {
    throw new Error(`Failed to fetch ${url}: ${res.status}`)
  }
  return res.json()
}

function toDate(v) {
  if (!v) return new Date().toISOString().slice(0, 10)
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return new Date().toISOString().slice(0, 10)
  return d.toISOString().slice(0, 10)
}

function escapeXml(v = '') {
  return String(v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

async function main() {
  const staticRoutes = [
    { loc: '/', priority: '1.0', changefreq: 'daily' },
    { loc: '/archive', priority: '0.8', changefreq: 'weekly' },
    { loc: '/categories', priority: '0.8', changefreq: 'weekly' },
    { loc: '/tags', priority: '0.8', changefreq: 'weekly' },
    { loc: '/about', priority: '0.6', changefreq: 'monthly' },
    { loc: '/friends', priority: '0.6', changefreq: 'weekly' },
    { loc: '/messages', priority: '0.6', changefreq: 'weekly' }
  ]

  const articleApi = `${API_BASE}/api/article/home-list?pageNum=1&pageSize=500`
  const articleResp = await requestJson(articleApi)
  const records = articleResp?.data?.records || []

  const articleRoutes = records
    .filter(item => item && item.id)
    .map(item => ({
      loc: `/article/${item.id}`,
      lastmod: toDate(item.createdAt),
      priority: '0.7',
      changefreq: 'weekly'
    }))

  const urls = [
    ...staticRoutes.map(item => ({ ...item, lastmod: toDate() })),
    ...articleRoutes
  ]

  const body = urls.map(item => {
    return [
      '  <url>',
      `    <loc>${escapeXml(new URL(item.loc, SITE_BASE).toString())}</loc>`,
      `    <lastmod>${escapeXml(item.lastmod)}</lastmod>`,
      `    <changefreq>${escapeXml(item.changefreq)}</changefreq>`,
      `    <priority>${escapeXml(item.priority)}</priority>`,
      '  </url>'
    ].join('\n')
  }).join('\n')

  const xml = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    body,
    '</urlset>',
    ''
  ].join('\n')

  await mkdir(path.dirname(OUTPUT), { recursive: true })
  await writeFile(OUTPUT, xml, 'utf-8')
  console.log(`sitemap generated: ${OUTPUT}, urls=${urls.length}`)
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
